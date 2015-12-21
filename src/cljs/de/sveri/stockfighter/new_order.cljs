(ns de.sveri.stockfighter.new-order
  (:require [schema.core :as s]
            [ajax.core :refer [POST]]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.helper :as h]))

(defn new-order [state]
  (let [vsa (:vsa @state)] (POST "/stockfighter/orders"
         {:params        (merge {:venue      (:venue vsa)
                                 :stock      (:stock vsa)
                                 :account    (:account vsa)}
                                (:new-order @state))

          :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
          :handler       (fn [e] (println e))
          :error-handler (fn [e] (println "some error occured: " e))})))

(defn new-order-page [state]
  [:div
   [:h4 "Place Order"]
   [:div.row
    [:div.col-md-2 [h/->local-text-field state "Target Price" [:new-order :price] {:type "number"}]]
    [:div.col-md-2 [h/->local-text-field state "Step" [:new-order :qty] {:type "number"}]]
    [:div.col-md-2 [h/->local-text-field state "Total Quantity" [:new-order :target-qty] {:type "number"}]]
    [:div.col-md-1
     (h/wrap-with-form "Direction"
                       [:select.form-control {:on-click #(swap! state assoc-in [:new-order :direction]
                                                                (-> % .-target .-value))
                                              :value    (get-in @state [:new-order :direction])}
                        [:option {:value "buy"} "Buy"]
                        [:option {:value "sell"} "Sell"]])]
    [:div.col-md-2
     (h/wrap-with-form "Order Type"
                       [:select.form-control {:on-click #(swap! state assoc-in [:new-order :orderType]
                                                                (-> % .-target .-value))
                                              :value    (get-in @state [:new-order :orderType])}
                        [:option {:value "limit"} "Limit"]
                        [:option {:value "market"} "Market"]
                        [:option {:value "fill-or-kill"} "fill-or-kill"]
                        [:option {:value "immediate-or-cancel"} "immediate-or-cancel"]])]
    [:div.col-md-1
     (h/wrap-with-form "" [:button.btn.btn-danger {:style {:margin-left "10px"} :on-click #(new-order state)} "Place Order"])]]])
