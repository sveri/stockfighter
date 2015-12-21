(ns de.sveri.stockfighter.user
  (:require [clojure.tools.namespace.repl :as tn]
            [schema.core :as s]
            [mount.core :as mount]
            [de.sveri.stockfighter.components.server]
            [de.sveri.stockfighter.components.db]))

(defn start []
  (s/set-fn-validation! true)
  (mount/start))

(defn stop []
  (mount/stop))

(defn reset []
  (stop)
  (tn/refresh :after 'de.sveri.stockfighter.user/start))
