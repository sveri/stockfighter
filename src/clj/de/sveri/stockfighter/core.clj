(ns de.sveri.stockfighter.core
  (:require [taoensso.timbre :as timbre]
            [de.sveri.stockfighter.cljccore :as cljc]
            [de.sveri.stockfighter.components.server]
            [mount.core :as mount])
  (:gen-class))

(defn -main [& args]
  ;(mount/start)
  (cljc/foo-cljc "hello from cljx")
  (timbre/info "server started."))
