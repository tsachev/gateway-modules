(ns gateway.common.action-logger
  "Action Logging.
  Provides a log-action macro that can be used to log actions.

  Usage:

    (:require
      [gateway.common.action-logger :refer [log-action]]
      #?(:cljs [gateway.common.action-logger :refer-macros [log-action]]))


  (log-action \"my-group\" \"peer\" peer-id \"joined.\")

  It hooks into timbre and allows filtering action groups.
  Can be used only when :info level is enabled for timbre.

  To enabled it do
  (gateway.common.action-logger/enable-action-log)
  "
  (:require
    [clojure.string :as str]
    [taoensso.timbre :as timbre]))

;; todo this may go into config map (for example to allow changing the :info level.)

(defonce ^:private NS "GATEWAY_ACTION_LOG")
(defonce LEVEL :info)
(defonce ^:private SEPARATOR "|")
(defonce ^:private PREFIX (str NS SEPARATOR))
(defonce ^:private PREFIX_LENGTH (count PREFIX))

(defn- update-action-ns [data]
  (let [{:keys [?ns-str]} data]
    (conj
      data
      (if (str/starts-with? ?ns-str PREFIX)
        (if-let [group-end (str/index-of ?ns-str SEPARATOR PREFIX_LENGTH)]
          {:?ns-str (subs ?ns-str (inc group-end))
           :group   (subs ?ns-str PREFIX_LENGTH group-end)}
          {:?ns-str NS})
        {:?ns-str ?ns-str}))))

(defn may-log-action?
  "Check if a group may be logged."
  [group]
  (timbre/may-log? LEVEL (str PREFIX group SEPARATOR)))

(defmacro log-action
  "Logs Action.
  (timbre/info some \"thing happened \" n \"time(s).\" )
  (log-action \"my-group\" some \"thing happened \" n \"time(s).\" )
  "
  [group & args]
  `(timbre/log! LEVEL :p ~args ~{:?line (:line (meta &form)) :?ns-str (str PREFIX group SEPARATOR (str *ns*))}))

(defn enable-action-log!
  "Enables action log with optional list of blacklisted action groups.
  It will replace the :output-fn in timbre config."
  ([] (enable-action-log! []))
  ([blacklisted-groups]
   (timbre/swap-config!
     (fn [c]
       (-> c
           (update-in [:output-fn]
                      (fn [original-output-fn]
                        (let [output-fn (or original-output-fn timbre/default-output-fn)]
                          (fn [data]
                            (output-fn (update-action-ns data))))))
           (update-in [:ns-blacklist]
                      (fn [original-ns-blacklist]
                        (vec (distinct (concat
                                         (filter #(not (str/starts-with? % PREFIX)) original-ns-blacklist)
                                         (map #(str PREFIX % SEPARATOR "*") blacklisted-groups)))))))))))
