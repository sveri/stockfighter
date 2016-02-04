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

(def price-spread 50)

(s/defn ->avg-price [orderbooks ask-or-bid & [last]]
  (let [last' (or last 6)
        asks (spec/select [spec/ALL ask-or-bid spec/FIRST :price]
                          (subvec (into [] orderbooks) 0 (if (< last' (count orderbooks)) last' (count orderbooks))))]
    (if (not-empty asks)
      (int (/ (reduce + asks) (count asks)))
      0)))

;buy order 10
; sell order 10.40

;avg-price 10.60
; avg-buy 10.40
; avg-sell 10.80

(s/defn clean-open-orders :- s/Any
  [venue stock open-orders :- (s/atom schem/orders) orderbooks]
  (let [deleted-ids (atom #{})
        deleted-futures (atom [])
        avg-price (->avg-price orderbooks :asks)
        avg-buy (- avg-price price-spread)
        avg-sell (+ avg-price price-spread)]
    (doseq [order @open-orders]
      (when (and (= "buy" (:direction order)) (<= (+ (:price order) (* 2 price-spread)) avg-buy))
        (swap! deleted-futures conj (api/delete-order venue stock (:id order))))
      (when (and (= "sell" (:direction order)) (<= avg-sell (- (:price order) (* 2 price-spread))))
        (swap! deleted-futures conj (api/delete-order venue stock (:id order)))))
    (doseq [deleted-future @deleted-futures]
      (state/update-booking @deleted-future state/booking)
      (swap! deleted-ids conj (:id @deleted-future)))
    (swap! open-orders (fn [old-orders] (remove #(contains? @deleted-ids (:id %)) old-orders)))))

(defn ->avg-spread [orderbooks]
  (let [avg-spread (- (->avg-price orderbooks :asks 30) (->avg-price orderbooks :bids 30))]
    (if (< 0 avg-spread) avg-spread 20)))

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

(defn add-stats [a spread qty-quantified r]
  (let [cur-val (get-in a [spread :rel] 0)
        cur-overall-qty (get-in a [spread :qty] 0)
        new-spread {:rel (/ (+ cur-val (/ (get r :qty) qty-quantified)) 2) :qty (+ (get r :qty) cur-overall-qty)}]
    (swap! a assoc spread new-spread)))

(s/defn sell-and-buy [vsa :- schem/vsa
                      open-orders :- (s/atom schem/orders) orderbooks :- schem/orderbooks]
  (let [avg-price (->avg-price orderbooks :asks)
        spread (->avg-spread orderbooks)
        price-adjust (- (/ spread 2) 1)

        qty-quantified 7
        orders (atom [])
        order-refs (atom [])
        buy-order-fn #(h/->new-order vsa "buy" (- avg-price price-spread) qty-quantified)
        sell-order-fn #(h/->new-order vsa "sell" (+ avg-price price-spread) qty-quantified)
        ;buy-order-fn #(h/->new-order vsa "buy" (- avg-price price-adjust %) qty-quantified)
        ;sell-order-fn #(h/->new-order vsa "sell" (+ avg-price price-adjust %) qty-quantified)
        position (:position @state/booking)
        max-pos -31]
      ;(println (:nav @state/booking) (:position @state/booking) (- avg-price price-adjust) (+ avg-price price-adjust))
      ;(println (sort-by-val (clean-map @succ-buy-sell) :qty))
      ;(println (clean-map (sort-by-val @succ-buy-sell :qty)))
      ;(println (sort-by-val @no-succ-buy-sell :qty))
      (doseq [i (range 2 3)]
        (cond
          (and (<= position (h/abs max-pos)) (<= max-pos position)) (do  (swap! orders conj (buy-order-fn))
                                                                      (swap! orders conj (sell-order-fn)))
          (< (h/abs max-pos) position) (do  (swap! orders conj (sell-order-fn)))
          (< position max-pos) (do  (swap! orders conj (buy-order-fn))))
        )
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


(comment (s/defn sell-and-buy [vsa :- schem/vsa
                               open-orders :- (s/atom schem/orders) orderbooks :- schem/orderbooks]
           (let [avg-price (->avg-price orderbooks :asks)
                 last-ask-and-bid (last-ask-and-bid' orderbooks)
                 ]
             ;(when (= 2 (count last-ask-and-bid))
             ;(when (and (= 2 (count last-ask-and-bid)) (< (count @open-orders) 3))
             (let [spread (->avg-spread orderbooks)
                   ;min-qty (min (:qty (first last-ask-and-bid)) (:qty (second last-ask-and-bid)))
                   price-adjust (- (/ spread 2) 1)
                   ;qty (int (/ min-qty 2))
                   qty-quantified 7
                   ;qty-quantified (if (< min-qty 15) min-qty (- min-qty (int (* 0.4 min-qty))))
                   buy-order (h/->new-order vsa "buy" (- avg-price price-adjust) qty-quantified)
                   sell-order (h/->new-order vsa "sell" (+ avg-price price-adjust) qty-quantified)
                   buy-order-l (h/->new-order vsa "buy" (- avg-price 70) (- qty-quantified (int (* 0.4 qty-quantified))))
                   sell-order-l (h/->new-order vsa "sell" (+ avg-price 70) (- qty-quantified (int (* 0.4 qty-quantified))))
                   ]
               ;(println spread (->avg-spread orderbooks))
               ;(when (< 20 spread)
               (let [buy-resp (api/new-order buy-order)
                     sell-resp (api/new-order sell-order)
                     ;buy-resp-l (api/new-order buy-order-l)
                     ;sell-resp-l (api/new-order sell-order-l)
                     ]
                 (when (< 0 (get @buy-resp :qty 0)) (swap! open-orders conj @buy-resp))
                 (when (< 0 (get @sell-resp :qty 0)) (swap! open-orders conj @sell-resp))
                 ;(when (< 0 (get @buy-resp-l :qty 0)) (swap! open-orders conj @buy-resp-l))
                 ;(when (< 0 (get @sell-resp-l :qty 0)) (swap! open-orders conj @sell-resp-l))
                 )))))
;))


;(s/defn correct-orders :- s/Any
;  [vsa position-max]
;  (when (< (:position @state/booking) position-max)
;    (println "correcting buy")
;    (let [avg-bid (:avg-bid @state/booking)
;          order (h/->new-order vsa "buy" (- avg-bid 10) 10)]
;      @(api/new-order order)))
;  (when (< (Math/abs position-max) (:position @state/booking))
;    (println "correcting sell")
;    (let [avg-ask (:avg-ask @state/booking)
;          order (h/->new-order vsa "sell" (+ avg-ask 10) 10)]
;      @(api/new-order order)))
;  )

(defn entry [vsa]
  (let [venue (:venue vsa)
        stock (:stock vsa)]
    ;(println "clean")
    (clean-open-orders venue stock state/open-orders (get @state/order-book (h/->unique-key venue stock)))
    (Thread/sleep 300)
    ;(println "end clean")
    ;(println "sell/buy")
    ;(correct-orders vsa)
    (sell-and-buy vsa state/open-orders (get @state/order-book (h/->unique-key venue stock)))
    ;(println "-------------------")
    ;(println "eand sell/buy")
    ;(println "correct")

    ;(println "end correct")
    ))
