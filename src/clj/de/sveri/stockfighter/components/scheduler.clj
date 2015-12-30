(ns de.sveri.stockfighter.components.scheduler
  (:require [mount.core :refer [defstate]]
    [immutant.scheduling :as s]
    #_[com.stuartsierra.component :as component]))

;
;(defrecord Scheduler []
;  component/Lifecycle
;  (start [component] (assoc component :scheduler {}))
;  (stop [component] (s/stop) (dissoc component :scheduler)))
;
;(defn new-scheduler []
;  (map->Scheduler {}))

(defstate quartzite :start (println "starting scheduler")
          :stop (s/stop))
