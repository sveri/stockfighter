(ns de.sveri.stockfighter.api.state
  (:require [de.sveri.stockfighter.schema-api :as schem]
            [schema.core :as s]
            [taoensso.timbre :as timb]
            [de.sveri.stockfighter.service.helper :as h]
            [incanter.stats :as stats]))

(def quotes-socket (atom {}))
(def executions-socket (atom {}))


(def quote-history (atom {}))
(s/defn ->quotes :- [schem/quote] [vsa] (get @quote-history (h/->unique-key vsa)))

(defn last-quote-ask [] (:ask (first (filter #(not (nil? (:ask %))) (->quotes (h/->vsa))))))
(defn last-quote-bid [] (:bid (first (filter #(not (nil? (:bid %))) (->quotes (h/->vsa))))))

(defn get-avg-quotes [bid-or-ask last-x]
  (let [quotes (->quotes (h/->vsa))
        relevant-quotes (map bid-or-ask (h/subvec-size-or-orig (filter #(not (nil? (bid-or-ask %))) quotes) last-x))]
    ;(println (int (/ (reduce + relevant-quotes) last-x)))
    ;(println (stats/mean relevant-quotes))
    ;(println (stats/median relevant-quotes))
    ;(println "---------")
    (int (/ (reduce + relevant-quotes) last-x))))

(defn ->staticstics [bid-or-ask last-x]
  (let [quotes' (->quotes (h/->vsa))
        quotes (map bid-or-ask (h/subvec-size-or-orig (filter #(not (nil? (bid-or-ask %))) quotes') last-x))]
    (println (stats/mean quotes))
    (println (stats/median quotes))
    (println (stats/variance quotes))
    (println (stats/sd quotes))
    (println (stats/kurtosis quotes))
    (println (stats/quantile quotes))))

(defn ->x-quantile [bid-or-ask x last-x]
  (let [quotes (->quotes (h/->vsa))
        relevant-quotes (map bid-or-ask (h/subvec-size-or-orig (filter #(not (nil? (bid-or-ask %))) quotes) last-x))]
    (int (stats/quantile relevant-quotes :probs x))))

(defn ->x-to-y-quantile [bid-or-ask x from to]
  (let [quotes (->quotes (h/->vsa))
        relevant-quotes (map bid-or-ask (h/subvec-size-or-orig-from-to (filter #(not (nil? (bid-or-ask %))) quotes) from to))]
    (int (stats/quantile relevant-quotes :probs x))))



(def execution-history (atom {}))
(defn ->executions [vsa] (get @execution-history (h/->unique-key vsa)))

(defn get-avg-executed [buy-or-sell last-x]
  (let [execs (->executions (h/->vsa))
        filtered-execs (into [] (filter #(= buy-or-sell (get-in % [:order :direction])) execs))
        last-x' (if (< (count filtered-execs) last-x) (count filtered-execs) last-x) ]
    (when (< 0 last-x') (int (/ (reduce + (map :price (subvec filtered-execs 0 last-x'))) last-x')))))


(defn get-excuted-bid []
  (let [execs (->executions (h/->vsa))]
    (:price (first (filter #(= "buy" (get-in % [:order :direction])) execs)))))

(defn get-excuted-ask []
  (let [execs (->executions (h/->vsa))]
    (:price (first (filter #(= "sell" (get-in % [:order :direction])) execs)))))

; nav = cash + (shares * share_price)
(def booking (atom {:nav 0 :position 0 :cash 0 :avg-bid 0 :avg-ask 0 :ask-count 0 :bid-count 0 :buy-sell-lock false}))


(s/defn ->nav [new-cash :- s/Num position :- s/Num order :- schem/order]
  (+ new-cash (* position (:price order))))


(def finished-executions (atom #{}))

(s/defn update-booking :- s/Any [order :- schem/order book-atom :- s/Any]
  (when (and (= false (:open order)) (not (contains? @finished-executions (:id order))))
    (swap! finished-executions conj (:id order))
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
                                                      avg-key new-avg)))))))))

(def order-book (atom {}))
(defn ->orderbook [vsa] (get @order-book (h/->unique-key (:venue (h/->vsa)) (:stock (h/->vsa)))))
(defn get-order-book-ask [vsa] (-> (->orderbook vsa)
                                   first
                                   :asks
                                   first
                                   :price))


(def open-orders (atom []))
(add-watch open-orders :orders-validation (fn [_ _ _ new] (s/validate schem/orders new)))
