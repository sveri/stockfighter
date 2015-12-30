(ns de.sveri.stockfighter.schema-api
  (:require [schema.core :as s]
    #?(:clj [clj-time.format :as f])))

#?(:clj (def api-time-format (f/formatters :date-time)))


;;;;;; gm ;;;;;;
(def levels (s/cond-pre (s/eq "chock_a_block") (s/eq "sell_side")))
(def levels-response {:account s/Str :instanceId s/Num :instructions s/Any :ok s/Bool :secondsPerTradingDay s/Num
                      :tickers [s/Str] :venues [s/Str] (s/optional-key :balances) s/Any})
(def game-state {:ok s/Bool :done s/Bool :id s/Num :state s/Str :details {:endOfTheWorldDay s/Num :tradingDay s/Num}})

(def game-info {:ok                       s/Bool :done s/Bool :id s/Num :state s/Str
                (s/optional-key :flash)   {(s/optional-key :info)    s/Str
                                           (s/optional-key :warning) s/Str}
                (s/optional-key :details) {:endOfTheWorldDay s/Num :tradingDay s/Num}})


;;;; stock

(def stock {:name s/Str :symbol s/Str})
(def stocks {:symbols [stock]})

(def bid-ask {:price s/Num :qty s/Num :isBuy s/Bool})
(def order-book {:ok s/Bool :venue s/Str :symbol s/Str :bids [bid-ask] :asks [bid-ask] :ts s/Inst})


(def direction (s/cond-pre (s/eq "buy") (s/eq "sell")))
(def order-type (s/cond-pre (s/eq "limit") (s/eq "market") (s/eq "fill-or-kill") (s/eq "immediate-or-cancel")))

(def vsa "venue - stock - account" {:venue s/Str :stock s/Str :account s/Str})

(def fill {:price s/Num :qty s/Num :ts s/Inst})
(def order {:ok        s/Bool :price s/Num :symbol s/Str :venue s/Str :direction direction :originalQty s/Num :qty s/Num
            :orderType order-type :id s/Num :account s/Str :ts s/Inst :fills [fill] :totalFilled s/Num :open s/Bool})
(def orders [order])

(def new-order {:account   s/Str :venue s/Str :stock s/Str :price s/Num :qty s/Num :direction direction
                :orderType order-type})

(def new-batch-order (merge new-order {:target-qty s/Num :level levels}))

(def quote {:symbol               s/Str :venue s/Str
            (s/optional-key :bid) s/Num
            (s/optional-key :ask) s/Num
            :bidSize              s/Num :askSize s/Num :bidDepth s/Num
            :askDepth             s/Num :last s/Num :lastSize s/Num :lastTrade s/Inst :quoteTime s/Inst
            })
;(def quote-stream {:ok s/Bool :quote quote})
(def quote-stream {:ok s/Bool :quote s/Str})

(def execution {:ok    s/Bool :account s/Str :venue s/Str :symbol s/Str :order order :standingId s/Num :incomingId s/Num
                :price s/Num :filled s/Num :filledAt s/Inst :standingComplete s/Bool :incomingComplete s/Bool})

(def execution-stream {:spread s/Num :bids-avg s/Num :asks-avg s/Num})

(def autobuy {s/Keyword new-batch-order})





;; general
(def response-error (s/conditional #(= "false" (:ok %)) {:ok (s/eq "false") :error s/Str}
                                   :else {:status s/Num :error s/Str}))
(defn error-or-succ [succ] (s/conditional #(= false (:ok %)) {:ok (s/eq false) :error s/Str}
                                          #(not (nil? (:status %))) {:status s/Num :error s/Str}
                                          :else succ))
(def ok-response {:status (s/eq 200) :headers s/Any :body {:ok s/Str}})
(def common-state {:game-info          levels-response
                   :game-state         game-state
                   :restart-websockets s/Bool})

(def booking {:nav s/Num :position s/Num :cash s/Num})



; cljs
(def ticker {:bid-avg s/Num :bid-avg-last-10 s/Num :bid-avg-last-100 s/Num})

(def state {
            ;(s/optional-key :level)      levels
            (s/optional-key :orders)     orders
            (s/optional-key :ticker)     ticker
            (s/optional-key :game-state) game-state
            (s/optional-key :executions) execution-stream
            (s/optional-key :game-info)  game-info
            (s/optional-key :cur-level)  s/Str

            (s/optional-key :new-order)  {(s/optional-key :price)      s/Num (s/optional-key :qty) s/Num
                                          (s/optional-key :target-qty) s/Num
                                          (s/optional-key :direction)  direction
                                          (s/optional-key :orderType)  order-type}
            })

(def local-state {(s/optional-key :instanceId) s/Num (s/optional-key :vsa) vsa})