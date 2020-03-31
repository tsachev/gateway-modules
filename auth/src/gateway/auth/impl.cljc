(ns gateway.auth.impl
  (:require [gateway.auth.core :refer [Authenticator] :as core]
            [gateway.auth.spec :as s]

            [gateway.common.utilities :as util]
            [gateway.common.tokens :as tokens]

            [taoensso.timbre :as timbre]
            [promesa.core :as p]

            [clojure.walk :refer [keywordize-keys]]
            [clojure.core.async :as a]

            [ghostwheel.core :as g :refer [>defn >defn- >fdef => | <- ?]]))

(defonce default-authentication-timeout 5000)

(defn sanitize [authentication]
  (if (:secret authentication)
    (assoc authentication :secret "XXX")
    (assoc authentication :token "XXX")))

(defn sanitize-response [response]
  (if (:access-token response)
    (assoc response :access-token "<TOKEN>")
    response))

(defprotocol AuthImpl
  (auth [this authentication-request]))

(defn- handle-gw-token-authentication
  [signature-key token]

  (let [gw-token (tokens/->token signature-key token)]

    (case (:type gw-token)
      :gw-request (if-let [gw-request (:gw-request gw-token)]

                    (p/resolved {:type             :success
                                 :gw-request       gw-request
                                 :impersonate-peer (:impersonate-peer gw-token)})

                    (p/rejected {:type    :failure
                                 :message "Token is missing the gateway request information"}))


      :authentication (if-let [user (:user gw-token)]
                        (p/resolved {:type :success
                                     :user user})

                        (p/rejected {:type    :failure
                                     :message "Token is missing the impersonation information"}))

      (p/rejected {:type    :failure
                   :message (str "Invalid gateway token type: " (:type gw-token))}))))

(>defn -authenticate!
  [authenticator-ch timeout authentication-request]
  [any? int? ::s/request => any?]
  (if-let [token (core/gateway-token (:authentication authentication-request))]
    (handle-gw-token-authentication (:signature-key authentication-request) token)

    (if authenticator-ch
      (-> (p/create (fn [resolve reject] (a/put! authenticator-ch [resolve reject authentication-request])))
          (p/timeout timeout {:type    :failure
                              :message "Authentication timed out"}))


      (p/rejected (ex-info "No authenticator configured" {:type    :failure
                                                          :message "No authenticator configured"})))))

(deftype DefaultAuthenticator [authenticator-ch timeout]
  Authenticator
  (stop [this]
    (when authenticator-ch
      (util/close-and-flush! authenticator-ch)))

  (authenticate [this authentication-request]
    (-authenticate! authenticator-ch timeout authentication-request)))


(defn authenticator
  [config impl]
  (let [ch (a/chan (a/dropping-buffer 20000))]
    (timbre/info "starting authenticator" impl)
    (a/go-loop []
      (let [msg (a/<! ch)]
        (when msg
          (let [[resolve reject authentication-request] msg
                authentication-request (update authentication-request :authentication keywordize-keys)
                authentication (:authentication authentication-request)]
            (timbre/debug "processing authentication" (sanitize authentication))

            (-> (auth impl authentication-request)
                (p/then (fn [v] (resolve v)))
                (p/catch (fn [v] (reject v))))
            (recur)))))
    (DefaultAuthenticator. ch (:timeout config default-authentication-timeout))))
