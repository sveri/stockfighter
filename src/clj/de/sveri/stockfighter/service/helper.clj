(ns de.sveri.stockfighter.service.helper
  (:require [de.sveri.stockfighter.schema-api :as schem]
            [clj-time.format :as f]
            [schema.core :as s]))

(def common-state (atom {:restart-websockets false}))

(defn ->vsa []
  {:account (:account (:game-info @common-state)) :venue (first (:venues (:game-info @common-state))) :stock (first (:tickers (:game-info @common-state)))})

(defn ->instanceid []
  (get-in @common-state [:game-info :instanceId]))

(defn ->unique-key
  ([{:keys [venue stock account]}] (->unique-key venue stock account))
  ([venue stock & [account]] (keyword (format "%s-%s-%s" venue stock account))))

(defn restart-api-websockets [on?]
  (swap! common-state assoc :restart-websockets on?))

(defn restart-api-websockets? [] (:restart-websockets @common-state))

(defn api->date [key value]
  (if (contains? #{:quoteTime :lastTrade :ts :filledAt} key)
    (.toDate (f/parse schem/api-time-format value))
    value))


(s/defn ->new-order [{:keys [venue stock account] :as vsa} :- schem/vsa buy-or-sell :- schem/direction price :- s/Num qty :- s/Num]
        {:account account :venue venue :stock stock :price price :qty qty :direction buy-or-sell :orderType "limit"})