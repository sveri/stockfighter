(ns de.sveri.stockfighter.api.state
  (:require [de.sveri.stockfighter.schema-api :as schem]
            [schema.core :as s]))

(def quotes-socket (atom {}))
(def executions-socket (atom {}))


(def quote-history (atom {}))
(def execution-history (atom {}))

; nav = cash + (shares * share_price)
(def booking (atom {:nav 0 :position 0 :cash 0 :avg-bid 0 :avg-ask 0 :ask-count 0 :bid-count 0 :buy-sell-lock false}))

(def order-book (atom {}))

(def open-orders (atom []))
(add-watch open-orders :orders-validation (fn [_ _ _ new] (s/validate schem/orders new)))
