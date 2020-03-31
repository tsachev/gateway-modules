(ns gateway.common.t-core
  (:require [clojure.test :refer :all]

            [taoensso.timbre :as timbre]
            [gateway.common.channel-buffers :as buf]
            [clojure.core.async :as a]))

(deftest drop-message-on-full-buffer
  (testing "On put in a channel over dropping buffer catch on-drop event and get dropped counter incremented"
    (let [buf-size (rand-int 100)
          _ (timbre/debug "buf-size:" buf-size)
          dropped (atom {})
          ch (a/chan (buf/dropping-buffer-with-signal
                       buf-size
                       (fn [itm total-dropped]
                         (timbre/info "dropped message" itm)
                         (swap! dropped assoc :dropped-msg itm :total-dropped total-dropped))))]

      (dotimes [n buf-size]
        (a/put! ch (str "msg no." n)))

      (timbre/debug "buffer count:" (.-n (.-buf ch)))

      (is (empty? @dropped))

      (a/put! ch (str "msg no." (inc buf-size)))

      (is (= 1 (:total-dropped @dropped)))
      (is (= 1 (buf/dropped-count (.-buf ch))))
      (is (= (str "msg no." (inc buf-size)) (:dropped-msg @dropped))))))

