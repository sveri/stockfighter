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
            [de.sveri.stockfighter.api.turnbased :as turn]
            [de.sveri.stockfighter.api.lvl-three :as three]))


(def bot-enabled (atom false))

(def lvl "dueling_bulldozers")



(s/defn ->new-order [{:keys [venue stock account]} :- schem/vsa buy-or-sell :- schem/direction price :- s/Num qty :- s/Num]
  {:account account :venue venue :stock stock :price price :qty qty :direction buy-or-sell :orderType "limit"})

(def tick-allowed (atom true))

(s/defn tick-bot [{:keys [venue stock] :as vsa} :- schem/vsa]
  (when (and @tick-allowed @bot-enabled)
  ;(when @bot-enabled
    (reset! tick-allowed false)
    (three/be-a-market-maker-now? vsa state/open-orders
                                  (get @state/order-book (h/->unique-key venue stock)))
    (reset! tick-allowed true)))

(defn start-turn-based [vsa]
  (when (and @tick-allowed @bot-enabled)
    (reset! tick-allowed false)
    (turn/entry vsa)
    (reset! tick-allowed true)))

(s/defn enable-bots :- s/Any
  []
  ;[{:keys [venue stock account] :as vsa}]
  (let [vsa (h/->vsa)]
    (println "enabling autobuy for: " vsa)
    (reset! bot-enabled true)
    #_(schedule #(tick-bot vsa) (-> (id (str "bot-" (h/->unique-key vsa))) (every 200)))
    (schedule #(start-turn-based vsa) (-> (id (str "bot-" (h/->unique-key vsa))) (every 1000)))))


(defn disable-bot []
  (reset! bot-enabled false)
  (stop (id (str "bot-" (h/->unique-key (h/->vsa))))))







(defn start-level []
  (let [game-info @(api/start-game lvl)]
    (if (:ok game-info)
      (do
        (swap! h/common-state assoc :game-info game-info)
        (let [vsa (h/->vsa)]
          (ws/connect-executions vsa)
          (ws/connect-quotes vsa)
          (jobs/start-order-book (:venue vsa) (:stock vsa) state/order-book nil)
          ;(jobs/start-clean-open-orders (:venue vsa) (:stock vsa) state/open-orders)
          ;(jobs/start-correcting-orders vsa)
          ))
      (println "error starting game: " game-info))))

(defn stop-level []
  (let [resp @(api/stop-game (h/->instanceid))]

    (println "stopped level")
    (clojure.pprint/pprint resp)))