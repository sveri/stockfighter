(ns de.sveri.stockfighter.service.helper
  (:require [de.sveri.stockfighter.schema-api :as schem]
            [clj-time.format :as f]))

(def common-state (atom {:restart-websockets false}))

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
