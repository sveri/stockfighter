(ns de.sveri.stockfighter.service.jobs
  (:require [de.sveri.stockfighter.api.state :as state]
            [de.sveri.stockfighter.api.calculations :as calc]
            [de.sveri.stockfighter.api.api :as stock-api]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.service.helper :as h]
            [clj-time.core :as time-core]
            [clj-time.coerce :as time-coerce]
            [immutant.scheduling :refer :all]
            [de.sveri.stockfighter.api.api :as api]))

(s/defn start-pass-executions* :- s/Any
  [{:keys [venue stock account]} :- schem/vsa send-fn :- s/Any conn-uids :- s/Any]
  (doseq [uid (:any @conn-uids)]
    (send-fn uid [:executions/last (calc/->accumulated-executions venue stock account state/execution-history)])))

(s/defn start-pass-executions :- s/Any [vsa :- schem/vsa {:keys [send-fn connected-uids]} :- s/Any]
  (let [key (keyword (str "executions" (h/->unique-key vsa)))]
    (schedule #(start-pass-executions* vsa send-fn connected-uids) (-> (id key) (every 2 :seconds)))))

(defn delete-executions [vsa]
  (stop (id (keyword (str "executions" (h/->unique-key vsa))))))


(s/defn game-info* :- s/Any
  [instance-id :- s/Num send-fn :- s/Any conn-uids :- s/Any]
  (let [game-info @(stock-api/get-level-info instance-id)]
    (swap! h/common-state assoc :game-state game-info)
    (doseq [uid (:any @conn-uids)] (send-fn uid [:game/info game-info]))))

(s/defn start-game-info :- s/Any [instance-id :- s/Num vsa :- schem/vsa {:keys [send-fn connected-uids]} :- s/Any]
  (let [key (keyword (str "game-info" (h/->unique-key vsa)))]
    (schedule #(game-info* instance-id send-fn connected-uids) (-> (id key) (every 60 :seconds)))))

(defn delete-game-info [vsa]
  (stop (id (keyword (str "game-info" (h/->unique-key vsa))))))


(s/defn start-order-book* :- s/Any
  [venue stock orderbook-atom {:keys [send-fn connected-uids]} :- s/Any]
  (let [orderbook @(stock-api/->orderbook venue stock)]
    (swap! orderbook-atom update (h/->unique-key venue stock) conj orderbook)
    #_(doseq [uid (:any @connected-uids)] (send-fn uid [:order/order-book {:orderbook orderbook}]))))

(s/defn start-order-book :- s/Any
  [venue stock orderbook-atom ws]
  (schedule #(start-order-book* venue stock orderbook-atom ws) (-> (id (str "order-book" venue stock)) (every 1000))))

(defn stop-order-book [venue stock]
  (stop (id (str "order-book" venue stock))))


(def can-clean (atom true))
(defn remove-already-closed [venue stock open-orders]
  (let [deleted-futures (atom [])]
    (doseq [order @open-orders]
     (swap! deleted-futures conj (api/->order-status venue stock (:id order))))
    (doseq [deleted-future @deleted-futures]
      (when-not (:open deleted-future)
        (swap! open-orders (fn [old-orders] (remove #(= (:id deleted-future) (:id %)) old-orders)))))))


(s/defn start-clean-open-orders* :- s/Any
  [venue stock open-orders :- (s/atom schem/orders)]
  (when @can-clean
    (reset! can-clean false)
    (remove-already-closed venue stock open-orders)
    (let [deleted-ids (atom #{})
          deleted-futures (atom [])
          loss-modifier 50
          price-spread 40
          avg-buy (state/get-avg-quotes :bid 10)
          avg-sell (state/get-avg-quotes :ask 20)
          ;avg-sell 20
          loss-price (+ loss-modifier (* 2 price-spread))]
      (doseq [order @open-orders]
        (when (or (and (= "buy" (:direction order)) (<= (+ (:price order) loss-price) avg-buy))
                  (and (= "sell" (:direction order)) (<= avg-sell (- (:price order) loss-price))))
          (when (= "buy" (:direction order))
            (println "avg buy" avg-buy " order-price: " (:price order)))
          (when (= "sell" (:direction order))
            (println "avg sell" avg-buy " order-price: " (:price order)))
          (swap! deleted-futures conj (api/delete-order venue stock (:id order)))))
      (doseq [deleted-future @deleted-futures]
        (state/update-booking @deleted-future state/booking)
        (swap! deleted-ids conj (:id @deleted-future)))
      (swap! open-orders (fn [old-orders] (remove #(contains? @deleted-ids (:id %)) old-orders))))
    (reset! can-clean true)))

(s/defn start-clean-open-orders :- s/Any
  [venue stock open-orders :- (s/atom schem/orders)]
  (schedule #(start-clean-open-orders* venue stock open-orders) (-> (id "clean-orders") (every 5000))))

(defn stop-clean-open-orders []
  (stop (id "clean-orders")))


(s/defn start-correcting-orders* :- s/Any
  [vsa]
  (when (< (:position @state/booking) -80)
    (println "correcting buy")
    (let [avg-bid (:avg-bid @state/booking)
          order (h/->new-order vsa "buy" (- avg-bid 10) 50)]
      @(api/new-order order)))
  (when (< 80 (:position @state/booking))
    (println "correcting sell")
    (let [avg-ask (:avg-ask @state/booking)
          order (h/->new-order vsa "sell" (+ avg-ask 10) 50)]
      @(api/new-order order))))

(s/defn start-correcting-orders :- s/Any [vsa]
  (schedule #(start-correcting-orders* vsa) (-> (id "correcting-orders") (every 10000))))

(defn stop-correcting-orders []
  (stop (id "correcting-orders")))


(s/defn start-pass-booking* :- s/Any
  [send-fn :- s/Any conn-uids :- s/Any]
  (doseq [uid (:any @conn-uids)] (send-fn uid [:game/booking @state/booking])))

(s/defn start-pass-booking :- s/Any [{:keys [send-fn connected-uids]} :- s/Any]
  (schedule #(start-pass-booking* send-fn connected-uids) (-> (id "pass-booking") (every 1 :seconds))))

(defn stop-pass-booking []
  (stop (id "pass-booking")))