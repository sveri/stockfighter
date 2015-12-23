(ns de.sveri.stockfighter.api.lvl-three
  (:require [de.sveri.stockfighter.api.calculations :as calc]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]
            [immutant.scheduling :refer :all]))


(def sell-me (atom []))

(s/defn bought-something? :- s/Bool [order :- schem/order] (< 0 (:totalFilled order)))

(def fooo (atom [1 2 3 4]))

;(defn sell-lower-thingy [a]
;  (swap! a
;         (fn [v] (mapv #(if) v))))

(s/defn generate-sell-order :- [schem/new-order] [venue :- s/Str stock :- s/Str account :- s/Str order-response :- schem/order]
  (mapv (fn [fill]
         {:account   account :venue venue :stock stock :price (int (+ (:price fill) (* 0.01 (:price fill))))
          :qty       (:qty fill) :direction "sell"
          :orderType "immediate-or-cancel"})
       (:fills order-response)))

(defn buy-a-thing [venue stock account quote-history]
  (let [buy-price (int (calc/get-avg-bid venue stock account quote-history 5))
        buy-order {:account   account :venue venue :stock stock :price buy-price :qty 100 :direction "buy"
                   :orderType "immediate-or-cancel"}]
    (println "trying to buy: " buy-order)
    (let [order-response (api/new-order buy-order)]
      (when (bought-something? order-response)
        (reset! sell-me (generate-sell-order venue stock account order-response))
        (println "bought " buy-order)))))

(defn sell-a-thing []
  (let [sell-order (first @sell-me)
        sell-response (api/new-order sell-order)]
    (println "trying to sell: " sell-order)
    (println sell-response)
    ;(if (= (:qty sell-order) (:totalFilled sell-response))
    ;  (do (println "sold: " sell-response)
    ;      (swap! sell-me (fn [v] (subvec v 1))))
    ;  (swap! sell-me (fn [v] (let [cur-price (:price sell-order)]
    ;                           (assoc v 0 (assoc sell-order :price (- cur-price (* cur-price 0.03))))))))
    ))

(s/defn start-lvl-three :- s/Any
  [{:keys [venue stock account] :as vsa} :- schem/vsa quote-history :- s/Any]
  (if (empty? @sell-me)
  ;(if (< (count @sell-me) 3)
    (buy-a-thing venue stock account quote-history)
    (sell-a-thing)))