(ns gateway.common.configuration
  (:require [gateway.common.tokens :as tokens]))

(defn token-ttl
  [configuration]
  (let [ttl (* (get-in configuration [:authentication :token-ttl] 0) 1000)]
    (if (pos? ttl)
      ttl
      tokens/*ttl*)))

