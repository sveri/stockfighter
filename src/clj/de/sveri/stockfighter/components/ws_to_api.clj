(ns de.sveri.stockfighter.components.ws-to-api
  (:require [mount.core :refer [defstate]]
            [de.sveri.stockfighter.api.websockets :as ws]))

(defstate ws-to-api :start (println "starting ws to api")
          :stop (ws/close-execution-and-quote-socket))


