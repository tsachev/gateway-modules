(defproject com.tick42.gateway/bus-domain "3.0.18"
  :plugins [[lein-modules "0.3.11"]]

  :dependencies [[com.tick42.gateway/common :version]
                 [com.tick42.gateway/global-domain :version]

                 [com.taoensso/timbre "_"]
                 [org.clojure/core.async "_"]]

  :profiles {:dev {:dependencies   [[com.tick42.gateway/basic-auth :version]
                                    [com.tick42.gateway/local-node :version]
                                    [com.tick42.gateway/common-test :version]]
                   :resource-paths ["test/resources"]}}

  :test2junit-run-ant false
  :test2junit-output-dir "test-results"

  :modules {:subprocess nil})
