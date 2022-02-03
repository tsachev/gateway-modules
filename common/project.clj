(defproject com.tick42.gateway/common "3.0.15-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]

  :dependencies [[instaparse "_"]
                 [com.auth0/java-jwt "3.18.1"]
                 [cheshire "_"]
                 [com.taoensso/timbre "_"]
                 [gnl/ghostwheel "_"]
                 [org.clojure/core.async "_"]
                 [com.github.ben-manes.caffeine/caffeine "_"]]

  :modules {:subprocess nil})
