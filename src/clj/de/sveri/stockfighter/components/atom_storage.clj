(ns de.sveri.stockfighter.components.atom-storage
  (:require [de.sveri.stockfighter.api.websockets :as ws]
            [de.sveri.stockfighter.api.orders :as o]
            [de.sveri.stockfighter.service.helper :as h]
    [mount.core :refer [defstate]]
            #_[com.stuartsierra.component :as component]))

(def quotes-file "./env/dev/quotes.edn")
(def executions-file "./env/dev/executions.edn")
(def order-file "./env/dev/orders.edn")
(def common-file "./env/dev/common-state.edn")
(def booking-file "./env/dev/booking.edn")

(defn load-atoms []
  (letfn [(readfile [a path]
            (try (reset! a (read-string (slurp path)))
                 (catch Exception e (do (println "could not load atoms") (.printStackTrace e)))) )]
    ;(readfile ws/execution-history executions-file)
    (readfile h/common-state common-file)
    ;(readfile ws/booking booking-file)
    ))

(defn store-atoms []
  ;(spit quotes-file (prn-str @ws/quote-history))
  ;(spit executions-file (prn-str @ws/execution-history))
  (spit common-file (prn-str (update-in @h/common-state [:game-info] dissoc :instructions)))
  ;(spit booking-file (prn-str @ws/booking))
  ;(spit order-file (prn-str @o/order-history))
  )

;(defrecord AtomStorage []
;  component/Lifecycle
;  (start [component] (load-atoms) component)
;  (stop [component] (store-atoms) component))
;
;(defn new-atom-storage []
;  (map->AtomStorage {}))

(defstate atom-storage :start (load-atoms)
          :stop (store-atoms))