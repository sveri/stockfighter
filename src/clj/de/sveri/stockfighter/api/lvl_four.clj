(ns de.sveri.stockfighter.api.lvl-four
  (:require [de.sveri.stockfighter.api.state :as state]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]
            [immutant.scheduling :refer :all]
            [com.rpl.specter :as spec]))

(s/defn ->new-order [{:keys [venue stock account] :as vsa} :- schem/vsa buy-or-sell :- schem/direction price :- s/Num qty :- s/Num]
  {:account account :venue venue :stock stock :price price :qty qty :direction buy-or-sell :orderType "limit"})


(s/defn ->avg-price [orderbooks ask-or-bid]
  (let [asks (spec/select [spec/ALL ask-or-bid spec/FIRST :price]
                          (subvec (into [] orderbooks) 0 (if (< 10 (count orderbooks)) 10 (count orderbooks))))]
    (if (not-empty asks)
      (int (/ (reduce + asks) (count asks)))
      0)))

(defn get-depth [ask-or-bid orderbooks]
  (if-let [things (ask-or-bid (first orderbooks))]
    (:qty (first things))
    0))

(s/defn be-a-market-maker-now? [vsa :- schem/vsa
                                open-orders :- (s/atom schem/orders) orderbooks :- schem/orderbooks
                                booking :- (s/atom schem/booking)]
  (let [avg-price (->avg-price orderbooks :asks)
        buy-order (->new-order vsa "buy" (- avg-price 200) 200)
        sell-order (->new-order vsa "sell" (+ avg-price 100) 200)]

    (when (and (< (get-depth :asks orderbooks) 10000) (< (get-depth :bids orderbooks) 10000)
               (< (count @open-orders) 3))
      (when (< (:position @booking) 300)
        (let [o-resp @(api/new-order buy-order)]
          (if (< 0 (:qty o-resp)) (swap! open-orders conj o-resp))))
      (when (< -300 (:position @booking))
        (println "selling")
        (let [o-resp @(api/new-order sell-order)]
          (when (< 0 (:qty o-resp)) (swap! open-orders conj o-resp)))))))
