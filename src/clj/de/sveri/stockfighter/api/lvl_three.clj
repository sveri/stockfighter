(ns de.sveri.stockfighter.api.lvl-three
  (:require [de.sveri.stockfighter.api.state :as state]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]
            [immutant.scheduling :refer :all]
            [com.rpl.specter :as spec]
            [de.sveri.stockfighter.api.config :as conf]
            [de.sveri.stockfighter.service.helper :as h]))



(s/defn ->avg-price [orderbooks ask-or-bid]
  (let [asks (spec/select [spec/ALL ask-or-bid spec/FIRST :price]
                          (subvec (into [] orderbooks) 0 (if (< 10 (count orderbooks)) 10 (count orderbooks))))]
    (if (not-empty asks)
      (int (/ (reduce + asks) (count asks)))
      0)))

(defn last-ask-and-bid' [orderbooks]
  (let [order (first orderbooks)]
    (remove #(= nil %) [(first (:asks order)) (first (:bids order))])))

(s/defn sell-and-buy [vsa :- schem/vsa
                      open-orders :- (s/atom schem/orders) orderbooks :- schem/orderbooks]
  (let [
        avg-price (->avg-price orderbooks :asks)
        ;buy-order (h/->new-order vsa "buy" (- avg-price 20) 10)
        ;sell-order (h/->new-order vsa "sell" (+ avg-price 20) 10)

        last-ask-and-bid (last-ask-and-bid' orderbooks)

        ;last-order-on-book (first orderbooks)
        ]
    #_(println spread-first-orderbook)

    (when (and (= 2 (count last-ask-and-bid)) (< (count @open-orders) 3))
      (let [spread (- (:price (first last-ask-and-bid)) (:price (second last-ask-and-bid)))
            ;buy-price (+ (:price (second last-ask-and-bid)) 50)
            ;sell-price (- (:price (first last-ask-and-bid)) 10)
            min-qty (min (:qty (first last-ask-and-bid)) (:qty (second last-ask-and-bid)))
            qty (int (/ min-qty 2))
            qty-quantified (- qty (int (* 0.2 qty)))
            ;sell-qty (- qty qty-quantifier)

            buy-order (h/->new-order vsa "buy" (- avg-price 20) qty-quantified)
            sell-order (h/->new-order vsa "sell" (+ avg-price 20) qty-quantified)
            ]
        (when (< 20 spread)
          ;(println "spread: " spread " - buy: " buy-price
          ;         " - sell: " sell-price)
          (let [buy-resp (api/new-order buy-order)
                sell-resp (api/new-order sell-order)]
            (when (< 0 (get @buy-resp :qty 0)) (swap! open-orders conj @buy-resp))
            (when (< 0 (get @sell-resp :qty 0)) (swap! open-orders conj @sell-resp)))
          )))))

(s/defn be-a-market-maker-now? [vsa :- schem/vsa
                                open-orders :- (s/atom schem/orders) orderbooks :- schem/orderbooks]
  (sell-and-buy vsa open-orders orderbooks))

;(def default-qty 20)
;
;(s/defn bought-sold-something? :- s/Bool
;  [order :- (s/maybe (schem/error-or-succ schem/order))]
;  (if (and order (:totalFilled order))
;    (< 0 (:totalFilled order))
;    (do (println "error occured: " order)
;        false)))
;
;(s/defn buy-or-sell? :- schem/direction [venue stock account quote-history]
;  (if (< (or (calc/get-avg-bid venue stock account quote-history 2) 0)
;         (or (calc/get-avg-bid venue stock account quote-history 4) 0))
;    "buy"
;    "sell"))
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
;      (let [o-resp @(api/new-order order)]
;        ;(println "foo: " (:direction order) " - " given-price)
;        ;(println o-resp)
;        o-resp))))

;(s/defn ->new-order-resp :- s/Any
;  [venue :- s/Str stock :- s/Str account :- s/Str direction :- schem/direction
;   qty :- s/Num price :- s/Num]
;  (let [order {:account   account :venue venue :stock stock
;               :price     price
;               :qty       (if (= 0 qty) default-qty qty)
;               :direction direction
;               :orderType "limit"}]
;    ;:orderType "immediate-or-cancel"}]
;
;    (let [o-resp @(api/new-order order)]
;      ;(println "foo: " (:direction order) " - " given-price)
;      ;(println o-resp)
;      o-resp)))

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

;(defn avg-ask [book]
;  (let [prices (spec/select [spec/ALL :asks spec/FIRST :price] book)]
;    (int (/ (reduce + prices) (count prices)))))
;
;(def ^:dynamic unlocked true)
;
;(def counter-order (atom {:tries 0 :order nil :order-result nil}))
;
;;(add-watch counter-order :co-watch (fn [_ _ _ new] #_(println new) new))
;
;(defn delete-and-reorder [venue stock account new-qty a]
;  (let [order-result (:order-result @a)
;        _ @(api/delete-order venue stock (:id order-result))
;        new-sell-order {:account   account :venue venue :stock stock
;                        :price     (- (:price order-result) 20)
;                        :qty       new-qty
;                        :direction "sell"
;                        :orderType "limit"}]
;    (reset! a {:tries 0 :order new-sell-order :order-result nil})))
;
;(defmulti sell-counter-order (fn [_ _ a] (:order-result @a)))
;
;(defmethod sell-counter-order nil [_ _ a]
;  (let [order-result @(api/new-order (:order @a))]
;    (if (= 0 (:qty order-result))
;      (reset! a {:tries 0 :order nil :order-result nil})
;      (swap! a assoc :order-result order-result))))
;
;(defmethod sell-counter-order :default [venue stock a]
;  (let [order-status @(api/->order-status venue stock (get-in @a [:order-result :id]))]
;    (if (= 0 (:qty order-status))
;      (reset! a {:tries 0 :order nil :order-result nil})
;      (if (= 20 (:tries @a))
;        (delete-and-reorder venue stock (get-in @a [:order :account]) (:qty order-status) a)
;        (swap! a update :tries + 1)))))
;
;(s/defn buy-count-order [venue stock account orderbook :- schem/order-book]
;  (when-let [ask (first (:asks orderbook))]
;    (let [ask-price (:price ask)
;          bids (first (:bids orderbook))
;          bid-price (:price bids)
;          spread (if (and ask-price bid-price) (- ask-price bid-price) nil)
;          qty (if (and ask (< (:qty ask) 31)) (:qty ask) 10)
;          buy-order {:account   account :venue venue :stock stock
;                     :price     ask-price
;                     :qty       qty
;                     :direction "buy"
;                     :orderType "immediate-or-cancel"}
;          sell-order {:account   account :venue venue :stock stock
;                      :price     (if (and spread (< spread 100))
;                                   (+ ask-price (- spread 20)) (+ ask-price 50))
;                      :qty       qty
;                      :direction "sell"
;                      :orderType "limit"}]
;      (when ask
;        (let [o-result @(api/new-order buy-order)]
;          (when (< 0 (:totalFilled o-result))
;            (swap! counter-order assoc :order sell-order)))))))
;

;(s/defn start-lvl-three :- s/Any
;  [{:keys [venue stock account]} :- schem/vsa orderbook :- schem/orderbooks booking :- (s/atom schem/booking)]
;  (when unlocked
;    (alter-var-root #'unlocked (fn [_] false))
;    (if (:order @counter-order)
;      (sell-counter-order venue stock counter-order)
;      (buy-count-order venue stock account (first orderbook)))
;    (alter-var-root #'unlocked (fn [_] true))))
;
;
;(s/defn sell-order! [{:keys [venue stock account] :as sellorder} :- schem/new-order open-orders :- (s/atom schem/orders)
;                     target-qty :- s/Num]
;  (let [sell-result @(api/new-order (assoc sellorder :qty target-qty))]
;    (when (< 0 (:qty sell-result))
;      (swap! open-orders conj sell-result)
;      (loop []
;        (let [status @(api/->order-status venue stock (:id sell-result))]
;          (when (< 0 (:qty status))
;            (Thread/sleep 500)
;            (recur))
;          #_(if (= 0 (:qty status))
;              (swap! active-bots dec)
;              (do (Thread/sleep 500) (recur)))))
;      #_(do #_(println "waiting id: " (:id sell-result))
;          (Thread/sleep 1000)
;          (let [status @(api/->order-status venue stock (:id sell-result))]
;            (if (< 0 (:qty status))
;              #_(do #_(println "delete and reorder: " (:id status))
;                  (let [delete-order-response @(api/delete-order venue stock (:id status))]
;                    #_(println delete-order-response " -= " (< (:totalFilled delete-order-response) (:qty status)) " - " (:qty delete-order-response) " - " (:qty status))
;                    (if (< (:totalFilled delete-order-response) (:qty status))
;                      (sell-order! {:account   account :venue venue :stock stock
;                                    :price     (- (:price sellorder) 20)
;                                    :qty       (- (:qty status) (:qty delete-order-response))
;                                    :direction "sell"
;                                    :orderType "limit"}
;                                   active-bots)
;                      (swap! active-bots dec))))
;              (swap! active-bots dec))))
;      )))
;
;(s/defn start-on-order [venue stock account orderbook :- schem/order-book open-orders :- (s/atom schem/orders)]
;  (when-let [ask (first (:asks orderbook))]
;    (let [ask-price (:price ask)
;          bids (first (:bids orderbook))
;          bid-price (:price bids)
;          spread (if (and ask-price bid-price) (- ask-price bid-price) nil)
;          qty (if (and ask (< (:qty ask) 31)) (:qty ask) 10)
;          buy-order {:account   account :venue venue :stock stock
;                     :price     ask-price
;                     :qty       qty
;                     :direction "buy"
;                     :orderType "immediate-or-cancel"}
;          sell-order {:account   account :venue venue :stock stock
;                      :price     (+ ask-price 20) #_(if (and spread (< spread 100))
;                                                      (+ ask-price (- spread 20)) (+ ask-price 50))
;                      :qty       qty
;                      :direction "sell"
;                      :orderType "limit"}]
;      (when ask
;        (let [o-result @(api/new-order buy-order)]
;          (when (< 0 (:totalFilled o-result))
;            (sell-order! sell-order open-orders (:totalFilled o-result))))))))
;
;
;(s/defn update-booking :- s/Any [order :- schem/order book-atom :- s/Any]
;  (let [fills (:fills order)
;        buy-or-sell (:direction order)]
;    (doseq [fill fills]
;      (let [add-min-position? (if (= (:direction order) "buy") + -)
;            add-min-cash? (if (= (:direction order) "buy") - +)]
;        (swap! book-atom (fn [b-old] (let [new-position (add-min-position? (:position b-old) (:qty fill))
;                                           new-cash (add-min-cash? (:cash b-old) (* (:price fill) (:qty fill)))
;                                           avg-key (if (= "buy" buy-or-sell) :avg-ask :avg-bid)
;                                           avg-count-key (if (= "buy" buy-or-sell) :ask-count :bid-count)
;                                           old-avg (avg-key b-old)
;                                           old-count (avg-count-key b-old)
;                                           new-avg (int (/ (+ old-avg (:price fill)) (if (= old-count 0) 1 2)))]
;                                       (assoc b-old :position new-position
;                                                    :cash new-cash
;                                                    avg-count-key (inc old-count)
;                                                    avg-key new-avg))))))))
;
;
;(defmulti buy-or-sell-when-enough (fn [_ _ _ _ _ booking] (< 2 (:position @booking))))
;
;(defmethod buy-or-sell-when-enough true
;  [venue stock account orderbook _ booking]
;  (when-let [bid (first (:bids orderbook))]
;    (let [position (:position @booking)
;          bid-price (:price bid)
;          qty position
;          ;qty (min position (:qty bid))
;          new-order {:account   account :venue venue :stock stock
;                     :price     bid-price
;                     :qty       qty
;                     :direction "sell"
;                     :orderType "immediate-or-cancel"}]
;      (when (< (+ 20 (:last-buy @booking)) bid-price)
;      ;(when (or (= 0 (:avg-ask @booking)) (< (:avg-ask @booking) bid-price))
;        @(api/new-order new-order)))))
;
;(defmethod buy-or-sell-when-enough false
;  [venue stock account orderbook orderbook-before booking]
;  (when-let [ask (first (:asks orderbook))]
;    (let [ask-price (:price ask)
;          ask-price-before (or (:price (first (:asks orderbook-before))) 0)
;          qty (if (and ask (< (:qty ask) 60)) (:qty ask) 20)
;          buy-order {:account   account :venue venue :stock stock
;                     :price     ask-price
;                     :qty       qty
;                     :direction "buy"
;                     :orderType "immediate-or-cancel"}]
;      (when (< (* 0.013 ask-price) (- ask-price-before ask-price))
;        (when-let [o-result @(api/new-order buy-order)]
;          (when (< 0 (:totalFilled o-result))
;            (swap! booking assoc :last-buy ask-price)))))))
