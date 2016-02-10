(ns de.sveri.stockfighter.api.turn-lvl6
  (:require [de.sveri.stockfighter.api.turnbased :as turn]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.api.state :as state]
            [schema.core :as s]
            [com.rpl.specter :as spec]
            [incanter.stats :as stats]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]))



(s/defn ->avg-price [orderbooks ask-or-bid & [last]]
  (let [asks (spec/select [spec/ALL ask-or-bid spec/FIRST :price]
                          (subvec (into [] orderbooks) 0 0))]
    (if (not-empty asks)
      (int (/ (reduce + asks) (count asks)))
      0))
  )


(s/defn open-orders->position :- s/Num [open-orders :- schem/orders]
  (let [sell-orders (reduce (fn [a b] (+ a (:qty b))) 0 (filter #(= "sell" (:direction %)) open-orders))
        buy-orders (reduce (fn [a b] (+ a (:qty b))) 0 (filter #(= "buy" (:direction %)) open-orders))]
    (- buy-orders sell-orders)))

(defn get-bid-price [] (->> (state/->quotes (h/->vsa))
                            (filter #(not (nil? (:bid %))))
                            first
                            :bid
                            ))
(defn get-ask-price [] (->> (state/->quotes (h/->vsa))
                            (filter #(not (nil? (:ask %))))
                            first
                            :ask
                            ))


(def spread-trigger 800)

(defn is-cur-bid-much-smaller? [cur-bid]
  (let [avg-bid (state/get-avg-quotes :bid 120)]
    (if (and cur-bid avg-bid)
      (do #_(when (< spread-trigger (- avg-bid cur-bid)) (println avg-bid cur-bid (- avg-bid cur-bid))) (< spread-trigger (- avg-bid cur-bid)))
      false)))

(defn is-cur-ask-much-larger [cur-ask]
  (let [avg-ask (state/get-avg-quotes :ask 120)]
    ;(println cur-ask avg-ask)
    (if (and cur-ask avg-ask)
      (do #_(when (< spread-trigger (- cur-ask avg-ask)) (println cur-ask avg-ask (- cur-ask avg-ask))) (< spread-trigger (- cur-ask avg-ask)))
      false)))

(defn get-mean-ask []
  (state/->x-quantile :ask 0.5 1000))

(defn get-high-ask []
  (state/->x-quantile :ask 0.65 5000))

(defn get-low-bid []
  (state/->x-quantile :bid 0.35 5000))

(s/defn sell-and-buy [vsa :- schem/vsa
                      open-orders :- (s/atom schem/orders)]
  (let [
        orders (atom [])
        order-refs (atom [])
        last-x 100
        last-quote-bid (state/last-quote-bid)
        last-quote-ask (state/last-quote-ask)
        mean-ask (state/->x-to-y-quantile :ask 0.5 160 30)
        mean-bid (state/->x-to-y-quantile :bid 0.5 160 30)
        ask-high-border (get-high-ask)
        bid-low-border (get-low-bid)
        qty 40
        ]
    (when (< (+ 1000 ask-high-border) last-quote-ask)
      ;(println "high ask" ask-high-border last-quote-ask)
      (println "larger: sell: " (- last-quote-ask 200) " - buy: " mean-bid)
      (swap! orders conj (h/->new-order vsa "sell" (- last-quote-ask 200) qty))
      (swap! orders conj (h/->new-order vsa "buy" mean-bid qty)))
    (when (< last-quote-bid (- bid-low-border 1000))
      ;(println "small bid" last-quote-bid bid-low-border)
      (println "smaller sell: " mean-ask " - buy " (+ 200 last-quote-bid))
      (swap! orders conj (h/->new-order vsa "sell" mean-ask qty))
      (swap! orders conj (h/->new-order vsa "buy" (+ 200 last-quote-bid) qty)))
    (doseq [order @orders]
      (swap! order-refs conj (api/new-order order)))
    (doseq [ref @order-refs]
      (let [r @ref]
        (when (< 0 (get r :qty 0))
          (swap! open-orders conj r))))))


(defn entry [vsa]
  (let [venue (:venue vsa)
        stock (:stock vsa)]
    (sell-and-buy vsa state/open-orders)
    ))