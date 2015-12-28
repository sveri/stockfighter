(ns de.sveri.stockfighter.api.calculations
  (:require [de.sveri.stockfighter.service.helper :as h]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]))



(defn get-avg-of [quotes key]
  (let [clean-quotes (filter key quotes)]
    (when (not-empty clean-quotes)
      (double (/ (reduce (fn [sum bid] (if bid (+ sum bid) sum)) (map key clean-quotes)) (count clean-quotes))))))

; might return nil, have to find out when?
(defn get-avg-bid
  ([quotes] (get-avg-of quotes :bid))
  ([venue stock account quote-history & [last-x]]
   (let [quotes (->> (h/->unique-key venue stock account)
                     (get @quote-history))
         quotes-short (if (and last-x (< last-x (count quotes)))
                        (subvec (into [] quotes) 0 last-x)
                        quotes)]
     (get-avg-bid quotes-short))))

(s/defn ->accumulated-executions :- schem/execution-stream
  ([venue :- s/Str stock :- s/Str account :- s/Str execution-history :- s/Any]
    (->> (h/->unique-key venue stock account)
         (get @execution-history)
         (->accumulated-executions)))
  ([executions :- s/Any]
    (let [completed-bids (filter #(= true (:standingComplete %)) executions)
          completed-asks (filter #(= true (:incomingComplete %)) executions)
          ;filled (reduce + (map :filled completed))
          ;price (reduce + (map :price completed))
          bids (reduce + (map :price completed-bids))
          asks (reduce + (map :price completed-asks))
          bids-avg (if (= 0 bids) 0 (/ bids (count completed-bids)))
          asks-avg (if (= 0 asks) 0 (/ asks (count completed-asks)))
          spread (- asks-avg bids-avg)]
      ;(println completed)
      {
       ;:total-filled 0
       :spread spread
       :bids-avg bids-avg
       :asks-avg asks-avg})
    #_(if-let [completed (filter #(= true (:standingComplete %)) executions)]
      (let [completed-bids (filter #(= "buy" (get-in % [:order :direction])) completed)
            completed-asks (filter #(= "sell" (get-in % [:order :direction])) completed)
            filled (reduce + (map :filled completed))
            price (reduce + (map :price completed))
            bids (reduce + (map :price completed-bids))
            asks (reduce + (map :price completed-asks))]
        ;(println completed)
        {:total-filled filled
         :filled-avg (if (= 0 price) 0 (/ price (count completed)))
         :bids-avg (if (= 0 bids) 0 (/ bids (count completed-bids)))
         :asks-avg (if (= 0 asks) 0 (/ asks (count completed-asks)))})
      {:total-filled 0 :filled-avg 0 :bids-avg 0 :asks-avg 0})))
