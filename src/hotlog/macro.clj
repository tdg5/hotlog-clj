(ns hotlog.macro
  (:require [clojure.string :as cstr]))

;; Cursive-users
(declare log!)

;; use org.clojure/tools.logging if available, otherwise fallback to clj console logger
(defmacro defn-log!
  "Define a log! fn in the calling namespace. Uses org.clojure/tools.logging if available, otherwise
  falls back to logging messages to the console with println."
  []
  (try
   (eval
    `(do
      (require 'clojure.tools.logging)
      (defmacro ~'log! [& ~'args]
        `(clojure.tools.logging/log ~@~'args))))
   (catch Exception _
     (eval
      `(do
        (defmacro ~'log! [~'level ~'ex & ~'more]
          `(let [~'more# (if (instance? Throwable ~~'ex)
                         (vec ~~'more)
                         (into [~~'ex] ~~'more))]

                         (println (str *ns*) (.toUpperCase (name ~~'level))
                                  (clojure.string/join " " ~'more#))
                         (when (instance? Throwable ~~'ex)
                           (.printStackTrace ^Throwable ~~'ex))))
        (~'log! :warn (str "clojure.tools.logging not found on classpath, "
                           *ns*
                           " logging to console.")))))))
