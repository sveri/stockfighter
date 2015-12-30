(ns de.sveri.stockfighter.api.lvl-three
  (:require [de.sveri.stockfighter.api.calculations :as calc]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]
            [immutant.scheduling :refer :all]))

(def default-qty 20)

(s/defn bought-sold-something? :- s/Bool
  [order :- (s/maybe (schem/error-or-succ schem/order))]
  (if (and order (:totalFilled order))
    (< 0 (:totalFilled order))
    (do (println "error occured: " order)
        false)))

(s/defn buy-or-sell? :- schem/direction [venue stock account quote-history]
  (if (< (or (calc/get-avg-bid venue stock account quote-history 2) 0)
         (or (calc/get-avg-bid venue stock account quote-history 4) 0))
    "buy"
    "sell"))
;
;(s/defn update-booking :- s/Any [order-response :- schem/order book-atom :- s/Any]
;  (let [fills (:fills order-response)]
;    (doseq [fill fills]
;      (let [add-min-position? (if (= (:direction order-response) "buy") + -)
;            add-min-cash? (if (= (:direction order-response) "buy") - +)]
;        (swap! book-atom (fn [b-old] (let [new-position (add-min-position? (:position b-old) (:qty fill))
;                                           new-cash (add-min-cash? (:cash b-old) (* (:price fill) (:qty fill)))]
;                                       (assoc b-old :position new-position
;                                                    :cash new-cash))))))))

;(s/defn ->new-order-resp :- s/Any
;  [venue :- s/Str stock :- s/Str account :- s/Str quote-history :- s/Any direction :- schem/direction
;   qty :- s/Num & [given-price :- (s/maybe s/Num)]]
;  (when-let [float-price (calc/get-avg-bid venue stock account quote-history 5)]
;    (let [price (int float-price)
;          order {:account   account :venue venue :stock stock
;                 :price     (if (and given-price (< price given-price)) given-price price)
;                 :qty       (if (= 0 qty) default-qty qty)
;                 :direction direction
;                 ;:orderType "limit"}]
;                 :orderType "immediate-or-cancel"}]
;
;      (let [o-resp (api/new-order order)]
;        ;(println "foo: " (:direction order) " - " given-price)
;        ;(println o-resp)
;        o-resp))))

(s/defn ->new-order-resp :- s/Any
  [venue :- s/Str stock :- s/Str account :- s/Str direction :- schem/direction
   qty :- s/Num price :- s/Num]
  (let [order {:account   account :venue venue :stock stock
               :price     price
               :qty       (if (= 0 qty) default-qty qty)
               :direction direction
               :orderType "limit"}]
    ;:orderType "immediate-or-cancel"}]

    (let [o-resp (api/new-order order)]
      ;(println "foo: " (:direction order) " - " given-price)
      ;(println o-resp)
      o-resp)))

;(s/defn buy-a-thing [venue stock account quote-history quote :- schem/quote]
;  (let [qty (if (< (:bidSize quote) default-qty) (:bidSize quote) default-qty)
;        order-response (->new-order-resp venue stock account quote-history "buy" qty (:bid quote))]
;    (println "qty " qty (:bidSize quote) (:bid quote))
;    (when (bought-sold-something? order-response)
;      #_(update-booking order-response booking)
;      #_(println "bought " order-response))))
;
;(s/defn sell-a-thing [venue stock account quote-history quote :- schem/quote]
;  (let [quote-price (:ask quote)
;        sell-response (->new-order-resp venue stock account quote-history "sell" (:askSize quote) quote-price)]
;    (when (bought-sold-something? sell-response)
;      #_(update-booking sell-response booking)
;      #_(println "sold: " sell-response))))

;(s/defn start-lvl-three :- s/Any
;  [{:keys [venue stock account]} :- schem/vsa quote-history :- s/Any quote :- schem/quote
;   booking :- (s/atom schem/booking)]
;  ;(clojure.pprint/pprint quote)
;  (let [bid (:bid quote)
;        ask (:ask quote)]
;    (when (and ask bid)
;      (let [diff (- ask bid)
;            bid-adapted (int (+ bid (* 0.3 diff)))
;            ask-adapted (int (- ask (* 0.3 diff)))]
;        (println quote)
;        (println bid-adapted ask-adapted)
;        (->new-order-resp venue stock account "buy" 2 bid-adapted)
;        (->new-order-resp venue stock account "sell" 2 ask-adapted))))
;  #_(cond
;      (< 200 (get @booking :position 0)) (sell-a-thing venue stock account quote-history quote)
;      (> -200 (get @booking :position 0)) (buy-a-thing venue stock account quote-history quote)
;      (= "buy" (buy-or-sell? venue stock account quote-history)) (buy-a-thing venue stock account quote-history quote)
;      (= "sell" (buy-or-sell? venue stock account quote-history)) (sell-a-thing venue stock account quote-history quote)
;      :else nil
;      ;:lse (sell-a-thing venue stock account quote-history quote)
;      ))

(def counter-order (atom nil))

(s/defn start-lvl-three :- s/Any
  [{:keys [venue stock account]} :- schem/vsa orderbook :- schem/orderbooks
   booking :- (s/atom schem/booking)]
  ;(println (first (:asks (first orderbook))))
  (let [ask (first (:asks (first orderbook)))
        bids (first (:bids (first orderbook)))
        qty 2]
    (if @counter-order
      (let [order-response (api/new-order @counter-order)]
        (when (= qty (:totalFilled order-response))
          (reset! counter-order nil)))
      (when ask
        (let [buy-order {:account   account :venue venue :stock stock
                         :price     (:price ask)
                         :qty       qty
                         :direction "buy"
                         :orderType "immediate-or-cancel"}
              sell-order {:account   account :venue venue :stock stock
                          :price     (+ (:price ask) 30)
                          :qty       qty
                          :direction "sell"
                          :orderType "immediate-or-cancel"}
              order-response (api/new-order buy-order)]
          (when (= qty (:totalFilled order-response))
            (reset! counter-order sell-order)))))
    ))