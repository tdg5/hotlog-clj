(ns hotlog.macro
  (:require [clojure.string :as cstr]))

;; use org.clojure/tools.logging if available, otherwise fallback to clj console logger
(defmacro defn-log!
  "Define a log! fn identified by the given :logger-ns or the calling ns. Uses
  org.clojure/tools.logging if available. If org.clojure/tools.logging is not available, then
  logging behavior depends on the value given for the :fallback-to-println? option. When
  :fallback-to-println? is true (the default), log messages are written to the console with println.
  When :fallback-to-println? is false, log messages are swallowed."
  ([] `(defn-log! {}))
  ([{:keys [fallback-to-println? logger-ns] :or {fallback-to-println? true}}]
   (try
    (eval
     `(do
       (require 'clojure.tools.logging)
       (let [logger-ns# (or (some-> ~logger-ns name) (str *ns*))]
         (defn ~'log!
            ([~'level ~'msg]
             (~'log! ~'level nil ~'msg))
            ([~'level ~'throwable ~'msg]
             (clojure.tools.logging/log logger-ns# ~'level ~'throwable ~'msg))))))
    (catch Exception _
      (when fallback-to-println?
        (eval
         `(do
           (let [logger-ns# (or (some-> ~logger-ns name) (str *ns*))]
             (defn ~'log! [~'level ~'throwable & ~'more]
               (let [~'more# (if (instance? Throwable ~'throwable)
                               (vec ~'more)
                               (into [~'throwable] ~'more))]
                 (println logger-ns# (.toUpperCase (name ~'level)) (cstr/join " " ~'more#))
                 (when (instance? Throwable ~'throwable)
                   (.printStackTrace ^Throwable ~'throwable))))
             (~'log! :warn (str "clojure.tools.logging not found on classpath, logging to console."))))))))))
