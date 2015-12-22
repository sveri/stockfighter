(ns de.sveri.stockfighter.ticker
  (:require [ajax.core :refer [POST]]
            [de.sveri.stockfighter.helper :as h]))



(defn start-quote-ticker [local-state]
  (let [vsa (:vsa @local-state)] (POST "/stockfighter/ticker/start"
                                 {:params        vsa
                                  :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
                                  :handler       h/success-handler
                                  :error-handler h/error-handler})))

(defn stop-quote-ticker [local-state]
  (let [vsa (:vsa @local-state)] (POST "/stockfighter/ticker/stop"
                                 {:params        vsa
                                  :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
                                  :handler       h/success-handler
                                  :error-handler h/error-handler})))

(defn ticker-page [local-state state]
  [:div
   [:table.table
    [:thead
     [:tr [:td [:button.btn.btn-primary.pull-left {:style {:margin-left "10px"} :on-click #(do (.preventDefault %)
                                                                                               (start-quote-ticker local-state))} "Start All Ticker"]]
      [:td [:button.btn.btn-primary.pull-left {:style {:margin-left "10px"} :on-click #(do (.preventDefault %)
                                                                                           (stop-quote-ticker local-state))} "Stop All Ticker"]]
      [:td "Average Bid"] [:td "Average Bid Last 10"] [:td "Average Bid Last 100"]]]
    [:tbody
     [:tr

      [:td]
      [:td]
      [:td (get-in @state [:ticker :bid-avg])]
      [:td (get-in @state [:ticker :bid-avg-last-10])]
      [:td (get-in @state [:ticker :bid-avg-last-100])]]]]])
