(ns de.sveri.stockfighter.api.state
  (:require [de.sveri.stockfighter.schema-api :as schem]
            [schema.core :as s]
            [taoensso.timbre :as timb]))

(def quotes-socket (atom {}))
(def executions-socket (atom {}))


(def quote-history (atom {}))
(def execution-history (atom {}))

; nav = cash + (shares * share_price)
(def booking (atom {:nav 0 :position 0 :cash 0 :avg-bid 0 :avg-ask 0 :ask-count 0 :bid-count 0 :buy-sell-lock false}))


(s/defn ->nav [new-cash :- s/Num position :- s/Num order :- schem/order]
  (+ new-cash (* position (:price order))))

(s/defn update-booking :- s/Any [order :- schem/order book-atom :- s/Any]
  (timb/info " updating " (:id order))
  (timb/info order)
  (when (= 0 (:qty order))
    (let [fills (:fills order)
         buy-or-sell (:direction order)]
     (doseq [fill fills]
       (let [add-min-position? (if (= (:direction order) "buy") + -)
             add-min-cash? (if (= (:direction order) "buy") - +)]
         (swap! book-atom (fn [b-old] (let [new-position (add-min-position? (:position b-old) (:qty fill))
                                            new-cash (add-min-cash? (:cash b-old) (* (:price fill) (:qty fill)))
                                            avg-key (if (= "buy" buy-or-sell) :avg-ask :avg-bid)
                                            avg-count-key (if (= "buy" buy-or-sell) :ask-count :bid-count)
                                            old-avg (avg-key b-old)
                                            old-count (avg-count-key b-old)
                                            new-avg (int (/ (+ old-avg (:price fill)) (if (= old-count 0) 1 2)))]
                                        (assoc b-old :position new-position
                                                     :cash new-cash
                                                     :nav (->nav new-cash new-position order)
                                                     avg-count-key (inc old-count)
                                                     avg-key new-avg))))))))
  (timb/info @book-atom))

(def order-book (atom {}))


(def open-orders (atom []))
(add-watch open-orders :orders-validation (fn [_ _ _ new] (s/validate schem/orders new)))
