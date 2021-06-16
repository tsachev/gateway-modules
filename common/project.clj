(defproject com.tick42.gateway/common "3.0.12-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]

  :dependencies [[instaparse "_"]
                 [com.auth0/java-jwt "3.8.1"]
                 [commons-codec/commons-codec "1.13"]
                 [cheshire "_"]
                 [com.taoensso/timbre "_"]
                 [gnl/ghostwheel "_"]
                 [org.clojure/core.async "_"]
                 [com.github.ben-manes.caffeine/caffeine "_"]]

  :modules {:subprocess nil})
