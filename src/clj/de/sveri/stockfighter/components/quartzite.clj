(ns de.sveri.stockfighter.components.quartzite
  (:require [mount.core :refer [defstate]]
            ;[twarc.core :as twarc]
            ))

;(def props {:threadPool.class "org.quartz.simpl.SimpleThreadPool"
;            :threadPool.threadCount 1
;            :plugin.triggHistory.class "org.quartz.plugins.history.LoggingTriggerHistoryPlugin"
;            :plugin.jobHistory.class "org.quartz.plugins.history.LoggingJobHistoryPlugin"})


(defstate quartzite :start (println "starte")
          :stop (println "stop"))
