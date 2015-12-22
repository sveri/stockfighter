(ns de.sveri.stockfighter.components.ws-to-api
  (:require #_[mount.core :refer [defstate]]
    [de.sveri.stockfighter.api.websockets :as ws]
    [com.stuartsierra.component :as component]))

;(defstate ws-to-api :start (println "starting ws to api")
;          :stop (ws/close-execution-and-quote-socket))




(defrecord WebsocketsToApi []
  component/Lifecycle
  (start [component]
    (assoc component :websockets-to-api {}))
  (stop [component] (ws/close-execution-and-quote-socket) (dissoc component :websockets-to-api)))

(defn new-websockets-to-api []
  (map->WebsocketsToApi {}))
