(defproject cljml-core-matrix "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :test-paths  ["src/" "test/" ]
  :plugins  [[lein-codox "0.10.3"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [net.mikera/core.matrix "0.57.0"]
                 [com.rpl/specter "1.0.1"]
                 [cc.artifice/clj-ml "0.8.6"]])
