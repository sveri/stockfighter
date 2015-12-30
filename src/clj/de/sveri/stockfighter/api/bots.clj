(ns de.sveri.stockfighter.api.bots
  (:require [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.schema-api :as schem]
            [schema.core :as s]
            [de.sveri.stockfighter.api.lvl-two :as two]
            [de.sveri.stockfighter.api.lvl-three :as three]
            [de.sveri.stockfighter.api.websockets :as ws]
            [immutant.scheduling :refer :all]))


(def autobuy-state (atom {}))



(s/defn tick-bot [{:keys [venue stock account] :as vsa} :- schem/vsa orderbook :- schem/orderbooks
                  booking :- (s/atom schem/booking)]
  (let [key (h/->unique-key venue stock account)
        lvl (:level (key @autobuy-state))]
    (when-let [autobuy-data (key @autobuy-state)]
      (cond
        ;(= lvl "chock_a_block") (two/autobuy autobuy-data quote)
        (= lvl "sell_side") (three/start-lvl-three vsa orderbook booking)))))

(s/defn enable-bots :- s/Any
  [{:keys [venue stock account] :as vsa} order :- schem/new-batch-order level :- schem/levels]
  (println "enabling autobuy for: " venue stock account " and level: " level)
  (swap! autobuy-state assoc (h/->unique-key venue stock account) (assoc order :level level))
  (schedule #(tick-bot vsa (get @ws/order-book (h/->unique-key venue stock)) ws/booking)
            (-> (id (str "bot-" (h/->unique-key vsa))) (every 2 :seconds))))

(s/defn disable-bots :- s/Any [vsa :- schem/vsa]
  (let [key (h/->unique-key vsa)
        lvl (:level (key @autobuy-state))]
    (println "disabling autobuy for: " vsa " and level: " lvl)
    (swap! autobuy-state dissoc key)
    (stop (id (str "bot-" (h/->unique-key vsa))))))

