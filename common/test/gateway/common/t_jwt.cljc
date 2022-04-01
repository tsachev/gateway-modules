(ns gateway.common.t-jwt
  (:require
    #?(:cljs [cljs.test :refer-macros [deftest is testing]]
       :clj  [clojure.test :refer [deftest is testing]])
    [clojure.string :as str]
    #?(:cljs [gateway.common.jwt :as jwt]
       :clj  [gateway.common.jwtj :as jwt])))

(deftest round-trip
  (testing "encodes/decodes"
    (let [key "123"
          payload {:test "val"}
          token (jwt/sign payload key)
          decoded (jwt/unsign token key nil)]
      (is (= payload decoded)))))

(deftest unsign-url-encoded
  (testing "token containing _"
    (let [payload {:type       :gw-request
                   :gw-request {:type      :activity
                                :id        "r-a5abd2e406034e94b2528f17ba817010-361"
                                :peer_type "client-view"
                                :activity  {:id     "a-a5abd2e406034e94b2528f17ba817010-36"
                                            :owner? true}
                                :peer_name "client-view"}}
          token (jwt/sign payload "123")]
      (is (str/includes? (get (str/split token #"\.") 1) "_"))
      (is (not-empty (jwt/unsign token "123" nil))))))
