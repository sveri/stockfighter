(ns de.sveri.stockfighter.user
  (:require [schema.core :as s]
            #_[mount.core :as mount]
            [clojure.tools.namespace.repl :as tn]
    [reloaded.repl :refer [go reset stop]]
    [de.sveri.stockfighter.components.components :refer [dev-system]]
    [de.sveri.stockfighter.components.server]
    [de.sveri.stockfighter.components.db]
            ))

;(defn start []
;  (s/set-fn-validation! true)
;  (mount/start))
;
;(defn stop []
;  (mount/stop))
;
;(defn reset []
;  (stop)
;  (tn/refresh :after 'de.sveri.stockfighter.user/start))



(defn start-dev-system []
  (s/set-fn-validation! true)
  (go))

(reloaded.repl/set-init! dev-system)
