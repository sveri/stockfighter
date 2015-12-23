(ns de.sveri.stockfighter.new-order
  (:require [schema.core :as s]
            [ajax.core :refer [POST]]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.helper :as h]))

(s/defn ->new-order :- schem/new-batch-order [local-state :- schem/local-state state :- schem/state]
        (let [vsa (:vsa @local-state)]
          (merge vsa
                 (:new-order @state))))

(defn new-order [local-state state]
  (POST "/stockfighter/orders"
        {:params        (->new-order local-state state)
         :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
         :handler       (fn [e] (println e))
         :error-handler (fn [e] (println "some error occured: " e))}))

(defn new-autobuy [local-state state]
  (POST "/stockfighter/autobuy"
        {:params        (->new-order local-state state)
         :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
         :handler       (fn [e] (println e))
         :error-handler (fn [e] (println "some error occured: " e))}))

(defn new-autobuy-stop [local-state]
  (POST "/stockfighter/autobuy/stop"
        {:params        (:vsa @local-state)
         :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
         :handler       (fn [e] (println e))
         :error-handler (fn [e] (println "some error occured: " e))}))

(defn start-lvl-three [local-state]
  (POST "/level/three/start"
        {:params        (:vsa @local-state)
         :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
         :handler       (fn [e] (println e))
         :error-handler (fn [e] (println "some error occured: " e))}))

(defn stop-lvl-three [local-state]
  (POST "/level/three/stop"
        {:params        (:vsa @local-state)
         :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
         :handler       (fn [e] (println e))
         :error-handler (fn [e] (println "some error occured: " e))}))

(defn new-order-page [local-state state]
  [:div
   [:h4 "Place Order"]
   [:div.row
    [:div.col-md-2 [h/->local-text-field state "Target Price" [:new-order :price] {:type "number"}]]
    [:div.col-md-2 [h/->local-text-field state "Step" [:new-order :qty] {:type "number"}]]
    [:div.col-md-2 [h/->local-text-field state "Total Quantity" [:new-order :target-qty] {:type "number"}]]
    [:div.col-md-1
     (h/wrap-with-form "Direction"
                       [:select.form-control {:on-change #(swap! state assoc-in [:new-order :direction]
                                                                 (-> % .-target .-value))
                                              :value     (get-in @state [:new-order :direction])}
                        [:option {:value "buy"} "Buy"]
                        [:option {:value "sell"} "Sell"]])]
    [:div.col-md-2
     (h/wrap-with-form "Order Type"
                       [:select.form-control {:on-change #(swap! state assoc-in [:new-order :orderType]
                                                                 (-> % .-target .-value))
                                              :value     (get-in @state [:new-order :orderType])}
                        [:option {:value "limit"} "Limit"]
                        [:option {:value "market"} "Market"]
                        [:option {:value "fill-or-kill"} "fill-or-kill"]
                        [:option {:value "immediate-or-cancel"} "immediate-or-cancel"]])]
    [:div.col-md-1
     (h/wrap-with-form "" [:button.btn.btn-danger {:style    {:margin-left "10px"}
                                                   :on-click #(new-order local-state state)} "Place Order"])]]
   [:div.row
    [:div.col-md-2
     (h/wrap-with-form "" [:button.btn.btn-danger {:style    {:margin-left "10px"}
                                                   :on-click #(new-autobuy local-state state)} "Auto Buy to Target"])]
    [:div.col-md-2 (h/wrap-with-form "" [:button.btn.btn-danger {:style    {:margin-left "10px"}
                                                                 :on-click #(new-autobuy-stop local-state)} "Stop Auto Buy"])]
    [:div.col-md-2 (h/wrap-with-form "" [:button.btn.btn-danger {:style    {:margin-left "10px"}
                                                                 :on-click #(start-lvl-three local-state)} "Start Lvl 3"])]
    [:div.col-md-2 (h/wrap-with-form "" [:button.btn.btn-danger {:style    {:margin-left "10px"}
                                                                 :on-click #(stop-lvl-three local-state)} "Stop Lvl 3"])]]])
