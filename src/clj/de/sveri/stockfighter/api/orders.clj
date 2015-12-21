(ns de.sveri.stockfighter.api.orders
  (:require [de.sveri.stockfighter.schema-api :as schem]
            [schema.core :as s]
            [de.sveri.stockfighter.api.api :as api]
            [immutant.scheduling :refer :all]))

(def default-key "new-order-batch")

(s/defn new-order :- s/Any [order :- schem/new-batch-order]
  (doseq [_ (range 0 (/ (:target-qty order) (:qty order)))]
    (api/new-order order)
    (Thread/sleep 2000)))

(s/defn better-new-order :- s/Any [order :- schem/new-batch-order]
  (schedule  #(api/new-order order) (-> (id default-key) (every 500))))