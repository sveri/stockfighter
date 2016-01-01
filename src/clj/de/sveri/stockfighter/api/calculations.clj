(ns de.sveri.stockfighter.api.calculations
  (:require [de.sveri.stockfighter.service.helper :as h]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]))



(s/defn get-avg-of :- (s/maybe s/Num) [quotes key]
  (let [clean-quotes (filter key quotes)]
    (when (not-empty clean-quotes)
      (double (/ (reduce (fn [sum bid] (if bid (+ sum bid) sum)) (map key clean-quotes)) (count clean-quotes))))))

; might return nil, have to find out when?
(s/defn get-avg-bid :- (s/maybe s/Num)
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
    (let [completed-bids (filter #(and (< 0 (:filled %))(= "buy" (get-in % [:order :direction]))) executions)
          completed-asks (filter #(and (< 0 (:filled %)) (= "sell" (get-in % [:order :direction]))) executions)
          bids (reduce + (map :price completed-bids))
          asks (reduce + (map :price completed-asks))
          bids-avg (if (= 0 bids) 0 (/ bids (count completed-bids)))
          asks-avg (if (= 0 asks) 0 (/ asks (count completed-asks)))
          spread (- asks-avg bids-avg)]
      ;(println "asks: " (count completed-asks) " - bids: " (count completed-bids))
      {:spread spread :bids-avg bids-avg :asks-avg asks-avg})))
