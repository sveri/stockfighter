(ns de.sveri.stockfighter.api.turn-lvl3
  (:require [de.sveri.stockfighter.api.turnbased :as turn]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.api.state :as state]
            [schema.core :as s]
            [com.rpl.specter :as spec]
            [incanter.stats :as stats]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]))


(s/defn clean-open-orders :- s/Any
  [venue stock open-orders :- (s/atom schem/orders) bid-ask-mean]
  (let [deleted-ids (atom #{})
        deleted-futures (atom [])
        price-spread 70
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
        (swap! deleted-futures conj (api/delete-order venue stock (:id order)))))
    (doseq [deleted-future @deleted-futures]
      (state/update-booking @deleted-future state/booking)
      (swap! deleted-ids conj (:id @deleted-future))
      (println @deleted-ids))
    (swap! open-orders (fn [old-orders] (remove #(contains? @deleted-ids (:id %)) old-orders)))))


(defn get-avg-spread [last-x]
  (let [{:keys [venue stock]} (h/->vsa)
        orderbooks (get @state/order-book (h/->unique-key venue stock))
        bids (spec/select [spec/ALL :bids spec/FIRST :price]
                          (h/subvec-size-or-orig orderbooks last-x))
        mean-bid (stats/mean bids)
        asks (spec/select [spec/ALL :asks spec/FIRST :price]
                          (h/subvec-size-or-orig orderbooks last-x))
        mean-ask (stats/mean asks)]
    (- mean-ask mean-bid)
    ))


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

(defn get-last-bid []
  (->> (state/->quotes (h/->vsa))
       (filter #(not (nil? (:bid %))))
       first
       :bid
       ))



(s/defn sell-and-buy [vsa :- schem/vsa
                      open-orders :- (s/atom schem/orders)]
  (let [
        bid-price' (get-bid-price)
        ask-price' (get-ask-price)
        spread (int (/ (- ask-price' bid-price' 10) 2))
        bid-price (+ bid-price' spread)
        ask-price (- ask-price' spread)
        qty-quantified 150
        orders (atom [])
        order-refs (atom [])
        ;buy-order-fn #(h/->new-order vsa "buy" bid-price qty-quantified)
        ;sell-order-fn #(h/->new-order vsa "sell" ask-price qty-quantified)
        last-executed-bid (state/get-excuted-bid)
        avg-executed-buy (if (nil? (state/get-avg-executed "buy" 10)) bid-price (state/get-avg-executed "buy" 10))
        avg-buy (state/get-avg-quotes :bid 5)
        buy-price (- avg-buy 40)
        ;avg-executed-sell (if (nil? (state/get-avg-executed "sell" 10)) ask-price (state/get-avg-executed "sell" 10))
        sell-qty (if (< 200 (:position @state/booking)) 75 (:position @state/booking))
        position (+ (:position @state/booking) (open-orders->position @state/open-orders))
        open-orders-pos (open-orders->position @state/open-orders)
        ;max-pos -200
        ]
    (when (and (< open-orders-pos 250) (< -250 open-orders-pos) )
      (cond
        (< 0 position) (do (swap! orders conj (h/->new-order vsa "sell" (+ 100 last-executed-bid) sell-qty)))
        (<= position 0) (do (swap! orders conj (h/->new-order vsa "buy" buy-price qty-quantified))))
      (doseq [order @orders]
        (swap! order-refs conj (api/new-order order)))
      (doseq [ref @order-refs]
        (let [r @ref]
          (when (< 0 (get r :qty 0))
            #_(if (and (< position (h/abs max-pos)) (< max-pos position))
                (add-stats succ-buy-sell spread qty-quantified r)
                (add-stats no-succ-buy-sell spread qty-quantified r))
            (swap! open-orders conj r))))
      )

    ;(when (< 0 (get @buy-resp
    ;(cond
    ;  (comment (and (<= position (h/abs max-pos)) (<= max-pos position)) (do (swap! orders conj (buy-order-fn))
    ;                                                                         (swap! orders conj (sell-order-fn))))
    ;  (< (h/abs max-pos) position) (do (swap! orders conj (h/->new-order vsa "sell" avg-executed-sell qty-quantified)))
    ;  (<= position 0) (do (swap! orders conj (h/->new-order vsa "buy" avg-executed-buy qty-quantified))))
    ))


(defn entry [vsa]
  (let [venue (:venue vsa)
        stock (:stock vsa)
        orderbooks (get @state/order-book (h/->unique-key venue stock))]
    ;(println @cancelled-order-count)
    ;(clean-open-orders venue stock state/open-orders (get-avg-spread 50))
    ;(Thread/sleep 300)
    ;(println "end clean")
    ;(println "sell/buy")
    ;(correct-orders vsa)
    (sell-and-buy vsa state/open-orders)
    ;(println "-------------------")
    ;(println "eand sell/buy")
    ;(println "correct")

    ;(println "end correct")
    ))