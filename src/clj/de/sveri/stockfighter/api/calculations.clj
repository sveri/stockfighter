(ns de.sveri.stockfighter.api.calculations
  (:require [de.sveri.stockfighter.service.helper :as h]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]))



(defn get-avg-of [quotes key]
  (let [clean-quotes (filter key quotes)]
    (when (not-empty clean-quotes)
      (double (/ (reduce (fn [sum bid] (if bid (+ sum bid) sum)) (map key clean-quotes)) (count clean-quotes))))))

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
    (if-let [completed (filter #(= true (:standingComplete %)) executions)]
      (let [filled (reduce + (map :filled completed))
            price (reduce + (map :price completed))]
        {:total-filled filled :filled-avg (if (= 0 price) 0 (/ price (count completed)))})
      {:total-filled 0 :filled-avg 0})))
