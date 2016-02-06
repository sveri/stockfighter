(ns de.sveri.stockfighter.api.turnbased
  (:require [de.sveri.stockfighter.service.jobs :as jobs]
            [de.sveri.stockfighter.api.state :as state]
            [de.sveri.stockfighter.api.api :as api]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.schema-api :as schem]
            [schema.core :as s]
            [clj-time.core :as time-core]
            [clj-time.coerce :as time-coerce]
            [com.rpl.specter :as spec]))




(defn last-ask-and-bid' [orderbooks]
  (let [order (first orderbooks)]
    (remove #(= nil %) [(first (:asks order)) (first (:bids order))])))

; spread to buy and sell rate {spread {:rel spread-relation :qty all-over-qty}}
(def succ-buy-sell (atom {}))
(def no-succ-buy-sell (atom {}))

(defn sort-by-val [m nested-key]
  (into (sorted-map-by (fn [key1 key2]
                         (compare [(get-in m [key2 nested-key]) key2]
                                  [(get-in m [key1 nested-key]) key1])))
        m))

(defn clean-map [m]
  (filter (fn [e] (< 200 (:qty (second e)))) m))


(def price-spread 40)

(def cancelled-order-count (atom 0))

(s/defn ->avg-price [orderbooks ask-or-bid & [last]]
  (let [last' (or last 10)
        asks (spec/select [spec/ALL ask-or-bid spec/FIRST :price]
                          (subvec (into [] orderbooks) 0 (if (< last' (count orderbooks)) last' (count orderbooks))))]
    (if (not-empty asks)
      (int (/ (reduce + asks) (count asks)))
      0)))

(s/defn clean-open-orders :- s/Any
  [venue stock open-orders :- (s/atom schem/orders) bid-ask-mean]
  (let [deleted-ids (atom #{})
        deleted-futures (atom [])
        avg-buy (- bid-ask-mean price-spread)
        avg-sell (+ bid-ask-mean price-spread)
        loss-modifier 50
        loss-price (+ loss-modifier (* 2 price-spread))
        ;best-ask (state/best-ask)
        ;best-bid (state/best-bid)
        ]
    (doseq [order @open-orders]
      (when (or (and (= "buy" (:direction order)) (<= (+ (:price order) loss-price) avg-buy))
                (and (= "sell" (:direction order)) (<= avg-sell (- (:price order) loss-price))))
        (swap! deleted-futures conj (api/delete-order venue stock (:id order)))
        (swap! cancelled-order-count + (:qty order))))
    (doseq [deleted-future @deleted-futures]
      (state/update-booking @deleted-future state/booking)
      (swap! deleted-ids conj (:id @deleted-future)))
    (swap! open-orders (fn [old-orders] (remove #(contains? @deleted-ids (:id %)) old-orders)))))

(defn ->avg-spread [orderbooks]
  (let [avg-spread (- (->avg-price orderbooks :asks 30) (->avg-price orderbooks :bids 30))]
    (if (< 0 avg-spread) avg-spread 20)))

(defn add-stats [a spread qty-quantified r]
  (let [cur-val (get-in a [spread :rel] 0)
        cur-overall-qty (get-in a [spread :qty] 0)
        new-spread {:rel (/ (+ cur-val (/ (get r :qty) qty-quantified)) 2) :qty (+ (get r :qty) cur-overall-qty)}]
    (swap! a assoc spread new-spread)))

(s/defn open-orders->position :- s/Num [open-orders :- (s/atom schem/orders)]
  (let [sell-orders (reduce (fn [a b] (+ a (:qty b))) 0 (filter #(= "sell" (:direction %)) open-orders))
        buy-orders (reduce (fn [a b] (+ a (:qty b))) 0 (filter #(= "buy" (:direction %)) open-orders))]
    (- buy-orders sell-orders)))

(s/defn sell-and-buy [vsa :- schem/vsa
                      open-orders :- (s/atom schem/orders) orderbooks :- schem/orderbooks
                      avg-price]
  (let [spread (->avg-spread orderbooks)
        qty-quantified 20
        orders (atom [])
        order-refs (atom [])
        buy-order-fn #(h/->new-order vsa "buy" (- avg-price price-spread) qty-quantified)
        sell-order-fn #(h/->new-order vsa "sell" (+ avg-price price-spread) qty-quantified)
        position (+ (:position @state/booking) (open-orders->position @state/open-orders))
        max-pos -60]
    (cond
      (and (<= position (h/abs max-pos)) (<= max-pos position)) (do (swap! orders conj (buy-order-fn))
                                                                    (swap! orders conj (sell-order-fn)))
      (< (h/abs max-pos) position) (do (swap! orders conj (sell-order-fn)))
      (< position max-pos) (do (swap! orders conj (buy-order-fn))))
    (doseq [order @orders]
      (swap! order-refs conj (api/new-order order)))
    (doseq [ref @order-refs]
      (let [r @ref]
        (when (< 0 (get r :qty 0))
          (if (and (< position (h/abs max-pos)) (< max-pos position))
            (add-stats succ-buy-sell spread qty-quantified r)
            (add-stats no-succ-buy-sell spread qty-quantified r))
          (swap! open-orders conj r))))
    ))
;))

(defn get-bid-ask-mean [orderbooks & [last]]
  (let [avg-ask (->avg-price orderbooks :asks (or last 6))
        avg-bid (->avg-price orderbooks :bids (or last 6))
        avg-spread (- avg-ask avg-bid)]
    (+ avg-bid (int (/ avg-spread 2)))))

(defn entry [vsa]
  (let [venue (:venue vsa)
        stock (:stock vsa)
        orderbooks (get @state/order-book (h/->unique-key venue stock))]
    ;(println bid-ask-mean avg-ask avg-bid avg-spread)
    (println @state/booking)
    (println @cancelled-order-count)
    (clean-open-orders venue stock state/open-orders (get-bid-ask-mean orderbooks))
    ;(Thread/sleep 300)
    ;(println "end clean")
    ;(println "sell/buy")
    ;(correct-orders vsa)
    (sell-and-buy vsa state/open-orders orderbooks (get-bid-ask-mean orderbooks 1))
    ;(println "-------------------")
    ;(println "eand sell/buy")
    ;(println "correct")

    ;(println "end correct")
    ))
