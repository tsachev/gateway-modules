(ns gateway.common.context.state
  (:require [gateway.common.utilities :as util]
            [gateway.reason :refer [->Reason throw-reason]]
            [taoensso.timbre :as timbre]
            [clojure.string :as string]))

;; contexts

(defn contexts [state] (vals (get-in state [:contexts])))

(defn add-context
  [state context]
  (assoc-in state [:contexts (:id context)] context))

(defn remove-context
  [state context-id]
  (util/dissoc-in state [:contexts context-id]))

(defn context-by-name
  "Finds a context by name. Actually faster than filter/some"

  [state name peer]
  (let [peer-user (some-> peer :identity :user)
        service? (some-> peer :options :service?)
        contexts (vals (:contexts state))]

    (or
      ;; try to match against the same user
      (reduce
        (fn [_ c] (when (and (= name (:name c))
                             (= peer-user (some-> c :identity :user))) (reduced c)))
        nil
        contexts)
      ;; try to match against a context that was created by a service
      (reduce
        (fn [_ c] (when (and (= name (:name c))
                             (or service? (some-> c :options :service?))) (reduced c)))
        nil
        contexts))))

(defn context-by-id
  [state context-id]
  (when context-id
    (get-in state [:contexts context-id])))

(defn context-by-id*
  [state context-id]
  (if-let [context (get-in state [:contexts context-id])]
    context
    (throw (ex-info (str "Unable to find context with id " context-id) {}))))

(defn add-context-member
  "Adds a peer as a member to a context"

  [state context peer-id]
  (if (and context peer-id)
    (let [existing-members (get context :members)]
      (if (contains? existing-members peer-id)
        state
        (assoc-in state [:contexts (:id context)] (update context :members (fnil conj #{}) peer-id))))
    state))

(defn- remove-context-owner
  [context peer-id]
  (if (= peer-id (:owner context))
    (dissoc context :owner)
    context))

(defn remove-context-member
  "Removes a peer from the context members"

  [state context peer-id]
  (let [updated (-> (update context :members disj peer-id)
                    (remove-context-owner peer-id))]
    [(assoc-in state [:contexts (:id context)] updated) updated]))

;; context delta processing

(defmulti apply-delta-cmd (fn [context-data cmd] (first cmd)))

(defmethod apply-delta-cmd :removed
  [context-data [_ removed-keys]]
  (if (seq removed-keys)
    (apply dissoc context-data removed-keys)
    context-data))

(defmethod apply-delta-cmd :added
  [context-data [_ added-dict]]
  (if (some? added-dict)
    (reduce-kv assoc
               context-data
               added-dict)
    context-data))

(defmethod apply-delta-cmd :updated
  [context-data [_ updated-dict]]
  (if (some? updated-dict)
    (reduce-kv (fn [data k v]
                 (update data k (fn [existing]
                                  (cond
                                    (and (vector? v) (vector? existing)) v
                                    (and (associative? v) (associative? existing)) (merge existing v)
                                    :else v))))
               context-data
               updated-dict)
    context-data))

(defmethod apply-delta-cmd :reset
  [context-data [_ updated-dict]]
  (if updated-dict updated-dict context-data))

(defmulti apply-command (fn [context-data cmd] (keyword (:type cmd))))

(defn- get-map
  [m k]
  (let [v (get m k)]
    (if (or (nil? v) (not (map? v))) {} v)))

(defn- set-in
  [m [k & ks] v]
  (if (nil? k)
    v
    (if ks
      (assoc m k (set-in (get-map m k) ks v))
      (assoc m k v))))

(defn- string->path
  [s]
  (if (string/blank? s)
    nil
    (string/split s #"\.")))

(defmethod apply-command :set
  [context-data {:keys [value path]}]
  (set-in (or context-data {}) (string->path path) value))

(defmethod apply-command :remove
  [context-data {:keys [path]}]
  (let [p (string->path path)]
    (if (nil? p)
      {}
      (util/dissoc-in context-data p {:keep true}))))

(defmethod apply-delta-cmd :commands
  [context-data [_ commands]]
  (reduce
    (fn [result cmd]
      (apply-command result (util/keywordize cmd)))
    context-data
    commands))

(defmethod apply-delta-cmd :default
  [context-data [cmd payload]]
  (timbre/warn "ignoring unknown context delta command" cmd "with payload" payload)
  context-data)

(defn apply-delta
  [state context delta version]

  (let [context-id (:id context)]
    (-> state
        (assoc-in [:contexts context-id :data]
                  (reduce apply-delta-cmd
                          (or (:data context) {})
                          delta))
        (assoc-in [:contexts context-id :version] version))))

(defn ->ctx
  [creator name data lifetime read_permissions write_permissions ctx-id version]
  (let [identity (:identity creator)
        options (:options creator)]
    (cond-> {:id                ctx-id
             :data              data
             :identity          identity
             :lifetime          lifetime
             :read_permissions  read_permissions
             :write_permissions write_permissions
             :members           #{}
             :version           version
             :name              name
             :creator           (:id creator)}
            options (assoc :options options))))

(defn next-version [context]
  (let [version (:version context {:updates 0})]
    (-> version
        (update :updates inc)
        (assoc :timestamp (util/current-time)))))

(defn new-version []
  {:updates   0
   :timestamp (util/current-time)})
