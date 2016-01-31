(ns de.sveri.stockfighter.api.pvp-orchestrator
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


(def autobuy-state (atom {}))

;(ws/connect-executions conf/vsa)

;(jobs/start-order-book conf/pvp-venue conf/pvp-stock state/order-book nil)



(s/defn ->new-order [{:keys [venue stock account] :as vsa} :- schem/vsa buy-or-sell :- schem/direction price :- s/Num qty :- s/Num]
  {:account account :venue venue :stock stock :price price :qty qty :direction buy-or-sell :orderType "limit"})

(s/defn tick-bot [{:keys [venue stock account] :as vsa} :- schem/vsa]
        (let [key (h/->unique-key venue stock account)]
          (when-let [_ (key @autobuy-state)]
            (three/be-a-market-maker-now? vsa state/open-orders
                                          (get @state/order-book (h/->unique-key venue stock))))))

(s/defn enable-bots :- s/Any
        [{:keys [venue stock account] :as vsa} order :- schem/new-batch-order]
        (println "enabling autobuy for: " venue stock account)
        (swap! autobuy-state assoc (h/->unique-key venue stock account) (assoc order :level "running"))
        (schedule #(tick-bot vsa)
                  (-> (id (str "bot-" (h/->unique-key vsa))) (every 1500))))