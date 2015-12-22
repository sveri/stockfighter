(ns de.sveri.stockfighter.components.atom-storage
  (:require [de.sveri.stockfighter.api.websockets :as ws]
            [de.sveri.stockfighter.api.orders :as o]
            [mount.core :refer [defstate]]))

(def quotes-file "./env/dev/quotes.edn")
(def executions-file "./env/dev/executions.edn")
(def order-file "./env/dev/orders.edn")

(defn load-atoms []
  (try (reset! ws/quote-history (read-string (slurp quotes-file)))
       (catch Exception e (do (println "could not load atoms") (.printStackTrace e))))
  (try (reset! ws/execution-history (read-string (slurp executions-file)))
       (catch Exception e (do (println "could not load atoms") (.printStackTrace e))))
  (try (reset! o/order-history (read-string (slurp order-file)))
       (catch Exception e (do (println "could not load atoms") (.printStackTrace e)))))

(defn store-atoms []
  (spit quotes-file (prn-str @ws/quote-history))
  (spit executions-file (prn-str @ws/execution-history))
  (spit order-file (prn-str @o/order-history)))

(defstate atom-storage :start (load-atoms)
          :stop (store-atoms))