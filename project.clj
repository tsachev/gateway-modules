(defproject com.tick42.gateway/modules-parent "3.0.16-SNAPSHOT"
  :description "Tick42 Gateway Modules Parent"
  :url "https://github.com/Glue42/gateway-modules"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}

  :target-path "target/%s"

  :profiles {:uberjar           {:aot          :all
                                 :dependencies [[org.clojure/clojure "_"]]}
             :provided          {:dependencies [[org.clojure/clojure "_"]]}

             :dev               {:plugins      [[lein-kibit "0.1.8"]
                                                [jonase/eastwood "1.2.2"]
                                                ;;https://github.com/xsc/lein-ancient/issues/123
                                                [lein-ancient "0.6.15"]
                                                [test2junit "1.4.2"]]
                                 :dependencies [[org.clojure/tools.reader "_"]]}

             :limited-resources {:jvm-opts ["-Xms1g" "-Xmx1g" "-XX:+HeapDumpOnOutOfMemoryError"
                                            "-Xss512k" "-XX:MetaspaceSize=256m" "-XX:MaxMetaspaceSize=256m"
                                            "-XX:CompressedClassSpaceSize=64m" "-XX:ReservedCodeCacheSize=32m"]}}


  :plugins [[lein-modules "0.3.11"]
            [com.andrewmcveigh/lein-auto-release "0.1.10"]]

  :modules {:inherited {:url                 "https://github.com/Glue42/gateway-modules"
                        :license             {:name "MIT"
                                              :url  "https://opensource.org/licenses/MIT"}
                        :deploy-repositories [["clojars" {:url      "https://clojars.org/repo"
                                                          :username :env/CLOJARS_USER
                                                          :password :env/CLOJARS_PASS
                                                          :signing  {:gpg-key "5771E8CF02241B72"}}]]}

            :versions  {org.clojure/clojure                    "1.10.3"
                        com.taoensso/timbre                    "5.1.2"
                        org.clojure/core.async                 "1.5.648"
                        org.clojure/tools.reader               "1.3.6"
                        instaparse                             "1.4.10"
                        cheshire                               "5.10.2"
                        com.github.ben-manes.caffeine/caffeine "2.9.3"
                        funcool/promesa                        "6.0.2"
                        gnl/ghostwheel                         "0.3.9"}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["modules" "change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["vcs" "push"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["modules" "change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]
				  ]	

  :packaging "pom"
  :pom-addition [:modules
                 [:module "auth"]
                 [:module "basic-auth"]
                 [:module "common"]
                 [:module "common-test"]
                 [:module "context-domain"]
                 [:module "activity-domain"]
                 [:module "agm-domain"]
                 [:module "global-domain"]
                 [:module "metrics-domain"]
                 [:module "bus-domain"]
                 [:module "local-node"]
                 [:module "ghostwheel-stub"]])
