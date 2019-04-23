(ns hotlog.core-test
  (:require [clojure.test :refer :all]
            [hotlog.core :as subject])
  (:import [ch.qos.logback.classic Level LoggerContext]
           [ch.qos.logback.classic.spi LoggingEvent]
           [ch.qos.logback.core ConsoleAppender FileAppender]
           [ch.qos.logback.core.spi FilterReply]
           [org.slf4j Logger LoggerFactory]))

(def trace-event
  (doto (LoggingEvent.)
        (.setLevel Level/TRACE)))

(def debug-event
  (doto (LoggingEvent.)
        (.setLevel Level/DEBUG)))

(def info-event
  (doto (LoggingEvent.)
        (.setLevel Level/INFO)))

(def warn-event
  (doto (LoggingEvent.)
        (.setLevel Level/WARN)))

(def error-event
  (doto (LoggingEvent.)
        (.setLevel Level/ERROR)))

(defn logger-context []
  (LoggerFactory/getILoggerFactory))

(deftest logger-levels
  (testing ":debug"
    (is (= Level/DEBUG (:debug subject/logger-levels))))

  (testing ":error"
    (is (= Level/ERROR (:error subject/logger-levels))))

  (testing ":info"
    (is (= Level/INFO (:info subject/logger-levels))))

  (testing ":trace"
    (is (= Level/TRACE (:trace subject/logger-levels))))

  (testing ":warn"
    (is (= Level/WARN (:warn subject/logger-levels)))))

(deftest threshold-filter-levels
  (testing ":debug"
    (is (= "DEBUG" (:debug subject/threshold-filter-levels))))

  (testing ":error"
    (is (= "ERROR" (:error subject/threshold-filter-levels))))

  (testing ":info"
    (is (= "INFO" (:info subject/threshold-filter-levels))))

  (testing ":trace"
    (is (= "TRACE" (:trace subject/threshold-filter-levels))))

  (testing ":warn"
    (is (= "WARN" (:warn subject/threshold-filter-levels)))))

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

(deftest threshold-filter
  (testing "filter configured with specified level"
    (let [threshold-filter* (subject/threshold-filter {:level :info})]
      (is (= FilterReply/DENY (.decide threshold-filter* trace-event)))
      (is (= FilterReply/DENY (.decide threshold-filter* debug-event)))
      (is (= FilterReply/NEUTRAL (.decide threshold-filter* info-event)))
      (is (= FilterReply/NEUTRAL (.decide threshold-filter* warn-event)))
      (is (= FilterReply/NEUTRAL (.decide threshold-filter* error-event)))))

  (testing "filter is started by default"
    (let [threshold-filter* (subject/threshold-filter {:level :info})]
      (is (= true (.isStarted threshold-filter*)))))

  (testing "filter is not started when :start? is given as false"
    (let [threshold-filter* (subject/threshold-filter {:level :info :start? false})]
      (is (= false (.isStarted threshold-filter*))))))

(deftest simple-encoder-factory
  (testing "encoder is configured with given context"
    (let [context (logger-context)
          encoder (subject/simple-encoder-factory {:context context})]
      (is (= context (.getContext encoder)))))

  ;; currently the encoder always returns false when encoder.isStarted is called. Uncomment this
  ;; test if/when this has been fixed and logback has been updated.
  ;; https://github.com/qos-ch/logback/pull/455
  (comment
   (testing "encoder is started by default"
     (let [encoder (subject/simple-encoder-factory {:context (logger-context)})]
       (Thread/sleep 1000)
       (is (= true (.isStarted encoder))))))

  ;; currently encoder.isStarted always returns false. See above.
  (testing "encoder is not started when false is given for start?"
    (let [encoder (subject/simple-encoder-factory {:context (logger-context)
                                                   :start? false})]
      (is (= false (.isStarted encoder)))))

  (testing "encoder is configured with expected pattern"
    (let [encoder (subject/simple-encoder-factory {:context (logger-context)})]
      (is (= "%date %level %logger [%thread]: %msg%n" (.getPattern encoder))))))

(deftest build-appender-console
  (testing "appender is started by default"
    (let [console-appender (subject/build-appender {:context (logger-context)
                                                    :level :info
                                                    :type :console})]
      (is (= true (.isStarted console-appender)))))

  (testing "appender is not started when false is given for :start?"
    (let [console-appender (subject/build-appender {:context (logger-context)
                                                    :level :info
                                                    :start? false
                                                    :type :console})]
      (is (= false (.isStarted console-appender)))))

  (testing "appender is configured with the given name when :name is given"
    (let [name "appender-00"
          console-appender (subject/build-appender {:context (logger-context)
                                                    :level :info
                                                    :name name
                                                    :start? false
                                                    :type :console})]
      (is (= name (.getName console-appender)))))

  (testing "appender is configured with the specified context"
    (let [context (logger-context)
          console-appender (subject/build-appender {:context context
                                                    :level :info
                                                    :start? false
                                                    :type :console})]
      (is (= context (.getContext console-appender)))))

  (testing "appender is configured with threshold filter configured with given level"
    (let [console-appender (subject/build-appender {:context (logger-context)
                                                    :level :info
                                                    :start? true
                                                    :type :console})
          threshold-filter* (-> console-appender .getCopyOfAttachedFiltersList seq first)]
      (is (= true (.isStarted threshold-filter*)))
      (is (= FilterReply/DENY (.getFilterChainDecision console-appender trace-event)))
      (is (= FilterReply/DENY (.getFilterChainDecision console-appender debug-event)))
      (is (= FilterReply/NEUTRAL (.getFilterChainDecision console-appender info-event)))
      (is (= FilterReply/NEUTRAL (.getFilterChainDecision console-appender warn-event)))
      (is (= FilterReply/NEUTRAL (.getFilterChainDecision console-appender error-event)))))

  (testing "appender is configured with encoder generated by given :encoder-factory fn"
    (let [encoder-factory-called (atom false)
          encoder-instance (atom nil)
          start? true
          context (logger-context)
          encoder-factory (fn [args]
                            (reset! encoder-factory-called true)
                            (reset! encoder-instance (subject/simple-encoder-factory args))
                            (is (= start? (:start? args)))
                            (is (= context (:context args)))
                            @encoder-instance)
          console-appender (subject/build-appender {:context context
                                                    :encoder-factory encoder-factory
                                                    :level :info
                                                    :start? start?
                                                    :type :console})]
      (is (= true @encoder-factory-called))
      (is (= @encoder-instance (.getEncoder console-appender))))))

(deftest build-appender-file
  (testing "appender is started by default"
    (let [file-appender (subject/build-appender {:context (logger-context)
                                                 :level :info
                                                 :path "/tmp/file-logger-01"
                                                 :type :file})]
      (is (= true (.isStarted file-appender)))))

  (testing "appender is not started when false is given for :start?"
    (let [file-appender (subject/build-appender {:context (logger-context)
                                                 :level :info
                                                 :path "/tmp/file-logger-02"
                                                 :start? false
                                                 :type :file})]
      (is (= false (.isStarted file-appender)))))

  (testing "appender is configured with the given name when :name is given"
    (let [name "appender-01"
          file-appender (subject/build-appender {:context (logger-context)
                                                 :level :info
                                                 :name name
                                                 :start? false
                                                 :type :console})]
      (is (= name (.getName file-appender)))))

  (testing "appender is configured with the specified context"
    (let [context (logger-context)
          file-appender (subject/build-appender {:context context
                                                 :level :info
                                                 :path "/tmp/file-logger-03"
                                                 :start? false
                                                 :type :file})]
      (is (= context (.getContext file-appender)))))

  (testing "appender is configured with threshold filter configured with given level"
    (let [file-appender (subject/build-appender {:context (logger-context)
                                                 :level :info
                                                 :path "/tmp/file-logger-04"
                                                 :start? true
                                                 :type :file})
          threshold-filter* (-> file-appender .getCopyOfAttachedFiltersList seq first)]
      (is (= true (.isStarted threshold-filter*)))
      (is (= FilterReply/DENY (.getFilterChainDecision file-appender trace-event)))
      (is (= FilterReply/DENY (.getFilterChainDecision file-appender debug-event)))
      (is (= FilterReply/NEUTRAL (.getFilterChainDecision file-appender info-event)))
      (is (= FilterReply/NEUTRAL (.getFilterChainDecision file-appender warn-event)))
      (is (= FilterReply/NEUTRAL (.getFilterChainDecision file-appender error-event)))))

  (testing "appender is configured with encoder generated by given :encoder-factory fn"
    (let [encoder-factory-called (atom false)
          encoder-instance (atom nil)
          start? true
          context (logger-context)
          encoder-factory (fn [args]
                            (reset! encoder-factory-called true)
                            (reset! encoder-instance (subject/simple-encoder-factory args))
                            (is (= start? (:start? args)))
                            (is (= context (:context args)))
                            @encoder-instance)
          file-appender (subject/build-appender {:context context
                                                 :encoder-factory encoder-factory
                                                 :level :info
                                                 :path "/tmp/file-logger-05"
                                                 :start? start?
                                                 :type :file})]
      (is (= true @encoder-factory-called))
      (is (= @encoder-instance (.getEncoder file-appender)))))

  (testing "appender is configured with specified file path"
    (let [path "/tmp/file-logger-6"
          file-appender (subject/build-appender {:context (logger-context)
                                                 :level :info
                                                 :path path
                                                 :start? true
                                                 :type :file})]
      (is (= path (.getFile file-appender))))))

(deftest build-logger
  (testing "logger is configured as additive when true is given for :additive?"
    (let [logger (subject/build-logger {:additive? true
                                        :appenders []
                                        :name "logger-01"})]
      (is (= true (.isAdditive logger)))))

  (testing "logger defaults to not additive when no value is given for :additive?"
    (let [logger (subject/build-logger {:appenders [] :name "logger-02"})]
      (is (= false (.isAdditive logger)))))

  (testing "logger is configured with the given context when one is given"
    (let [context (logger-context)
          logger (subject/build-logger {:appenders []
                                        :context context
                                        :name "logger-03"})]
      (is (= context (.getLoggerContext logger)))))

  (testing "logger generates new context when no :context is given"
    (let [logger (subject/build-logger {:appenders []
                                        :name "logger-04"})]
      (is (= LoggerContext (class (.getLoggerContext logger))))))

  (testing "logger has all appenders detached and stopped when true is given for :detach-and-stop-all-appenders?"
    (let [appender-name "appender-02"
          logger-name "logger-05"
          initial-logger (subject/build-logger {:appenders [{:level :info
                                                             :name appender-name
                                                             :start? true
                                                             :type :console}]
                                                :name logger-name})
          initial-appender (.getAppender initial-logger appender-name)]
      (is (= true (.isStarted initial-appender)))
      (let [logger (subject/build-logger {:appenders []
                                          :detach-and-stop-all-appenders? true
                                          :name logger-name})
            appender (.getAppender logger appender-name)]
        (is (= false (.isStarted initial-appender)))
        (is (nil? appender)))))

  (testing "by default, logger has all appenders detached and stopped when true is given for :detach-and-stop-all-appenders?"
    (let [appender-name "appender-03"
          logger-name "logger-06"
          initial-logger (subject/build-logger {:appenders [{:level :info
                                                             :name appender-name
                                                             :start? true
                                                             :type :console}]
                                                :name logger-name})
          initial-appender (.getAppender initial-logger appender-name)]
      (is (= true (.isStarted initial-appender)))
      (let [logger (subject/build-logger {:appenders []
                                          :name logger-name})
            appender (.getAppender logger appender-name)]
        (is (= false (.isStarted initial-appender)))
        (is (nil? appender)))))

  (testing "logger does not have all appenders detached and stopped when false is given for :detach-and-stop-all-appenders?"
    (let [appender-name "appender-04"
          logger-name "logger-07"
          initial-logger (subject/build-logger {:appenders [{:level :info
                                                             :name appender-name
                                                             :start? true
                                                             :type :console}]
                                                :name logger-name})
          initial-appender (.getAppender initial-logger appender-name)]
      (is (= true (.isStarted initial-appender)))
      (let [logger (subject/build-logger {:appenders []
                                          :detach-and-stop-all-appenders? false
                                          :name logger-name})
            appender (.getAppender logger appender-name)]
        (is (= true (.isStarted initial-appender)))
        (is (= initial-appender appender)))))

  (testing "logger is configured with specified :level when keyword is given"
    (let [logger (subject/build-logger {:appenders [] :level :info :name "logger-08"})]
      (is (= Level/INFO (.getLevel logger)))))

  (testing "logger is configured with specified :level when ch.qos.logback.classic.Level constant is given"
    (let [logger (subject/build-logger {:appenders [] :level Level/INFO :name "logger-09"})]
      (is (= Level/INFO (.getLevel logger)))))

  (testing "logger assumes level of INFO when nil is given for :level"
    (let [logger (subject/build-logger {:appenders [] :level nil :name "logger-10"})]
      (is (= Level/INFO (.getLevel logger)))))

  (testing "logger is configured with the specified :appenders"
    (let [console-appender-name "appender-05"
          file-appender-name "appender-06"
          path "/tmp/logger-11"
          context (logger-context)
          logger (subject/build-logger {:additive? true
                                        :appenders [{:level :info
                                                     :name console-appender-name
                                                     :start? true
                                                     :type :console}
                                                    {:level :info
                                                     :name file-appender-name
                                                     :path path
                                                     :start? true
                                                     :type :file}]
                                        :context context
                                        :name "logger-11"})
          console-appender (.getAppender logger console-appender-name)
          file-appender (.getAppender logger file-appender-name)]
      (is (= console-appender-name (.getName console-appender)))
      (is (= ConsoleAppender (class console-appender)))
      (is (= context (.getContext console-appender)))
      (is (= true (.isStarted console-appender)))
      (is (= file-appender-name (.getName file-appender)))
      (is (= FileAppender (class file-appender)))
      (is (= path (.getFile file-appender)))
      (is (= context (.getContext file-appender)))
      (is (= true (.isStarted file-appender))))))

(deftest build-loggers
  (testing "loggers are configured as additive when true is given for :additive?"
    (let [loggers (subject/build-loggers [{:additive? true
                                           :appenders []
                                           :name "loggers-01"}
                                          {:additive? true
                                           :appenders []
                                           :name "loggers-02"}])]
      (is (= true (every? #(.isAdditive %) loggers)))))

  (testing "loggers default to not additive when no value is given for :additive?"
    (let [loggers (subject/build-loggers [{:appenders [] :name "loggers-03"}
                                          {:appenders [] :name "loggers-04"}])]
      (is (= true (not-any? #(.isAdditive %) loggers)))))

  (testing "loggers are configured with the given context when one is given"
    (let [context (logger-context)
          loggers (subject/build-loggers [{:appenders [] :context context :name "loggers-05"}
                                          {:appenders [] :context context :name "loggers-06"}])]
      (is (= true (every? #(= context (.getLoggerContext %)) loggers)))))

  (testing "loggers generate new context when no :context is given"
    (let [loggers (subject/build-loggers [{:appenders [] :name "loggers-07"}
                                          {:appenders [] :name "loggers-08"}])]
      (is (= true (every? #(= LoggerContext (class (.getLoggerContext %))) loggers)))))

  (testing "loggers have all appenders detached and stopped when true is given for :detach-and-stop-all-appenders?"
    (let [appender-a-name "appenders-01"
          appender-b-name "appenders-02"
          logger-a-name "loggers-09"
          logger-b-name "loggers-10"

          [initial-logger-a initial-logger-b]
          (subject/build-loggers [{:appenders [{:level :info
                                                :name appender-a-name
                                                :start? true
                                                :type :console}]
                                   :name logger-a-name}
                                  {:appenders [{:level :info
                                                :name appender-b-name
                                                :start? true
                                                :type :console}]
                                   :name logger-b-name}])
          initial-appender-a (.getAppender initial-logger-a appender-a-name)
          initial-appender-b (.getAppender initial-logger-b appender-b-name)]
      (is (= true (.isStarted initial-appender-a)))
      (is (= true (.isStarted initial-appender-b)))
      (let [[logger-a logger-b] (subject/build-loggers [{:appenders []
                                                         :detach-and-stop-all-appenders? true
                                                         :name logger-a-name}
                                                        {:appenders []
                                                         :detach-and-stop-all-appenders? true
                                                         :name logger-b-name}])
            appender-a (.getAppender logger-a appender-a-name)
            appender-b (.getAppender logger-b appender-b-name)]
        (is (= false (.isStarted initial-appender-a)))
        (is (= false (.isStarted initial-appender-b)))
        (is (nil? appender-a))
        (is (nil? appender-b)))))

  (testing "by default, loggers have all appenders detached and stopped when true is given for :detach-and-stop-all-appenders?"
    (let [appender-a-name "appenders-03"
          appender-b-name "appenders-04"
          logger-a-name "loggers-11"
          logger-b-name "loggers-12"

          [initial-logger-a initial-logger-b]
          (subject/build-loggers [{:appenders [{:level :info
                                               :name appender-a-name
                                               :start? true
                                               :type :console}]
                                  :name logger-a-name}
                                  {:appenders [{:level :info
                                               :name appender-b-name
                                               :start? true
                                               :type :console}]
                                  :name logger-b-name}])
          initial-appender-a (.getAppender initial-logger-a appender-a-name)
          initial-appender-b (.getAppender initial-logger-b appender-b-name)]
      (is (= true (.isStarted initial-appender-a)))
      (is (= true (.isStarted initial-appender-b)))
      (let [[logger-a logger-b] (subject/build-loggers [{:appenders [] :name logger-a-name}
                                                        {:appenders [] :name logger-b-name}])
            appender-a (.getAppender logger-a appender-a-name)
            appender-b (.getAppender logger-b appender-b-name)]
        (is (= false (.isStarted initial-appender-a)))
        (is (= false (.isStarted initial-appender-b)))
        (is (nil? appender-a))
        (is (nil? appender-b)))))

  (testing "loggers do not have all appenders detached and stopped when false is given for :detach-and-stop-all-appenders?"
    (let [appender-a-name "appenders-05"
          appender-b-name "appenders-06"
          logger-a-name "loggers-13"
          logger-b-name "loggers-14"

          [initial-logger-a initial-logger-b]
          (subject/build-loggers [{:appenders [{:level :info
                                                :name appender-a-name
                                                :start? true
                                                :type :console}]
                                   :name logger-a-name}
                                  {:appenders [{:level :info
                                                :name appender-b-name
                                                :start? true
                                                :type :console}]
                                   :name logger-b-name}])

          initial-appender-a (.getAppender initial-logger-a appender-a-name)
          initial-appender-b (.getAppender initial-logger-b appender-b-name)]
      (is (= true (.isStarted initial-appender-a)))
      (is (= true (.isStarted initial-appender-b)))
      (let [[logger-a logger-b] (subject/build-loggers [{:appenders []
                                                         :detach-and-stop-all-appenders? false
                                                         :name logger-a-name}
                                                        {:appenders []
                                                         :detach-and-stop-all-appenders? false
                                                         :name logger-b-name}])
            appender-a (.getAppender logger-a appender-a-name)
            appender-b (.getAppender logger-b appender-b-name)]
        (is (= true (.isStarted initial-appender-a)))
        (is (= initial-appender-a appender-a))
        (is (= true (.isStarted initial-appender-b)))
        (is (= initial-appender-b appender-b)))))

  (testing "loggers are configured with specified :level when keyword is given"
    (let [loggers (subject/build-loggers [{:appenders [] :level :info :name "loggers-15"}
                                          {:appenders [] :level :info :name "loggers-16"}])]
      (is (= true (every? #(= Level/INFO (.getLevel %)) loggers)))))

  (testing "loggers are configured with specified :level when ch.qos.logback.classic.Level constant is given"
    (let [loggers (subject/build-loggers [{:appenders [] :level Level/INFO :name "loggers-17"}
                                          {:appenders [] :level Level/INFO :name "loggers-18"}])]
      (is (= true (every? #(= Level/INFO (.getLevel %)) loggers)))))

  (testing "loggers assume level of INFO when nil is given for :level"
    (let [loggers (subject/build-loggers [{:appenders [] :level nil :name "loggers-17"}
                                          {:appenders [] :level nil :name "loggers-18"}])]
      (is (= true (every? #(= Level/INFO (.getLevel %)) loggers)))))

  (testing "loggers are configured with the specified :appenders"
    (let [console-appender-a-name "appenders-07"
          console-appender-b-name "appenders-08"
          file-appender-a-name "appenders-09"
          file-appender-b-name "appenders-10"
          path-a "/tmp/loggers-19"
          path-b "/tmp/loggers-20"
          context-a (logger-context)
          context-b (logger-context)
          [logger-a logger-b] (subject/build-loggers [{:additive? true
                                                       :appenders [{:level :info
                                                                    :name console-appender-a-name
                                                                    :start? true
                                                                    :type :console}
                                                                   {:level :info
                                                                    :name file-appender-a-name
                                                                    :path path-a
                                                                    :start? true
                                                                    :type :file}]
                                                       :context context-a
                                                       :name "logger-19"}
                                                      {:additive? true
                                                       :appenders [{:level :info
                                                                    :name console-appender-b-name
                                                                    :start? true
                                                                    :type :console}
                                                                   {:level :info
                                                                    :name file-appender-b-name
                                                                    :path path-b
                                                                    :start? true
                                                                    :type :file}]
                                                       :context context-b
                                                       :name "logger-20"}])
          console-appender-a (.getAppender logger-a console-appender-a-name)
          file-appender-a (.getAppender logger-a file-appender-a-name)
          console-appender-b (.getAppender logger-b console-appender-b-name)
          file-appender-b (.getAppender logger-b file-appender-b-name)]
      (is (= console-appender-a-name (.getName console-appender-a)))
      (is (= ConsoleAppender (class console-appender-a)))
      (is (= context-a (.getContext console-appender-a)))
      (is (= true (.isStarted console-appender-a)))
      (is (= file-appender-a-name (.getName file-appender-a)))
      (is (= FileAppender (class file-appender-a)))
      (is (= path-a (.getFile file-appender-a)))
      (is (= context-a (.getContext file-appender-a)))
      (is (= true (.isStarted file-appender-a)))

      (is (= console-appender-b-name (.getName console-appender-b)))
      (is (= ConsoleAppender (class console-appender-b)))
      (is (= context-b (.getContext console-appender-b)))
      (is (= true (.isStarted console-appender-b)))
      (is (= file-appender-b-name (.getName file-appender-b)))
      (is (= FileAppender (class file-appender-b)))
      (is (= path-b (.getFile file-appender-b)))
      (is (= context-b (.getContext file-appender-b)))
      (is (= true (.isStarted file-appender-b))))))
