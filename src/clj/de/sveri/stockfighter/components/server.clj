(ns de.sveri.stockfighter.components.server
  (:require #_[mount.core :refer [defstate]]
    ;[de.sveri.stockfighter.components.config :refer [config]]
    ;[de.sveri.stockfighter.components.handler :refer [handler]]
    [taoensso.timbre :as timbre]
    [org.httpkit.server :refer [run-server]]
    [cronj.core :as cronj]
    [clojure.core.async :as async]
    [selmer.parser :as parser]
    [de.sveri.stockfighter.session :as session]
    [com.stuartsierra.component :as component])
  (:import (clojure.lang AFunction)))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "stockfigherui is shutting down...")
  (cronj/shutdown! session/cleanup-job)
  (timbre/info "shutdown complete!"))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  [config]

  (when (= (:env config) :dev) (parser/cache-off!))
  ;;start the expired session cleanup job
  (cronj/start! session/cleanup-job)
  (timbre/info "\n-=[ stockfigherui started successfully"
               (when (= (:env config) :dev) "using the development profile") "]=-"))

;(defstate server :start (run-server handler {:port (get-in config [:config :port] 3000)})
;          :stop (when (instance? AFunction server) (server)))



(defrecord WebServerProd [handler config]
  component/Lifecycle
  (start [component]
    (let [handler (:handler handler)
          server (run-server handler {:port (get-in config [:config :port] 3000)})]
      (assoc component :server server)))
  (stop [component]
    (let [server (:server component)]
      (when server (server)))
    component))

(defn new-web-server-prod []
  (map->WebServerProd {}))
