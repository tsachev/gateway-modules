(ns gateway.domains.global.util
  (:require [gateway.basic-auth.core :as basic]
            [gateway.auth.core :as auth]
            [gateway.domains.global.core :as global]
            [clojure.core.async :as a]))

(defn ->auth []
  {:default   :basic
   :available {:basic (basic/authenticator {})}})

(defn stop-auth [auths]
  (doseq [a (vals (:available auths))]
    (auth/stop a)))

(defn handle-hello
  [state source request env]
  (let [auth (->auth)]
    (try
      (let [ch (a/chan 100)]
        (try
          (let [[state _] (-> state
                              (global/handle-hello source request auth ch env))

                [auth-msg _] (a/alts!! [ch (a/timeout 1000)])]
            (-> state
                (global/handle-request source (:body auth-msg) nil nil env)))
          (finally
            (a/close! ch))))
      (finally
        (stop-auth auth)))))