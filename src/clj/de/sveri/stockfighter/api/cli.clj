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
            [de.sveri.stockfighter.api.turn-lvl4 :as t-four]
            [de.sveri.stockfighter.api.turn-lvl6 :as t-six]
            [live-chart :as c]))


(def bot-enabled (atom false))

;(def lvl "sell_side")
(def lvl "making_amends")
;(def lvl "irrational_exuberance")



(s/defn ->new-order [{:keys [venue stock account]} :- schem/vsa buy-or-sell :- schem/direction price :- s/Num qty :- s/Num]
  {:account account :venue venue :stock stock :price price :qty qty :direction buy-or-sell :orderType "limit"})

(def tick-allowed (atom true))


(defn start-turn-based [vsa]
  (when (and @tick-allowed @bot-enabled)
    (reset! tick-allowed false)
    (t-six/collect-accounts* vsa)
    (reset! tick-allowed true)))

(s/defn enable-bots :- s/Any
  []
  (let [vsa (h/->vsa)]
    (println "enabling autobuy for: " vsa)
    (reset! bot-enabled true)
    (schedule #(start-turn-based vsa) (-> (id (str "bot-" (h/->unique-key vsa))) (every 60000)))))


(defn disable-bot []
  (reset! bot-enabled false)
  (stop (id (str "bot-" (h/->unique-key (h/->vsa))))))







(defn start-level []
  (let [game-info @(api/start-game lvl)]
    (if (:ok game-info)
      (do
        (swap! h/common-state assoc :game-info game-info)
        (let [vsa (h/->vsa)]
          (ws/connect-executions vsa state/executions-socket state/execution-history state/booking)
          (ws/connect-quotes vsa)
          (jobs/start-order-book (:venue vsa) (:stock vsa) state/order-book nil)
          (jobs/start-clean-open-orders (:venue vsa) (:stock vsa) state/open-orders)
          (h/restart-api-websockets true)
          ;(jobs/start-correcting-orders vsa)
          ))
      (println "error starting game: " game-info))))

(defn stop-level []
  (let [resp @(api/stop-game (h/->instanceid))]
    (println "stopped level")
    (h/restart-api-websockets false)
    (clojure.pprint/pprint resp)))

(defn restart-level []
  (let [resp @(api/restart-game (h/->instanceid))]
    (println "stopped level")
    (h/restart-api-websockets true)
    (clojure.pprint/pprint resp)))


;first red
;second blue
;third green
;(c/show (c/time-chart [state/best-ask] :repaint-speed 2000) :title "some states")
;(c/show (c/time-chart [ get-order-book-ask] :repaint-speed 500 :y-min 5000) :title "some states")
;(c/show (c/time-chart [state/best-quote-ask get-avg-asks] :repaint-speed 2000 :time-periods 500 :y-min 5000 :y-max 8000) :title "some states")
;(c/show (c/time-chart [state/best-quote-ask state/best-quote-bid] :repaint-speed 2000 :time-periods 500 :y-min 5000 :y-max 8000) :title "some states")
;(c/show (c/time-chart [state/best-quote-ask state/best-quote-bid get-avg-asks] :repaint-speed 2000 :time-periods 500 :y-min 4000 :y-max 8000) :title "some states")
;(c/show (c/time-chart [t-three/get-bid-price state/get-excuted-bid] :repaint-speed 2000 :time-periods 500 :y-min 2000) :title "some states")
;(c/show (c/time-chart [t-three/get-ask-price state/get-excuted-ask] :repaint-speed 2000 :time-periods 500 :y-min 2000) :title "some states")
;(c/show (c/time-chart [state/get-excuted-bid state/get-excuted-ask] :repaint-speed 2000 :time-periods 500 :y-min 2000) :title "some states")
;(c/show (c/time-chart [state/last-quote-bid state/last-quote-ask state/get-excuted-bid get-nav] :repaint-speed 2000 :time-periods 500) :title "some states")
;(c/show (c/time-chart [state/last-quote-bid state/last-quote-ask state/get-excuted-bid state/get-excuted-ask] :repaint-speed 2000 :time-periods 500) :title "some states")
;(c/show (c/time-chart [state/last-quote-bid state/last-quote-ask] :repaint-speed 2000 :time-periods 500) :title "some states")
;(c/show (c/time-chart [state/last-quote-ask t-four/get-high-ask] :repaint-speed 2000 :time-periods 500) :title "some states")
(c/show (c/time-chart [state/last-quote-bid state/last-quote-ask] :repaint-speed 2000 :time-periods 500) :title "some states")
;
;(c/show (c/time-chart [get-nav] :repaint-speed 2000 :time-periods 500) :title "some states")