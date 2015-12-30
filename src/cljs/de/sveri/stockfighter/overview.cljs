(ns de.sveri.stockfighter.overview
  (:require [reagent.core :as reagent :refer [atom]]
            [alandipert.storage-atom :as alan]
            [schema.core :as s]
            [ajax.core :refer [GET POST]]
            [de.sveri.stockfighter.helper :as h]
            [de.sveri.stockfighter.comm-helper :as comm-h]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.new-order :as no]
            [de.sveri.stockfighter.quotes-ticker :as qt]
            [de.sveri.stockfighter.ticker :as tp]
            [de.sveri.stockfighter.game :as game]
            [de.sveri.stockfighter.executions :as exec]))

(def local-state (alan/local-storage (atom {:vsa {:venue "" :stock "" :account ""}}) :cljs-storage))
(add-watch local-state :validator-watch (fn [_ _ _ new] (s/validate schem/local-state new)))

(def state (atom {:cur-level "sell_side"
                  :new-order {:price     (js/parseInt 5000) :qty (js/parseInt 100) :target-qty 100 :direction "buy"
                              :orderType "limit"}}))
(add-watch state :validator-watch (fn [_ _ _ new] (s/validate schem/state new)))


;var chart = new Chartist.Bar('.ct-chart', {
;                                           labels: ['W1', 'W2', 'W3', 'W4', 'W5', 'W6', 'W7', 'W8', 'W9', 'W10'],
;                                           series: [
;                                                    [1, 2, 4, 8, 6, -2, -1, -4, -6, -2]
;                                                    ]
;                                           }, {
;                                               high: 10,
;                                               low: -10,
;                                               axisX: {
;                                                       labelInterpolationFnc: function(value, index) {
;                                                                                                      return index % 2 === 0 ? value : null;
;}
;}
;});

(defn init-chart [state]
  ;google.load('visualization', '1.1', {packages: ['line']});
  ;(.load js/google "visualization" "1.1" (clj->js {:packages ["line"]}))
  ;(let [table (js/google.visualization.DataTable.)])
  (.Bar js/Chartist "#chart" (clj->js
                              {:labels ["W1"]
                               :series [[1]]})
        (clj->js {:high 10
                  :low  -10
                  }))
  (println @state)
  )

(defn main-page []
  (reagent/create-class
    {:component-did-mount
                   #(init-chart state)
     :display-name "main page"
     :reagent-render
                   (fn []
                     [:div
                      [game/game-page local-state state]
                      [:hr]
                      [:div#chart]
                      ;[no/new-order-page local-state state]
                      ;[:hr]
                      ;[tp/ticker-page local-state state]
                      ;[:hr]
                      ;[exec/exec-page state]
                      ])})
  )

(defn ^:export main []
  (qt/start-router! state)
  (reagent/render-component (fn [] [main-page]) (h/get-elem "app")))

