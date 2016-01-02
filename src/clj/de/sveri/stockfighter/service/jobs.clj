(ns de.sveri.stockfighter.service.jobs
  (:require [de.sveri.stockfighter.api.websockets :as api-ws]
            [de.sveri.stockfighter.api.calculations :as calc]
            [de.sveri.stockfighter.api.api :as stock-api]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.service.helper :as h]
            [clj-time.core :as time-core]
            [clj-time.coerce :as time-coerce]
            [immutant.scheduling :refer :all]))

(s/defn start-pass-averages* :- s/Any
  [{:keys [venue stock account]} :- schem/vsa send-fn :- s/Any conn-uids :- s/Any]
  (doseq [uid (:any @conn-uids)]
    (send-fn uid [:quotes/averages
                  {:bid-avg          (or (calc/get-avg-bid venue stock account api-ws/quote-history) 0)
                   :bid-avg-last-10  (or (calc/get-avg-bid venue stock account api-ws/quote-history 10) 0)
                   :bid-avg-last-100 (or (calc/get-avg-bid venue stock account api-ws/quote-history 100) 0)}])))

(s/defn start-pass-averages :- s/Any [vsa :- schem/vsa {:keys [send-fn connected-uids]} :- s/Any]
  (let [key (keyword (str "quot-avg-" (h/->unique-key vsa)))]
    (schedule #(start-pass-averages* vsa send-fn connected-uids) (-> (id key) (every 5 :seconds)))))

(defn delete-pass-averages [vsa]
  (stop (id (keyword (str "quot-avg-" (h/->unique-key vsa))))))


(s/defn start-pass-executions* :- s/Any
  [{:keys [venue stock account]} :- schem/vsa send-fn :- s/Any conn-uids :- s/Any]
  (doseq [uid (:any @conn-uids)]
    (send-fn uid [:executions/last (calc/->accumulated-executions venue stock account api-ws/execution-history)])))

(s/defn start-pass-executions :- s/Any [vsa :- schem/vsa {:keys [send-fn connected-uids]} :- s/Any]
  (let [key (keyword (str "executions" (h/->unique-key vsa)))]
    (schedule #(start-pass-executions* vsa send-fn connected-uids) (-> (id key) (every 2 :seconds)))))

(defn delete-executions [vsa]
  (stop (id (keyword (str "executions" (h/->unique-key vsa))))))


(s/defn game-info* :- s/Any
  [instance-id :- s/Num send-fn :- s/Any conn-uids :- s/Any]
  (let [game-info (stock-api/get-level-info instance-id)]
    (swap! h/common-state assoc :game-state game-info)
    (doseq [uid (:any @conn-uids)] (send-fn uid [:game/info game-info]))))

(s/defn start-game-info :- s/Any [instance-id :- s/Num vsa :- schem/vsa {:keys [send-fn connected-uids]} :- s/Any]
  (let [key (keyword (str "game-info" (h/->unique-key vsa)))]
    (schedule #(game-info* instance-id send-fn connected-uids) (-> (id key) (every 60 :seconds)))))

(defn delete-game-info [vsa]
  (stop (id (keyword (str "game-info" (h/->unique-key vsa))))))


(s/defn start-order-book* :- s/Any
  [venue stock orderbook-atom {:keys [send-fn connected-uids]} :- s/Any]
  (let [orderbook (stock-api/->orderbook venue stock)]
    (swap! orderbook-atom update (h/->unique-key venue stock) conj orderbook)
    (doseq [uid (:any @connected-uids)] (send-fn uid [:order/order-book {:orderbook orderbook}]))))

(s/defn start-order-book :- s/Any
  [venue stock orderbook-atom ws]
  (schedule #(start-order-book* venue stock orderbook-atom ws) (-> (id (str "order-book" venue stock)) (every 2000 ))))

(defn stop-order-book [venue stock]
  (stop (id (str "order-book" venue stock))))


(s/defn start-clean-open-orders* :- s/Any
  [venue stock open-orders :- (s/atom schem/orders)]
  (let [deleted-ids (atom #{})]
    (doseq [order @open-orders]
     (let [now (time-coerce/to-long (time-core/now))
           order-time (.getTime (:ts order))
           diff (- now order-time)]
       (when (< 6000 diff)
         (stock-api/delete-order venue stock (:id order))
         (swap! deleted-ids conj (:id order))
         (println "deleted: " (:id order)))))
    (swap! open-orders (fn [old-orders] (remove #(contains? @deleted-ids (:id %)) old-orders)))))

(s/defn start-clean-open-orders :- s/Any
  [venue stock open-orders :- (s/atom schem/orders)]
  (schedule #(start-clean-open-orders* venue stock open-orders) (-> (id "clean-orders") (every 1000 ))))

(defn stop-clean-open-orders []
  (stop (id "clean-orders")))
