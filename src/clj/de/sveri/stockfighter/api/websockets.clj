(ns de.sveri.stockfighter.api.websockets
  (:require [gniazdo.core :as ws]
            [clojure.data.json :as json]
            [schema.core :as s]
            [clj-time.format :as f]
            [de.sveri.stockfighter.api.config :as conf]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.api.api :as api]
            [de.sveri.stockfighter.service.helper :as h]
            [com.rpl.specter :as spec]))

(def quotes-socket (atom {}))
(def executions-socket (atom {}))

(def quote-history (atom {}))
(def execution-history (atom {}))


; nav = cash + (shares * share_price)
(def booking (atom {:nav 0 :position 0 :cash 0}))

(def order-book (atom {}))

;(add-watch order-book :print-watch (fn [_ _ _ new] (clojure.pprint/pprint new) new))

(def test-book
  '({:ok     true,
     :venue  "MOEX",
     :symbol "UUKG",
     :ts     #inst "2015-12-30T15:00:34.598-00:00",
     :bids
             [{:price 9098, :qty 203, :isBuy true}
              {:price 8943, :qty 459, :isBuy true}
              {:price 8899, :qty 459, :isBuy true}
              {:price 8855, :qty 459, :isBuy true}
              {:price 8736, :qty 148, :isBuy true}],
     :asks
             [{:price 9143, :qty 40, :isBuy false}
              {:price 9188, :qty 40, :isBuy false}
              {:price 9233, :qty 40, :isBuy false}]}
     {:ok     true,
      :venue  "MOEX",
      :symbol "UUKG",
      :ts     #inst "2015-12-30T15:00:24.616-00:00",
      :bids
              [{:price 8793, :qty 203, :isBuy true}
               {:price 8663, :qty 438, :isBuy true}
               {:price 8620, :qty 438, :isBuy true}
               {:price 8577, :qty 438, :isBuy true}],
      :asks
              [{:price 9175, :qty 47, :isBuy false}
               {:price 9220, :qty 47, :isBuy false}
               {:price 9265, :qty 15, :isBuy false}]}
     {:ok     true,
      :venue  "MOEX",
      :symbol "UUKG",
      :ts     #inst "2015-12-30T15:00:24.616-00:00",
      :bids
              [{:price 8793, :qty 203, :isBuy true}
               {:price 8663, :qty 438, :isBuy true}
               {:price 8620, :qty 438, :isBuy true}
               {:price 8577, :qty 438, :isBuy true}],
      :asks
              [{:price 9175, :qty 47, :isBuy false}
               {:price 9220, :qty 47, :isBuy false}
               {:price 9265, :qty 15, :isBuy false}]}))

((fn avg-ask [book]
   ;(map #(get-in % [:asks :price]) book)
   (let [prices (spec/select [spec/ALL :asks spec/FIRST :price] book)]
     (int (/ (reduce + prices) (count prices))))
   )
  test-book)

(s/defn parse-quote :- s/Any
  [{:keys [venue stock] :as vsa} :- schem/vsa quote-response :- s/Str]
  (let [quote (json/read-str quote-response :key-fn keyword :value-fn h/api->date)]
    (if (:ok quote)
      (try
        ;(bots/start-bot vsa (:quote quote) (get @order-book (h/->unique-key venue stock)) booking)
        (swap! quote-history update (h/->unique-key vsa) conj (:quote quote))
        (catch Exception e (do (println (:quote quote)) (.printStackTrace e))))
      (println "something else happened: " quote-response))))

(s/defn connect-quotes :- s/Any [{:keys [venue stock account] :as vsa} :- schem/vsa]
  (println "starting quote-ticker for " vsa)
  (swap! quotes-socket assoc (h/->unique-key venue stock account)
         (ws/connect
           (str conf/ws-uri account "/venues/" venue "/tickertape/stocks/" stock)
           :on-receive #(parse-quote vsa %)
           :on-close (fn [a b] (println a " - " b " - " (format "Closed quote websocket for %s?" (str venue stock account)))
                       (when (and (h/restart-api-websockets?) (= 1006 a)) (connect-quotes vsa)))
           :on-error #(do (println (format "Some error occured for: %s - %s - %s:" venue stock account))
                          (.printStackTrace %)))))

(s/defn update-booking :- s/Any [{:keys [order]} :- schem/execution book-atom :- s/Any]
  (let [fills (:fills order)]
    (doseq [fill fills]
      (let [add-min-position? (if (= (:direction order) "buy") + -)
            add-min-cash? (if (= (:direction order) "buy") - +)]
        (swap! book-atom (fn [b-old] (let [new-position (add-min-position? (:position b-old) (:qty fill))
                                           new-cash (add-min-cash? (:cash b-old) (* (:price fill) (:qty fill)))]
                                       (assoc b-old :position new-position
                                                    :cash new-cash))))))))


(s/defn parse-execution :- s/Any
  [venue stock account execution-response :- s/Str]
  (let [execution (json/read-str execution-response :key-fn keyword :value-fn h/api->date)]
    (if (:ok execution)
      (do (swap! execution-history update (h/->unique-key venue stock account) conj execution)
          (update-booking execution booking))
      (println "something else happened: " execution-response))))

(s/defn connect-executions :- s/Any [{:keys [venue stock account] :as vsa} :- schem/vsa]
  (println "starting execution-ticker for " vsa)
  (swap! executions-socket assoc (h/->unique-key venue stock account)
         (ws/connect
           (str conf/ws-uri account "/venues/" venue "/executions/stocks/" stock)
           :on-receive #(parse-execution venue stock account %)
           :on-close (fn [a b] (println a " - " b " - " (format "Closed execution websocket for %s?" (str venue stock account)))
                       (when (and (h/restart-api-websockets?) (= 1006 a)) (connect-executions vsa)))
           :on-error #(println (format "Some error occured for: %s - %s - %s: \n %s" venue stock account (.printStackTrace %))))))

(s/defn close-sockets-by-key :- s/Any [{:keys [venue stock account]} :- schem/vsa]
  (when-let [socket (get @quotes-socket (h/->unique-key venue stock account))] (ws/close socket))
  (when-let [socket (get @executions-socket (h/->unique-key venue stock account))] (ws/close socket)))

(defn close-socket [s] (ws/close s))

(defn close-all-sockets [m]
  (for [[_ socket] m] (close-socket socket)))

(defn close-execution-and-quote-socket []
  (close-all-sockets @quotes-socket)
  (close-all-sockets @executions-socket))


; maybe need this when we need exact timestamp order and not the order received via ws
;(swap! quote-history assoc (h/->unique-key venue stock account) (sorted-set-by compare-dates))

;
;(defn compare-dates [a b]
;  (let [a-long (t-c/to-long (:quoteTime a)) b-long (t-c/to-long (:quoteTime b))]
;    (< a-long b-long)))
