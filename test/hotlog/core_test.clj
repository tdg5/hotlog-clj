(ns hotlog.core-test
  (:require [clojure.test :refer :all]
            [hotlog.core :as subject]))

(deftest noop-logger
  (testing "implements debug taking one argument and returning nil"
    (is (nil? (.debug subject/noop-logger "hi"))))

  (testing "implements error taking one argument and returning nil"
    (is (nil? (.error subject/noop-logger ""))))

  (testing "implements getName returning NoopLogger"
    (is (= "NoopLogger" (.getName subject/noop-logger))))

  (testing "implements info taking one argument and returning nil"
    (is (nil? (.info subject/noop-logger ""))))

  (testing "implements isDebugEnabled returning false"
    (is (false? (.isDebugEnabled subject/noop-logger))))

  (testing "implements isErrorEnabled returning false"
    (is (false? (.isErrorEnabled subject/noop-logger))))

  (testing "implements isInfoEnabled returning false"
    (is (false? (.isInfoEnabled subject/noop-logger))))

  (testing "implements isTraceEnabled returning false"
    (is (false? (.isTraceEnabled subject/noop-logger))))

  (testing "implements isWarnEnabled returning false"
    (is (false? (.isWarnEnabled subject/noop-logger))))

  (testing "implements trace taking one argument and returning nil"
    (is (nil? (.trace subject/noop-logger ""))))

  (testing "implements warn taking one argument and returning nil"
    (is (nil? (.warn subject/noop-logger "")))))
