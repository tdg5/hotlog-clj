(defproject hotlog "0.1.2-SNAPSHOT"
  :dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                 [org.clojure/clojure "1.10.0"]
                 [org.slf4j/slf4j-api "1.7.26"]]
  :description "Some niceties on top of slf4j and logback"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :profiles {:dev {:dependencies [[org.clojure/tools.logging "0.4.1"]]
                   :jvm-opts ["-Xverify:none"]
                   :plugins      [[lein-auto "0.1.3"]]
                   :source-paths ["src"]}}
  :repl-options {:init-ns hotlog.core}
  :repositories [["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]]
  :url "https://github.com/tdg5/hotlog")
