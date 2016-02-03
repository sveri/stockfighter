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


(s/defn clean-open-orders :- s/Any
  [venue stock open-orders :- (s/atom schem/orders)]
  (let [deleted-ids (atom #{})
        deleted-futures (atom [])]
    (doseq [order @open-orders]
      (let [now (time-coerce/to-long (time-core/now))
            order-time (.getTime (:ts order))
            diff (- now order-time)]
        (when (< -55000 diff)
          (swap! deleted-futures conj (api/delete-order venue stock (:id order))))))
    (doseq [deleted-future @deleted-futures]
      (state/update-booking @deleted-future state/booking)
      (swap! deleted-ids conj (:id @deleted-future)))
    (swap! open-orders (fn [old-orders] (remove #(contains? @deleted-ids (:id %)) old-orders)))))

(s/defn ->avg-price [orderbooks ask-or-bid]
  (let [asks (spec/select [spec/ALL ask-or-bid spec/FIRST :price]
                          (subvec (into [] orderbooks) 0 (if (< 10 (count orderbooks)) 10 (count orderbooks))))]
    (if (not-empty asks)
      (int (/ (reduce + asks) (count asks)))
      0)))

(defn ->avg-spread [orderbooks]
  (let [avg-spread (- (->avg-price orderbooks :asks) (->avg-price orderbooks :bids))]
    (if (< 0 avg-spread) avg-spread 20)))

(defn last-ask-and-bid' [orderbooks]
  (let [order (first orderbooks)]
    (remove #(= nil %) [(first (:asks order)) (first (:bids order))])))


(s/defn sell-and-buy [vsa :- schem/vsa
                      open-orders :- (s/atom schem/orders) orderbooks :- schem/orderbooks]
  (let [avg-price (->avg-price orderbooks :asks)]
    (let [spread (->avg-spread orderbooks)
          price-adjust (- (/ spread 2) 1)
          qty-quantified 7
          orders (atom [])
          order-refs (atom [])
          buy-order-fn #(h/->new-order vsa "buy" (- avg-price price-adjust %) qty-quantified)
          sell-order-fn #(h/->new-order vsa "sell" (+ avg-price price-adjust %) qty-quantified)
      (doseq [i (range 2 4)]
        (swap! orders conj (buy-order-fn i))
        (swap! orders conj (sell-order-fn i)))
      (doseq [order @orders]
        (swap! order-refs conj (api/new-order order)))
      (doseq [ref @order-refs]
        (when (< 0 (get @ref :qty 0)) (swap! open-orders conj @ref)))
        )))
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

(s/defn correct-orders :- s/Any
  [vsa]
  (when (< (:position @state/booking) -40)
    (println "correcting buy")
    (let [avg-bid (:avg-bid @state/booking)
          order (h/->new-order vsa "buy" (- avg-bid 10) 10)]
      @(api/new-order order)))
  (when (< 40 (:position @state/booking))
    (println "correcting sell")
    (let [avg-ask (:avg-ask @state/booking)
          order (h/->new-order vsa "sell" (+ avg-ask 10) 10)]
      @(api/new-order order)))
  )

(defn entry [vsa]
  (let [venue (:venue vsa)
        stock (:stock vsa)]
    ;(println "clean")
    (time (clean-open-orders venue stock state/open-orders))
    ;(println "end clean")
    ;(println "sell/buy")
    (time (correct-orders vsa))
    (time (sell-and-buy vsa state/open-orders (get @state/order-book (h/->unique-key venue stock))))
    (println "-------------------")
    ;(println "eand sell/buy")
    ;(println "correct")

    ;(println "end correct")
    ))
