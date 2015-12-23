(ns de.sveri.stockfighter.api.lvl-three
  (:require [de.sveri.stockfighter.api.calculations :as calc]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]))


(def sell-me (atom []))

(s/defn bought-something? :- s/Bool [order :- schem/order] (< 0 (:totalFilled order)))

(s/defn generate-sell-order :- [schem/new-order] [venue :- s/Str stock :- s/Str account :- s/Str order-response :- schem/order]
  (mapv (fn [fill]
         {:account   account :venue venue :stock stock :price (int (+ (:price fill) (* 0.05 (:price fill))))
          :qty       (:qty fill) :direction "sell"
          :orderType "fill-or-kill"})
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
    (when (= (:qty sell-order) (:totalFilled sell-response))
      (println "sold: " sell-response)
      (swap! sell-me (fn [v] (subvec v 1))))))

(s/defn start-lvl-three :- s/Any
  [{:keys [venue stock account] :as vsa} :- schem/vsa quote-history :- s/Any]
  (if (< (count @sell-me) 3)
    (buy-a-thing venue stock account quote-history)
    (sell-a-thing)
    ))