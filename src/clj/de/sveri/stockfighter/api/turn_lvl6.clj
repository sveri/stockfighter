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

(def default-account {s/Str {:booking-atom state/booking-default
                             :5-day-nav    s/Num
                             :10-day-nav   s/Num}})

(def accounts (atom {}))

(def buy-or-sell (atom "buy"))

(defn sort-by-val [m]
  (into (sorted-map-by (fn [key1 key2]
                         (compare [(get m key2) key2]
                                  [(get m key1) key1])))
        m))


(defn print-data-from-booking []
  (sort-by-val (reduce (fn [nav-map [account {:keys [booking-atom] :as data}]]
                         (let [nav (:nav @booking-atom)
                               ten-day-nav (:10-day-nav data)
                               five-day-nav (:5-day-nav data)]
                           (if (and (< 0 nav) (< ten-day-nav 0))
                             (assoc nav-map account
                                            [(double (/ nav 100))
                                             (double (/ ten-day-nav 100))
                                             ;(double (/ five-day-nav 100))
                                             (- nav ten-day-nav)
                                             (:ask-count @booking-atom)
                                             (:bid-count @booking-atom)
                                             ;(- nav five-day-nav)
                                             ])
                             nav-map))
                         ) {} @accounts)))

;(defn switch-buy-or-sell []
;  (let [buy-or-sell' @add-to-nav
;        new-buy-or-sell (if (= buy-or-sell' "buy") "sell" "buy")]
;    (reset! add-to-nav new-buy-or-sell)))

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
                     (swap! accounts assoc account {:booking-atom booking-atom
                                                    :5-day-nav    0
                                                    :10-day-nav   0})
                     (start-ws-for-account vsa account booking-atom)))
                 )
      (catch Exception e (println e)))
    (Thread/sleep 1000))
  (println "done collection accounts"))


(defmulti add-to-nav (fn [_ execution _] (get-in execution [:order :direction])))
(defmethod add-to-nav "sell" [nav {:keys [filled]} {:keys [ask]}]
  (+ nav (* filled ask)))
(defmethod add-to-nav "buy" [nav {:keys [filled]} {:keys [bid]}]
  (- nav (* filled bid)))
(defmethod add-to-nav :default [_ _ _] (println "execution null"))

(defn ->timed-nav [nav day-offset execution bids-and-asks]
  (let [
        execution-day (h/get-day-of-transaction (:ts-of-day-one @h/common-state) (:filledAt execution))
        bid-and-ask (get bids-and-asks (+ execution-day day-offset))
        ]
    (add-to-nav nav execution bid-and-ask)
    ))




(s/defn calculate-timed-navs [five-or-ten]
  (let [nav-key (if (= five-or-ten 5) :5-day-nav :10-day-nav)]
    (doseq [[account data] @accounts]
      (doseq [execution (state/->executions (assoc (h/->vsa) :account account))]
        (swap! accounts assoc-in [account nav-key] (->timed-nav (nav-key data) five-or-ten execution @state/bids-and-asks))))))

