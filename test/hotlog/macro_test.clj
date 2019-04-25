(ns hotlog.macro-test
  (:require [clojure.test :refer :all]
            [hotlog.core :as hotlog]
            [hotlog.macro :as subject]))

(comment "test after org.clojure/tools.logging test helpers are released"
  (deftest tools-logging-log!
    (testing "sss"
      (hotlog/build-logger {:appenders [{:level :debug :type :console}]
                            :name (str "hotlog.macro-test")})
      (subject/defn-log!)
      (log! :warn "This should say hi")
      (log! :error (Exception. "Oh no!")))))
