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
(defn with-width ([width] (with-width {} width)) ([m width] (merge m {:style {:width width}})))



(defn game-page [local-state state]
  [:div
   [:div.row
    [:div.col-md-12
     [:table
      [:tr {:style {:width 1400}}
       [:td
        [:input.form-control {:on-change   #(swap! state assoc :cur-level (-> % .-target .-value))
                              :placeholder "Level Name" :value (:cur-level @state)}]
        [:td [:button.btn.btn-primary (with-margin {:on-click #(start-game local-state state)} 20) "Start Game"]]
        [:td [:button.btn.btn-primary (with-margin {:on-click #(game-info local-state state)} 20) "Game Info"]]
        [:td [:span (with-margin {} 20)] (str "Day " (get-in @state [:game-info :details :tradingDay])
                                              " of " (get-in @state [:game-info :details :endOfTheWorldDay]))]
        [:td [:span (with-margin {} 20)] (str "Account: " (get-in @local-state [:vsa :account]))]
        [:td

         [:table (with-margin {} 20) [:tr [:td (with-width 70) "NAV"] [:td (with-width 90) "Cash"] [:td (with-width 50) "Pos"]
                                      [:td (with-width 50) "Bid C"] [:td (with-width 50) "Ask C"]
                                      [:td (with-width 70) "Avg Bid"] [:td (with-width 70) "Avg Ask"] [:td (with-width 50) "Spread"]]
          [:tr [:td (h/format-number (/ (get-in @state [:booking :nav]) 100))] [:td (h/format-number (/ (get-in @state [:booking :cash]) 100))]
           [:td (get-in @state [:booking :position])]
           [:td (get-in @state [:booking :bid-count])] [:td (get-in @state [:booking :ask-count])]
           [:td (get-in @state [:booking :avg-bid])] [:td (get-in @state [:booking :avg-ask])]
           [:td (- (get-in @state [:booking :avg-bid]) (get-in @state [:booking :avg-ask]))]]]]
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