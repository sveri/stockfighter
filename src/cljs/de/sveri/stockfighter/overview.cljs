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

(def local-state (alan/local-storage (atom {}) :cljs-storage))
(add-watch local-state :validator-watch (fn [_ _ _ new] (s/validate schem/local-state new)))

(def state (atom {:vsa       {:venue "" :stock "" :account ""}
                  :cur-level "sell_side"
                  :new-order {:price     (js/parseInt 5000) :qty (js/parseInt 100) :target-qty 100 :direction "buy"
                              :orderType "limit"}}))
(add-watch state :validator-watch (fn [_ _ _ new] (s/validate schem/state new)))


;(defn load-orders [_]
;  (let [venue (:venue @state) stock (:stock @state) account (:account @state)]
;    (GET (str "/stockfighter/orders/venue/" venue "/stock/" stock "/account/" account)
;         {:handler       #(swap! state assoc :orders %)
;          :error-handler h/error-handler})))

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
     [lp/level-page local-state state]
     [:hr]
     [no/new-order-page state]
     [:hr]
     [tp/ticker-page state]
     [:hr]
     [exec/exec-page state]
     ]))

(defn ^:export main []
  (qt/start-router! state)
  ;(lp/start-game state)
  (reagent/render-component (fn [] [main-page]) (h/get-elem "app")))

