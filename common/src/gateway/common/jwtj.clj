(ns gateway.common.jwtj
  (:require [cheshire.core :as cheshire])
  (:import (com.auth0.jwt.algorithms Algorithm)
           (com.auth0.jwt JWT)
           (java.nio.charset StandardCharsets)
           (java.util Base64)))

(set! *warn-on-reflection* true)

(defn- base64-encode-url-safe-string ^String [^bytes b]
  (let [encoder (-> (Base64/getUrlEncoder)
                    (.withoutPadding))]
    (.encodeToString encoder b)))

(defn- base64-decode ^bytes [^bytes b]
  (let [decoder (Base64/getUrlDecoder)]
    (.decode decoder b)))

(defn sign [payload ^String key]
  (let [algo (Algorithm/HMAC256 key)
        header (-> {:alg (.getName algo) :typ "JWT"}
                   (cheshire/generate-string)
                   (.getBytes StandardCharsets/UTF_8)
                   (base64-encode-url-safe-string))
        p (-> payload
              (cheshire/generate-string)
              (.getBytes StandardCharsets/UTF_8)
              (base64-encode-url-safe-string))

        signature (-> algo
                      (.sign (.getBytes header StandardCharsets/UTF_8)
                             (.getBytes p StandardCharsets/UTF_8))
                      (base64-encode-url-safe-string))]
    (str header "." p "." signature)))

(defn unsign [^String message ^String key claims]
  (let [algo (Algorithm/HMAC256 key)]
    (-> (JWT/require algo)
        (.build)
        (.verify message)
        (.getPayload)
        (.getBytes StandardCharsets/UTF_8)
        (base64-decode)
        (String. StandardCharsets/UTF_8)
        (cheshire/parse-string keyword))))
