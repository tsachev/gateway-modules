(ns gateway.common.tokens
  (:require [gateway.common.utilities :refer [now]]
            [gateway.common.peer-identity :as peer-identity]
            [clojure.walk :as walk]
    #?(:clj [gateway.common.jwtj :as jwt]
       :cljs [gateway.common.jwt :as jwt])))

(def ^:dynamic *ttl* (* 1000 60 10))                ; 10 minutes

(defn for-request
  (
   [state impersonate-identity gw-request exp]
   (jwt/sign (cond-> {:type             :gw-request
                      :impersonate-peer impersonate-identity
                      :gw-request       gw-request}
                     exp (assoc :exp (quot exp 1000)))
             (:signature-key state)))
  (
   [state impersonate-identity gw-request]
   (for-request state impersonate-identity gw-request (+ (now) *ttl*))))

(defn for-authentication
  (
   [state impersonate-identity exp]
   (jwt/sign (cond-> {:type :authentication
                      :user (:user impersonate-identity)}
                     exp (assoc :exp (quot exp 1000)))
             (:signature-key state)))
  (
   [state impersonate-identity]
   (for-authentication state impersonate-identity (+ (now) *ttl*))))


;; TODO: fixme
(defn ->token
  (
   [signature-key token-str exp]
   (let [t (-> (jwt/unsign token-str signature-key (when exp {:now (quot exp 1000)}))
               (update :type keyword)
               (update :impersonate-peer peer-identity/keywordize-id))]
     (if (= (:type t) :gw-request)
       (update t :gw-request #(-> %
                                  (walk/keywordize-keys)
                                  (update :type keyword)))
       t)))
  (
   [signature-key token-str]
   (->token signature-key token-str (now))))
