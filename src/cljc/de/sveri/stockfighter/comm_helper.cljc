(ns de.sveri.stockfighter.comm-helper
  (:require [schema.core :as s]))

(defn zip
  "Zips collections."
  [& colls]

  (partition (count colls) (apply interleave colls)))


(s/defn extract-client-target-price :- s/Num [info :- s/Str]
  (when info (nth (re-find #"(\d{2}\.\d{2}).*(\d{2}\.\d{2})" info) 2)))


(defn get-bids-or-asks [book bid-or-ask]
  (rseq (reduce (fn [a b] (conj a (:price (first b)))) [] (map bid-or-ask book))))
