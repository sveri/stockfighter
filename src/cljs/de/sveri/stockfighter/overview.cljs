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
            [de.sveri.stockfighter.level :as lp]
            [de.sveri.stockfighter.executions :as exec]))

;(def local-state (alan/local-storage (atom {:venue     "" :stock "" :account ""
;                                            :new-order {:price     (js/parseInt 0) :qty (js/parseInt 100) :direction "buy"
;                                                        :orderType "limit"}})
;                                     :cljs-storage))
;(add-watch local-state :validator-watch (fn [_ _ _ new] (s/validate schem/local-state new)))

(def state (atom {:vsa       {:venue "" :stock "" :account ""}
                  :cur-level "chock_a_block"
                  :new-order {:price     (js/parseInt 0) :qty (js/parseInt 100) :target-qty 100000 :direction "buy"
                              :orderType "limit"}}))
(add-watch state :validator-watch (fn [_ _ _ new] (s/validate schem/state new)))


;(defn input-page []
;  [:div
;   [:div.row
;    [:div.col-md-4 [h/->local-text-field state "Venue" [:vsa :venue]]]
;    [:div.col-md-4 [h/->local-text-field state "Stock" [:vsa :stock]]]
;    [:div.col-md-4 [h/->local-text-field state "Account" [:vsa :account]]]]])

(defn load-orders [_]
  (let [venue (:venue @state) stock (:stock @state) account (:account @state)]
    (GET (str "/stockfighter/orders/venue/" venue "/stock/" stock "/account/" account)
         {:handler       #(swap! state assoc :orders %)
          :error-handler h/error-handler})))

(defn button-page []
  [:div.row
   ;[:button.btn.btn-primary.pull-left {:style {:margin-left "10px"} :on-click #(do (.preventDefault %)
   ;                                                                                (start-quote-ticker state))} "Start All Ticker"]
   ;[:button.btn.btn-primary.pull-left {:style {:margin-left "10px"} :on-click #(do (.preventDefault %)
   ;                                                                                (stop-quote-ticker state))} "Stop All Ticker"]
   #_[:button.btn.btn-primary.pull-right {:style {:margin-right "30px"} :on-click load-orders} "Load Orders"]])

;(s/defn
;  order-table
;  :- s/Any
;  [orders :- schem/orders]
;  [:div
;   [:hr]
;   [:table.table
;    [:thead
;     [:tr
;      [:th "Price"]
;      [:th "Direction"]
;      [:th "Original QTY"]
;      [:th "QTY"]
;      [:th "Order Type"]
;      [:th "Open?"]]]
;    [:tbody
;     (for [[idx order] (comm-h/zip (range) orders)]
;       ^{:key idx} [:tr [:td (:price order)]
;                    [:td (:direction order)]
;                    [:td (:originalQty order)]
;                    [:td (:qty order)]
;                    [:td (:orderType order)]
;                    [:td (h/bool->string (:open order))]])]]])

(defn main-page []
  (let [orders (:orders @state)]
    [:div
     [lp/level-page state state]
     ;[input-page]
     [:hr]
     [no/new-order-page state]
     ;[:hr]
     ;[button-page]
     [:hr]
     [tp/ticker-page state]
     [:hr]
     [exec/exec-page state]
     ;[qt/t]
     ;     [order-table orders]
     ]))

(defn ^:export main []
  (qt/start-router! state)
  (lp/start-game state)
  (reagent/render-component (fn [] [main-page]) (h/get-elem "app")))

