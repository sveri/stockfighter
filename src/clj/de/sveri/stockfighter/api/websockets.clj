(ns de.sveri.stockfighter.api.websockets
  (:require [gniazdo.core :as ws]
            [clojure.data.json :as json]
            [schema.core :as s]
            [de.sveri.stockfighter.api.config :as conf]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.api.state :as state]))






(s/defn parse-quote :- s/Any
  [{:keys [venue stock] :as vsa} :- schem/vsa quote-response :- s/Str]
  (let [quote (json/read-str quote-response :key-fn keyword :value-fn h/api->date)]
    (if (:ok quote)
      (try
        ;(bots/start-bot vsa (:quote quote) (get @order-book (h/->unique-key venue stock)) booking)
        (swap! state/quote-history update (h/->unique-key vsa) conj (:quote quote))
        (catch Exception e (do (println (:quote quote)) (.printStackTrace e))))
      (println "something else happened: " quote-response))))

(s/defn connect-quotes :- s/Any [{:keys [venue stock account] :as vsa} :- schem/vsa]
  (println "starting quote-ticker for " vsa)
  (swap! state/quotes-socket assoc (h/->unique-key venue stock account)
         (ws/connect
           (str conf/ws-uri account "/venues/" venue "/tickertape/stocks/" stock)
           :on-receive #(parse-quote vsa %)
           :on-close (fn [a b] (println a " - " b " - " (format "Closed quote websocket for %s?" (str venue stock account)))
                       (when (and (h/restart-api-websockets?) (= 1006 a)) (connect-quotes vsa)))
           :on-error #(do (println (format "Some error occured for: %s - %s - %s:" venue stock account))
                          (.printStackTrace %)))))

(s/defn clean-open-order :- s/Any [execution :- schem/execution open-orders :- (s/atom schem/orders)]
  (when (= 0 (get-in execution [:order :qty]))
    (swap! open-orders (fn [a] (into [] (remove #(= (:id %) (get-in execution [:order :id])) a))))))


(s/defn parse-execution :- s/Any
  [venue stock account execution-response :- s/Str execution-history booking]
  (let [execution (json/read-str execution-response :key-fn keyword :value-fn h/api->date) ]
    (if (:ok execution)
      (do
        ;(println "executed: " execution)
        ;(clojure.pprint/pprint (subvec (into []  (state/->orderbook (h/->vsa)))  0 2))
        (swap! execution-history update (h/->unique-key venue stock account) conj execution)
          (clean-open-order execution state/open-orders)
          (state/update-booking (:order execution) booking))
      (println "something else happened: " execution-response))))

(s/defn connect-executions :- s/Any [{:keys [venue stock account] :as vsa} :- schem/vsa execution-socket execution-history booking]
  (println "starting execution-ticker for " vsa)
  (swap! execution-socket assoc (h/->unique-key venue stock account)
         (ws/connect
           (str conf/ws-uri account "/venues/" venue "/executions/stocks/" stock)
           :on-receive #(parse-execution venue stock account % execution-history booking)
           :on-close (fn [a b] (println a " - " b " - " (format "Closed execution websocket for %s?" (str venue stock account)))
                       (when (and (h/restart-api-websockets?) (= 1006 a)) (connect-executions vsa execution-socket execution-history booking)))
           :on-error #(println (format "Some error occured for: %s - %s - %s: \n %s" venue stock account (.printStackTrace %))))))

(s/defn close-sockets-by-key :- s/Any [{:keys [venue stock account]} :- schem/vsa]
  (when-let [socket (get @state/quotes-socket (h/->unique-key venue stock account))] (ws/close socket))
  (when-let [socket (get @state/executions-socket (h/->unique-key venue stock account))] (ws/close socket)))

(defn close-socket [s] (ws/close s))

(defn close-all-sockets [m]
  (for [[_ socket] m] (close-socket socket)))

(defn close-execution-and-quote-socket []
  (close-all-sockets @state/quotes-socket)
  (close-all-sockets @state/executions-socket))



