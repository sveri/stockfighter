(ns de.sveri.stockfighter.api.turn-lvl4
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

(defn bid-much-larger-than-last-one? [cur-buy-price]
  (let [last-executed-bid (state/get-excuted-bid)
        best-quote-bid (state/last-quote-bid)]))

(defn is-cur-much-larger-than-avg? []
  (let [last-executed-bid (state/get-excuted-bid)
        avg-quotes (state/get-avg-quotes :bid 30)]

    (if (and last-executed-bid avg-quotes)
      (do (println avg-quotes last-executed-bid (< 1500 (- last-executed-bid avg-quotes)))
          (< 1000 (- last-executed-bid avg-quotes)))
      false)))

(def spread-trigger 1000)

(defn is-cur-bid-much-smaller? [cur-bid]
  (let [avg-bid (state/get-avg-quotes :bid 20)]
    (if (and cur-bid avg-bid)
      (do (when (< spread-trigger (- avg-bid cur-bid)) (println avg-bid cur-bid (- avg-bid cur-bid))) (< spread-trigger (- avg-bid cur-bid)))
      false)))

(defn is-cur-ask-much-larger [cur-ask]
  (let [avg-ask (state/get-avg-quotes :ask 20)]
    (if (and cur-ask avg-ask)
      (do (when (< spread-trigger (- cur-ask avg-ask)) (println cur-ask avg-ask (- cur-ask avg-ask))) (< spread-trigger (- cur-ask avg-ask)))
      false)))

(s/defn sell-and-buy [vsa :- schem/vsa
                      open-orders :- (s/atom schem/orders)]
  (let [
        ;qty-quantified 150
        orders (atom [])
        order-refs (atom [])
        ;last-executed-bid (state/get-excuted-bid)
        last-quote-bid (state/last-quote-bid)
        last-quote-ask (state/last-quote-ask)
        avg-buy (state/get-avg-quotes :bid 30)
        avg-sell (state/get-avg-quotes :ask 30)
        ;buy-price (- avg-buy 40)
        ;sell-qty (if (< 200 (:position @state/booking)) 75 (:position @state/booking))
        ;position (+ (:position @state/booking) (open-orders->position @state/open-orders))
        ;open-orders-pos (open-orders->position @state/open-orders)
        qty 100
        ]
    (when (is-cur-ask-much-larger last-quote-ask)
      (println "larger: sell: " (- last-quote-ask 200) " - buy: " avg-buy)
      (swap! orders conj (h/->new-order vsa "sell" (- last-quote-ask 200) qty))
      (swap! orders conj (h/->new-order vsa "buy" avg-buy qty)))
    (when (is-cur-bid-much-smaller? last-quote-bid)
      (println "smaller sell: " avg-sell " - buy " (+ 200 last-quote-bid))
      (swap! orders conj (h/->new-order vsa "sell" avg-sell qty))
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