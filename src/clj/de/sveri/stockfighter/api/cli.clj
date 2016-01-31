(ns de.sveri.stockfighter.api.cli
  (:require [de.sveri.stockfighter.api.config :as conf]
            [de.sveri.stockfighter.api.state :as state]
            [de.sveri.stockfighter.api.websockets :as ws]
            [de.sveri.stockfighter.service.jobs :as jobs]
            [de.sveri.stockfighter.api.api :as api]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.schema-api :as schem]
            [schema.core :as s]
            [immutant.scheduling :refer :all]
            [de.sveri.stockfighter.api.lvl-four :as four]
            [de.sveri.stockfighter.api.lvl-three :as three]))


(def bot-enabled (atom false))

(def vsa (atom {}))

;(ws/connect-executions conf/vsa)

;(jobs/start-order-book conf/pvp-venue conf/pvp-stock state/order-book nil)



(s/defn ->new-order [{:keys [venue stock account]} :- schem/vsa buy-or-sell :- schem/direction price :- s/Num qty :- s/Num]
  {:account account :venue venue :stock stock :price price :qty qty :direction buy-or-sell :orderType "limit"})

(s/defn tick-bot [{:keys [venue stock] :as vsa} :- schem/vsa]
  (when @bot-enabled
    (three/be-a-market-maker-now? vsa state/open-orders
                                  (get @state/order-book (h/->unique-key venue stock)))))

(s/defn enable-bots :- s/Any
  [{:keys [venue stock account] :as vsa}]
  (println "enabling autobuy for: " venue stock account)
  (reset! bot-enabled true)
  (schedule #(tick-bot vsa) (-> (id (str "bot-" (h/->unique-key vsa))) (every 1500))))


(defn disable-bot [] (reset! bot-enabled false))







(defn start-level []
  (let [game-info (api/start-game "sell_side")]
    (if (:ok game-info)
      (do (swap! h/common-state assoc :game-info game-info)
          (reset! vsa {:account (:account game-info) :venue (first (:venues game-info)) :stock (first (:tickers game-info))}))
      (println "error starting game: " game-info))))