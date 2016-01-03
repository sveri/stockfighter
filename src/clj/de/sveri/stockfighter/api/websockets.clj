(ns de.sveri.stockfighter.api.websockets
  (:require [gniazdo.core :as ws]
            [clojure.data.json :as json]
            [schema.core :as s]
            [de.sveri.stockfighter.api.config :as conf]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.service.helper :as h]))

(def quotes-socket (atom {}))
(def executions-socket (atom {}))

(def quote-history (atom {}))
(def execution-history (atom {}))


; nav = cash + (shares * share_price)
(def booking (atom {:nav 0 :position 0 :cash 0 :avg-bid 0 :avg-ask 0 :ask-count 0 :bid-count 0 :buy-sell-lock false}))

(def order-book (atom {}))

(def open-orders (atom []))
(add-watch open-orders :orders-validation (fn [_ _ _ new] (s/validate schem/orders new)))




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
  (let [fills (:fills order)
        buy-or-sell (:direction order)]
    (doseq [fill fills]
      (let [add-min-position? (if (= (:direction order) "buy") + -)
            add-min-cash? (if (= (:direction order) "buy") - +)]
        (swap! book-atom (fn [b-old] (let [new-position (add-min-position? (:position b-old) (:qty fill))
                                           new-cash (add-min-cash? (:cash b-old) (* (:price fill) (:qty fill)))
                                           avg-key (if (= "buy" buy-or-sell) :avg-ask :avg-bid)
                                           avg-count-key (if (= "buy" buy-or-sell) :ask-count :bid-count)
                                           old-avg (avg-key b-old)
                                           old-count (avg-count-key b-old)
                                           new-avg (int (/ (+ old-avg (:price fill)) (if (= old-count 0) 1 2)))]
                                       (assoc b-old :position new-position
                                                    :cash new-cash
                                                    avg-count-key (inc old-count)
                                                    avg-key new-avg))))))))

(s/defn clean-open-order :- s/Any [execution :- schem/execution open-orders :- (s/atom schem/orders)]
  (when (= 0 (get-in execution [:order :qty]))
    (swap! open-orders (fn [a] (into [] (remove #(= (:id %) (get-in execution [:order :id])) a))))))


(s/defn parse-execution :- s/Any
  [venue stock account execution-response :- s/Str]
  (let [execution (json/read-str execution-response :key-fn keyword :value-fn h/api->date)]
    (if (:ok execution)
      (do (swap! execution-history update (h/->unique-key venue stock account) conj execution)
          (clean-open-order execution open-orders)
          (update-booking execution booking)
          )
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




;
;
;
;
;
;
;(def test-book
;  '({:ok     true,
;     :venue  "MOEX",
;     :symbol "UUKG",
;     :ts     #inst "2015-12-30T15:00:34.598-00:00",
;     :bids
;             [{:price 9098, :qty 203, :isBuy true}
;              {:price 8943, :qty 459, :isBuy true}
;              {:price 8899, :qty 459, :isBuy true}
;              {:price 8855, :qty 459, :isBuy true}
;              {:price 8736, :qty 148, :isBuy true}],
;     :asks
;             [{:price 9143, :qty 40, :isBuy false}
;              {:price 9188, :qty 40, :isBuy false}
;              {:price 9233, :qty 40, :isBuy false}]}
;     {:ok     true,
;      :venue  "MOEX",
;      :symbol "UUKG",
;      :ts     #inst "2015-12-30T15:00:24.616-00:00",
;      :bids
;              [{:price 8793, :qty 203, :isBuy true}
;               {:price 8663, :qty 438, :isBuy true}
;               {:price 8620, :qty 438, :isBuy true}
;               {:price 8577, :qty 438, :isBuy true}],
;      :asks
;              [{:price 9175, :qty 47, :isBuy false}
;               {:price 9220, :qty 47, :isBuy false}
;               {:price 9265, :qty 15, :isBuy false}]}
;     {:ok     true,
;      :venue  "MOEX",
;      :symbol "UUKG",
;      :ts     #inst "2015-12-30T15:00:24.616-00:00",
;      :bids
;              [{:price 8793, :qty 203, :isBuy true}
;               {:price 8663, :qty 438, :isBuy true}
;               {:price 8620, :qty 438, :isBuy true}
;               {:price 8577, :qty 438, :isBuy true}],
;      :asks
;              [{:price 9175, :qty 47, :isBuy false}
;               {:price 9220, :qty 47, :isBuy false}
;               {:price 9265, :qty 15, :isBuy false}]}))
;
;((fn avg-ask [book]
;   ;(map #(get-in % [:asks :price]) book)
;   (let [prices (spec/select [spec/ALL :asks spec/FIRST :price] book)]
;     (int (/ (reduce + prices) (count prices))))
;   )
;  test-book)