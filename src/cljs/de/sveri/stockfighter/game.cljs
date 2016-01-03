(ns de.sveri.stockfighter.game
  (:require [de.sveri.stockfighter.helper :as h]
            [ajax.core :refer [POST GET]]
            [de.sveri.stockfighter.new-order :as no]
            [de.sveri.stockfighter.ticker :as tick]))

(defn game-started [resp local-state]
  (println "size of venues: " (count (:venues resp)))
  (println "size of tickers: " (count (:tickers resp)))
  (swap! local-state assoc :vsa {:venue (first (:venues resp))
                                 :stock (first (:tickers resp))
                                 :account (:account resp)})
  (swap! local-state assoc :instanceId (:instanceId resp))
  )

(defn start-game [local-state state]
  (POST "/level/start"
        {:params        {:name (:cur-level @state)}
         :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
         :handler       #(game-started % local-state)
         :error-handler (fn [e] (println "some error occured: " e))}))

(defn game-info [local-state state]
  (GET (str "/level/state/instance/" (:instanceId @local-state))
       {:headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
        :handler       #(do (println "resp" %) (swap! state assoc :game-info %))
        :error-handler (fn [e] (println "some error occured: " e))}))

(defn with-margin [m size] (merge m {:style {:margin-left (str size "px")}}))



;(defn new-autobuy [local-state state]
;  (POST "/stockfighter/bots/start"
;        {:params        (merge {:level (:cur-level @state)} (->new-order local-state state))
;         :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
;         :handler       (fn [e] (println e))
;         :error-handler (fn [e] (println "some error occured: " e))}))
;
;(defn new-autobuy-stop [local-state]
;  (POST "/stockfighter/bots/stop"
;        {:params        (:vsa @local-state)
;         :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
;         :handler       (fn [e] (println e))
;         :error-handler (fn [e] (println "some error occured: " e))}))


(defn game-page [local-state state]
  [:div
   [:div.row
    [:div.col-md-12
     [:table
      [:tr {:style {:width 1200}}
       [:td
        [:input.form-control {:on-change   #(swap! state assoc :cur-level (-> % .-target .-value))
                              :placeholder "Level Name" :value (:cur-level @state)}]
        [:td [:button.btn.btn-primary (with-margin {:on-click #(start-game local-state state)} 20) "Start Game"]]
        [:td [:button.btn.btn-primary (with-margin {:on-click #(game-info local-state state)} 20) "Game Info"]]
        [:td [:span (with-margin {} 20)] (str "Day " (get-in @state [:game-info :details :tradingDay])
                                              " of " (get-in @state [:game-info :details :endOfTheWorldDay]))]
        [:td [:span (with-margin {} 20)] (str "Account: " (get-in @local-state [:vsa :account]))]
        [:td
         [:span (with-margin {} 20)]
         (str "NAV: " (get-in @state [:booking :nav]) " Cash: " (get-in @state [:booking :cash]) " AVG Bid :"
              (get-in @state [:booking :avg-bid]) " AVG Ask: " (get-in @state [:booking :avg-ask])
              "Bid Count: " (get-in @state [:booking :bid-count]) " Ask Count: " (get-in @state [:booking :ask-count]))]
        ]]]]]
   [:hr]
   [:div.row
    [:div.col-md-2
     [:button.btn.btn-primary.pull-left {:style {:margin-left "10px"} :on-click #(do (.preventDefault %)
                                                                                     (tick/start-quote-ticker local-state))} "Start All Ticker"]]
    [:div.col-md-2
     [:button.btn.btn-primary.pull-left {:style {:margin-left "10px"} :on-click #(do (.preventDefault %)
                                                                                     (tick/stop-quote-ticker local-state))} "Stop All Ticker"]]
    [:div.col-md-2
     (h/wrap-with-form "" [:button.btn.btn-danger {:style    {:margin-left "10px"}
                                                   :on-click #(no/new-autobuy local-state state)} "Start Bot"])]
    [:div.col-md-2 (h/wrap-with-form "" [:button.btn.btn-danger {:style    {:margin-left "10px"}
                                                                 :on-click #(no/new-autobuy-stop local-state)} "Stop Bot"])]]])