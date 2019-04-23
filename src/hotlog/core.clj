(ns hotlog.core
  (:refer-clojure :exclude [name])
  (:import [ch.qos.logback.classic Level]
           [ch.qos.logback.classic.encoder PatternLayoutEncoder]
           [ch.qos.logback.classic.filter ThresholdFilter]
           [ch.qos.logback.core ConsoleAppender FileAppender]
           [org.slf4j Logger LoggerFactory]))

(def logger-levels
  "Mapping from keyword to logback level"
  {:debug Level/DEBUG
   :error Level/ERROR
   :info Level/INFO
   :trace Level/TRACE
   :warn Level/WARN})

(def threshold-filter-levels
  "Mapping from keyword to logback level"
  (->> logger-levels
       (map (fn [[kw level]] [kw (str level)]))
       (into {})))

(def noop-logger
  "Returns a new logger instance that does nothing, but implements the required interface."
  (reify Logger
    (debug [this _] nil)
    (error [this _] nil)
    (getName [this] "NoopLogger")
    (info [this _] nil)
    (isDebugEnabled [this] false)
    (isErrorEnabled [this] false)
    (isInfoEnabled [this] false)
    (isTraceEnabled [this] false)
    (isWarnEnabled [this] false)
    (trace [this _] nil)
    (warn [this _] nil)))

(defn simple-encoder-factory
  "A simple Encoder factory function that yields a PatternLayoutEncoder with a prespecified pattern."
  [{:keys [context start?] :or {start? true}}]
  (let [encoder (doto (PatternLayoutEncoder.)
                      (.setContext context)
                      (.setPattern "%date %level %logger [%thread]: %msg%n"))]
    (when start?
      (.start encoder))
    encoder))

(defn threshold-filter
  "Constructs a ThresholdFilter from the given configuration."
  [{:keys [level start?]
                         :or {start? true}}]
  (let [filter-level (if (keyword? level)
                       (level threshold-filter-levels)
                       level)
        threshold-filter* (doto (ThresholdFilter.)
                                (.setLevel filter-level))]
    (when start?
      (.start threshold-filter*))
    threshold-filter*))

(defmulti build-appender
  "Construct an Appender of the specified :type from the given configuration."
  (fn [config] (:type config)))

(defmethod build-appender :console [{:keys [context encoder-factory level name start?]
                                     :or {encoder-factory simple-encoder-factory
                                          start? true}}]
  (let [threshold-filter* (threshold-filter {:level level :start? start?})
        appender (doto (ConsoleAppender.)
                       (.setContext context)
                       (.addFilter threshold-filter*))
        encoder (encoder-factory {:context context :start? start?})]
    (.setEncoder appender encoder)
    (when name
      (.setName appender name))
    (when start?
      (.start appender))
    appender))

(defmethod build-appender :file [{:keys [context encoder-factory level name path start?]
                                  :or {encoder-factory simple-encoder-factory
                                       start? true}}]
  (let [threshold-filter* (threshold-filter {:level level :start? start?})
        appender (doto (FileAppender.)
                   (.setFile path)
                   (.setContext context)
                   (.addFilter threshold-filter*))
        encoder (encoder-factory {:context context :start? start?})]
    (.setEncoder appender encoder)
    (when name
      (.setName appender name))
    (when start?
      (.start appender))
    appender))

(defn build-logger
  "Create a logger and many of constituents from the provided configuration."
  [{:keys [additive? appenders context detach-and-stop-all-appenders? level name]
    :or {additive? false
         context (LoggerFactory/getILoggerFactory)
         detach-and-stop-all-appenders? true
         level :info}}]
  (let [log-level (cond
                   (keyword? level) (level logger-levels)
                   (nil? level) (:info logger-levels)
                   :else level)
        logger (doto (LoggerFactory/getLogger name)
                     (.setAdditive (or additive? false))
                     (.setLevel log-level))]
    (when detach-and-stop-all-appenders?
      (.detachAndStopAllAppenders logger))
    (doseq [appender-config appenders]
      (let [appender (build-appender (assoc appender-config :context context))]
        (.addAppender logger appender)))
    logger))

(defn build-loggers
  "Build many loggers with a single call. See build-logger for the available configuration options."
  [configs]
  (->> configs
       (map build-logger)
       doall))
