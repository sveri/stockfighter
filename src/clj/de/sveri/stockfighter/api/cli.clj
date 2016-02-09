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
            [de.sveri.stockfighter.api.lvl-three :as three]
            [de.sveri.stockfighter.api.turn-lvl3 :as t-three]
            [de.sveri.stockfighter.api.turn-lvl4 :as t-four]
            [live-chart :as c]))


(def bot-enabled (atom false))

;(def lvl "sell_side")
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
    (t-four/entry vsa)
    (reset! tick-allowed true)))

(s/defn enable-bots :- s/Any
  []
  (let [vsa (h/->vsa)]
    (println "enabling autobuy for: " vsa)
    (reset! bot-enabled true)
    (schedule #(start-turn-based vsa) (-> (id (str "bot-" (h/->unique-key vsa))) (every 500)))))


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

(defn get-order-book-ask [] (state/get-order-book-ask (h/->vsa)))

(defn get-avg-asks [] (t-three/->avg-price (state/->orderbook (h/->vsa)) :asks))

(defn get-nav [] (int (/ (:nav @state/booking) 100)))

;(defn get-mean-bid [] (first (t-three/statistics)))
;(defn get-mean-ask [] (second (t-three/statistics)))

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
(c/show (c/time-chart [state/last-quote-bid state/last-quote-ask] :repaint-speed 2000 :time-periods 500) :title "some states")
(c/show (c/time-chart [state/last-quote-ask t-four/get-high-ask] :repaint-speed 2000 :time-periods 500) :title "some states")
(c/show (c/time-chart [state/last-quote-bid t-four/get-low-bid] :repaint-speed 2000 :time-periods 500) :title "some states")

(c/show (c/time-chart [get-nav] :repaint-speed 2000 :time-periods 500) :title "some states")