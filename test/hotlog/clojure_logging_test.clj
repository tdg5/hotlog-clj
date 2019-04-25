(ns hotlog.clojure-logging-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging.impl :as clog-impl]
            [hotlog.core :as subject]))

(deftest clojure.logging
  (testing "using hotlog logger via clojure.tools.logging/log macro"
    (let [logger-name "hotlog.clojure-logging-test"
          hotlog-logger (subject/build-logger {:additive? false
                                               :appenders [{:level :debug :type :console}]
                                               :detach-and-stop-all-appenders? true
                                               :level :debug
                                               :name logger-name})]
      (is (= hotlog-logger (clog-impl/get-logger (clog-impl/find-factory) logger-name))))))
