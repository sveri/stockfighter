(ns de.sveri.stockfighter.level
  (:require [de.sveri.stockfighter.helper :as h]
            [ajax.core :refer [POST GET]]
            [de.sveri.stockfighter.comm-helper :as comm-h]))

(defn game-started [resp local-state state]
  (println "size of venues: " (count (:venues resp)))
  (println "size of tickers: " (count (:tickers resp)))
  (swap! state assoc-in [:vsa :venue] (first (:venues resp)))
  (swap! state assoc-in [:vsa :stock] (first (:tickers resp)))
  (swap! state assoc-in [:vsa :account] (:account resp))
  (swap! local-state assoc :instanceId (:instanceId resp))
  )

(defn start-game [local-state state]
  (POST "/level/start"
        {:params        {:name (:cur-level @state)}
         :headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
         :handler       #(game-started % local-state state)
         :error-handler (fn [e] (println "some error occured: " e))}))

(defn game-info [local-state state]
  (GET (str "/level/state/instance/" (:instanceId @local-state))
       {:headers       {:X-CSRF-Token (h/get-value "__anti-forgery-token")}
        :handler       #(do (println "resp" %) (swap! state assoc :game-info %))
        :error-handler (fn [e] (println "some error occured: " e))}))

(defn with-margin [m size] (merge m {:style {:margin-left (str size "px")}}))



(defn level-page [local-state state]
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
        [:td [:span (with-margin {} 20)] (str "Finished: " (get-in @state [:game-state :done]))]
        [:td [:span (with-margin {} 20)] (str "Day " (get-in @state [:game-info :details :tradingDay])
                                              " of " (get-in @state [:game-info :details :endOfTheWorldDay]))]
        [:td [:span (with-margin {} 20)] (str "Venue: " (get-in @state [:vsa :venue]))]
        [:td [:span (with-margin {} 20)] (str "Stock: " (get-in @state [:vsa :stock]))]
        [:td [:span (with-margin {} 20)] (str "Account: " (get-in @state [:vsa :account]))]
        [:td
         [:span (with-margin {} 20)]
         (str "Target Price: " (get-in @state [:game-info :flash]))]
         ;(str "Target Price: " (comm-h/extract-client-target-price (get-in @state [:game-info :flash :info])))]
        ]]]]]])