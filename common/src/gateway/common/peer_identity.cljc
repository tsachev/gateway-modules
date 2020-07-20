(ns gateway.common.peer-identity
  (:require [gateway.reason :refer [throw-reason]]
            [clojure.string :as string]))


(def known-identity-keys {"application" {:required true}
                          "instance"    {:required false}
                          "region"      {:required false}
                          "environment" {:required false}
                          "machine"     {:required false}
                          "user"        {:required false}})

(defn keywordize-id [id]
  (reduce-kv (fn [m k v]
               (assoc m (if (contains? known-identity-keys k)
                          (keyword k)
                          k)
                        v))
             {}
             id))

(defn- missing-key? [m]
  (when-let [[k _] (some (fn [[k v]] (and (:required k) (nil? (get m (keyword k))))) known-identity-keys)]
    k))

(defn check-identity*
  "Checks the identity for validity. Returns a list of problems if any or nil otherwise"

  [identity]
  (when-let [missing-key (missing-key? identity)]
    (throw (ex-info (str "Identity " identity " is missing a required key: " missing-key) {}))))

(defn machine
  [endpoint ip]
  (if (and endpoint (string/includes? endpoint "127.0.0.1"))
    (or ip endpoint)
    (or endpoint ip)))