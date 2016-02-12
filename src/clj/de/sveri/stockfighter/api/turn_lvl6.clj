(ns de.sveri.stockfighter.api.turn-lvl6
  (:require [de.sveri.stockfighter.api.turnbased :as turn]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.api.state :as state]
            [schema.core :as s]
            [com.rpl.specter :as spec]
            [incanter.stats :as stats]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]
            [clojure.string :as str]
            [taoensso.encore :refer [when-lets]]
            [immutant.scheduling :refer :all]))


;(defn tt [a b c]
;  (when-lets [aa a
;              bb b]
;             (+ aa bb)))

(def accounts (atom {}))

(def buy-or-sell (atom "buy"))

(def bids-and-asks (atom {}))

(defn sort-by-val [m]
  (into (sorted-map-by (fn [key1 key2]
                         (compare [(get m key2) key2]
                                  [(get m key1) key1])))
        m))


(defn print-data-from-booking [k]
  (sort-by-val (reduce (fn [nav-map [account booking-a]]
                         (if (< 0 (k @booking-a))
                           (assoc nav-map account
                                         [(double (/ (k @booking-a) 100))
                                          (:ask-count @booking-a)
                                          (:bid-count @booking-a)])
                           nav-map)
                       ) {} @accounts)))

(defn switch-buy-or-sell []
  (let [buy-or-sell' @buy-or-sell
        new-buy-or-sell (if (= buy-or-sell' "buy") "sell" "buy")]
    (reset! buy-or-sell new-buy-or-sell)))

(s/defn ->new-order [{:keys [venue stock account] :as vsa} :- schem/vsa buy-or-sell :- schem/direction price :- s/Num qty :- s/Num]
  {:account account :venue venue :stock stock :price price :qty qty :direction buy-or-sell :orderType "limit"})


(s/defn ->avg-price [orderbooks ask-or-bid]
  (let [asks (spec/select [spec/ALL ask-or-bid spec/FIRST :price]
                          (subvec (into [] orderbooks) 0 (if (< 10 (count orderbooks)) 10 (count orderbooks))))]
    (if (not-empty asks)
      (int (/ (reduce + asks) (count asks)))
      0)))


(defn start-ws-for-account [vsa account booking-atom]
  (de.sveri.stockfighter.api.websockets/connect-executions (assoc vsa :account account) state/executions-socket state/execution-history booking-atom))

(defn get-resp-from-fake-delete [venue stock order-id]
  (:error (api/delete-order-fake venue stock order-id)))

(defn collect-accounts* [{:keys [venue stock] :as vsa}]
  (doseq [i (range 1 620)]
    (try
      (when-lets [resp (get-resp-from-fake-delete venue stock i)
                  acc-with-point (second (str/split resp #"ccount "))
                  account (subs acc-with-point 0 (- (count acc-with-point) 1))]
                 (when (nil? (get @accounts account))
                   (let [booking-atom (atom state/booking-default)]
                     (swap! accounts assoc account booking-atom)
                     (start-ws-for-account vsa account booking-atom)))
                 )
      (catch Exception e (println e)))
    (Thread/sleep 1000))
  (println "done collection accounts"))

;(s/defn trigger-buy-or-sell* :- s/Any
;  [vsa orderbooks]
;  (println "emitting fake: " @buy-or-sell)
;  (let [avg-price (->avg-price orderbooks :asks)
;        order-price (if (= "buy" @buy-or-sell) (+ avg-price 2000) (- avg-price 2000))
;        order (->new-order vsa @buy-or-sell order-price 20)
;        order-resp @(api/new-order order)
;        order-id (:id order-resp)]
;    (Thread/sleep 5000)
;    (collect-accounts order-id vsa)
;    ;(switch-buy-or-sell)
;    ))






