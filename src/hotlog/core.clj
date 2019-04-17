(ns hotlog.core
  (:import [org.slf4j Logger]))

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
