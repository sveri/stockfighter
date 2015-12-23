(ns de.sveri.stockfighter.api.lvl-three
  (:require [de.sveri.stockfighter.api.calculations :as calc]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]))


(s/defn start-lvl-three :- s/Any
  [{:keys [venue stock account] :as vsa} :- schem/vsa qty :- s/Num]
  (let [buy-price (- (calc/get-avg-bid venue stock account 5) 50)
        new-sell (+ (calc/get-avg-bid venue stock account 5) 50)
        buy-order {:account   account :venue venue :stock stock :price buy-price :qty 100 :direction "buy"
                   :orderType "buy"}]
    (api/new-order buy-order)))