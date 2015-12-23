(ns de.sveri.stockfighter.api.lvl-three
  (:require [de.sveri.stockfighter.api.websockets :as api-ws]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]))

(s/defn start-lvl-three :- s/Any [{:keys [venue stock account] :as vsa} :- schem/vsa]
  (let [new-buy (- (api-ws/get-avg-bid venue stock account 5) 50)
        new-sell (+ (api-ws/get-avg-bid venue stock account 5) 50)]))