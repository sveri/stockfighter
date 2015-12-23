(ns de.sveri.stockfighter.api.orders
  (:require [de.sveri.stockfighter.schema-api :as schem]
            [schema.core :as s]
            [de.sveri.stockfighter.api.api :as api]
            [immutant.scheduling :refer :all]
            [de.sveri.stockfighter.service.helper :as h]))

;(def default-key "new-order-batch")
;
;(def order-history (atom {}))
;
;(s/defn new-order :- s/Any [order :- schem/new-batch-order]
;  (doseq [_ (range 0 (/ (:target-qty order) (:qty order)))]
;    (api/new-order order)
;    (Thread/sleep 2000)))
;
;(defn order-and-save [{:keys [venue account stock] :as order}]
;  (let [order-res (api/new-order order)
;        key (h/->unique-key venue stock account)]
;    (swap! order-history update key conj order-res)))
;
;(s/defn better-new-order :- s/Any [order :- schem/new-batch-order]
;  (let [lim (/ (:target-qty order) (:qty order))]
;    (schedule #(order-and-save order) (-> (id default-key) (every 300) (limit lim)))))