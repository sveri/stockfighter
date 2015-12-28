(ns de.sveri.stockfighter.api.lvl-three
  (:require [de.sveri.stockfighter.api.calculations :as calc]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]
            [immutant.scheduling :refer :all]))


; nav = cash + (shares * share_price)
(def booking (atom {:nav 0 :position 0 :cash 0}))

(s/defn bought-sold-something? :- s/Bool [order :- (s/maybe schem/order)] (if order (< 0 (:totalFilled order)) false))

(s/defn buy-or-sell? :- schem/direction [venue stock account quote-history]
  (if (< (calc/get-avg-bid venue stock account quote-history 5)
         (calc/get-avg-bid venue stock account quote-history 10))
    "buy"
    "sell"))

(s/defn update-booking :- s/Any [order-response :- schem/order book-atom :- s/Any]
  (let [fills (:fills order-response)]
    (doseq [fill fills]
      (let [add-min-position? (if (= (:direction order-response) "buy") + -)
            add-min-cash? (if (= (:direction order-response) "buy") - +)]
        (swap! book-atom (fn [b-old] (let [new-position (add-min-position? (:position b-old) (:qty fill))
                                           new-cash (add-min-cash? (:cash b-old) (* (:price fill) (:qty fill)))]
                                       (assoc b-old :position new-position
                                                    :cash new-cash))))))))

(s/defn ->new-order-resp :- s/Any
  [venue :- s/Str stock :- s/Str account :- s/Str quote-history :- s/Any direction :- schem/direction
   qty :- s/Num & [given-price :- s/Num]]
  (when-let [float-price (calc/get-avg-bid venue stock account quote-history 5)]
    (let [price (int float-price)
          order {:account   account :venue venue :stock stock
                 :price     (if (< price given-price) given-price price)
                 :qty       qty :direction direction
                 :orderType "immediate-or-cancel"}]
      (api/new-order order))))

(defn buy-a-thing [venue stock account quote-history]
  (let [order-response (->new-order-resp venue stock account quote-history "buy" 50)]
    (when (bought-sold-something? order-response)
      (update-booking order-response booking)
      (println "bought " order-response))))

(s/defn sell-a-thing [venue stock account quote-history quote :- schem/quote]
  (let [quote-price (:ask quote)
        sell-response (->new-order-resp venue stock account quote-history "sell" (:askSize quote) quote-price)]
    (when (bought-sold-something? sell-response)
      (update-booking sell-response booking)
      (println "sold: " sell-response))))

(s/defn start-lvl-three :- s/Any
  [{:keys [venue stock account]} :- schem/vsa quote-history :- s/Any quote :- schem/quote]
  (cond
    (< 200 (get @booking :position 0)) (sell-a-thing venue stock account quote-history quote)
    (< -400 (get @booking :position 0)) (buy-a-thing venue stock account quote-history)
    (= "buy" (buy-or-sell? venue stock account quote-history)) (buy-a-thing venue stock account quote-history)
    :else (sell-a-thing venue stock account quote-history)))