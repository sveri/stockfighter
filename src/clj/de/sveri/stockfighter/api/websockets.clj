(ns de.sveri.stockfighter.api.websockets
  (:require [gniazdo.core :as ws]
            [clojure.data.json :as json]
            [schema.core :as s]
            [clj-time.format :as f]
            [de.sveri.stockfighter.api.config :as conf]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.service.helper :as h]))

(def quotes-socket (atom {}))
(def executions-socket (atom {}))

(def quote-history (atom {}))
(def execution-history (atom {}))

(defn api->date [key value]
  (if (contains? #{:quoteTime :lastTrade :ts :filledAt} key)
    (f/parse schem/api-time-format value)
    value))

(s/defn parse-quote :- s/Any
  [venue stock account quote-response :- s/Str]
  (let [quote (json/read-str quote-response :key-fn keyword :value-fn api->date)]
    (if (:ok quote)
      (swap! quote-history update (h/->unique-key venue stock account) conj (:quote quote))
      (println "something else happened: " quote-response))))

(s/defn connect-quotes :- s/Any [{:keys [venue stock account] :as vsa} :- schem/vsa]
  (println "starting quote-ticker for " vsa)
  (swap! quotes-socket assoc (h/->unique-key venue stock account)
         (ws/connect
           (str conf/ws-uri account "/venues/" venue "/tickertape/stocks/" stock)
           :on-receive #(parse-quote venue stock account %)
           :on-close (fn [a b] (println a " - " b " - " (format "Closed quote websocket for %s?" (str venue stock account))))
           :on-error #(println (format "Some error occured for: %s - %s - %s: \n %s" venue stock account (.printStackTrace %))))))


(s/defn parse-execution :- s/Any
  [venue stock account execution-response :- s/Str]
  (let [execution (json/read-str execution-response :key-fn keyword :value-fn api->date)]
    (if (:ok execution)
      (swap! execution-history update (h/->unique-key venue stock account) conj execution)
      (println "something else happened: " execution-response))))

(s/defn connect-executions :- s/Any [{:keys [venue stock account] :as vsa} :- schem/vsa]
  (println "starting execution-ticker for " vsa)
  (swap! executions-socket assoc (h/->unique-key venue stock account)
         (ws/connect
           (str conf/ws-uri account "/venues/" venue "/executions/stocks/" stock)
           :on-receive #(parse-execution venue stock account %)
           :on-close (fn [a b] (println a " - " b " - " (format "Closed execution websocket for %s?" (str venue stock account)))
                       (connect-executions vsa))
           :on-error #(println (format "Some error occured for: %s - %s - %s: \n %s" venue stock account (.printStackTrace %))))))

(s/defn close-sockets-by-key :- s/Any [{:keys [venue stock account]} :- schem/vsa]
  (when-let [socket (get @quotes-socket (h/->unique-key venue stock account))] (ws/close socket))
  ;(when-let [socket (get @executions-socket (h/->unique-key venue stock account))] (ws/close socket))
  )

(defn close-socket [s] (ws/close s))

(defn close-all-sockets [m]
  (for [[_ socket] m] (close-socket socket)))

(defn close-execution-and-quote-socket []
  (close-all-sockets @quotes-socket)
  (close-all-sockets @executions-socket))

;(close-all-sockets @quotes-socket)

(defn get-avg-of [quotes key]
  (let [clean-quotes (filter key quotes)]
    (double (/ (reduce (fn [sum bid] (if bid (+ sum bid) sum)) (map key clean-quotes)) (count clean-quotes)))))

(defn get-avg-bid
  ([quotes] (get-avg-of quotes :bid))
  ([venue stock account & [last-x]]
   (let [quotes (->> (h/->unique-key venue stock account)
                     (get @quote-history))
         quotes-short (if (and last-x (< last-x (count quotes)))
                        (subvec (into [] quotes) 0 last-x)
                        quotes)]
     (get-avg-bid quotes-short))))

(defn get-las-ten-bids
  ([venue stock account]
   (->> (h/->unique-key venue stock account)
        (get @quote-history)
        (get-las-ten-bids)))
  ([quotes]
   (subvec quotes (- (count quotes) 10))))

(s/defn ->accumulated-executions :- schem/execution-stream
  ([venue :- s/Str stock :- s/Str account :- s/Str]
   (->> (h/->unique-key venue stock account)
           (get @execution-history)
           (->accumulated-executions)))
  ([executions :- s/Any]
   (let [completed (filter #(= true (:standingComplete %)) executions)
         filled (reduce + (map :filled completed))
         price (reduce + (map :price completed))]
     {:total-filled filled :filled-avg (if (= 0 price) 0 (/ price (count completed)))})))


; maybe need this when we need exact timestamp order and not the order received via ws
;(swap! quote-history assoc (h/->unique-key venue stock account) (sorted-set-by compare-dates))

;
;(defn compare-dates [a b]
;  (let [a-long (t-c/to-long (:quoteTime a)) b-long (t-c/to-long (:quoteTime b))]
;    (< a-long b-long)))
