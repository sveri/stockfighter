(ns de.sveri.stockfighter.components.atom-storage
  (:require [de.sveri.stockfighter.api.websockets :as ws]
            [de.sveri.stockfighter.api.orders :as o]
            [de.sveri.stockfighter.service.helper :as h]
    #_[mount.core :refer [defstate]]
            [com.stuartsierra.component :as component]))

(def quotes-file "./env/dev/quotes.edn")
(def executions-file "./env/dev/executions.edn")
(def order-file "./env/dev/orders.edn")
(def common-file "./env/dev/common-state.edn")

(defn load-atoms []
  ;(try (reset! ws/quote-history (read-string (slurp quotes-file)))
  ;     (catch Exception e (do (println "could not load atoms") (.printStackTrace e))))
  (try (reset! ws/execution-history (read-string (slurp executions-file)))
       (catch Exception e (do (println "could not load atoms") (.printStackTrace e))))
  (try (reset! h/common-state (read-string (slurp common-file)))
       (catch Exception e (do (println "could not load atoms") (.printStackTrace e))))
  ;(try (reset! o/order-history (read-string (slurp order-file)))
  ;     (catch Exception e (do (println "could not load atoms") (.printStackTrace e))))
  )

(defn store-atoms []
  ;(spit quotes-file (prn-str @ws/quote-history))
  (spit executions-file (prn-str @ws/execution-history))
  (spit common-file (prn-str (update-in @h/common-state [:game-info] dissoc :instructions)))
  ;(spit order-file (prn-str @o/order-history))
  )

(defrecord AtomStorage []
  component/Lifecycle
  (start [component] (load-atoms) component)
  (stop [component] (store-atoms) component))

(defn new-atom-storage []
  (map->AtomStorage {}))

;(defstate atom-storage :start (load-atoms)
;          :stop (store-atoms))