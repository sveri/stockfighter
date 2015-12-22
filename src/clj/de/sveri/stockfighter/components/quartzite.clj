(ns de.sveri.stockfighter.components.quartzite
  (:require [mount.core :refer [defstate]]
            [immutant.scheduling :refer [stop]]
            ))


(defstate quartzite :start (println "starting scheduler")
          :stop (stop))
