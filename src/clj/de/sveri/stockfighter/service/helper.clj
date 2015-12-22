(ns de.sveri.stockfighter.service.helper)

(def common-state (atom {:restart-websockets false}))

(defn ->unique-key
  ([{:keys [venue stock account]}] (->unique-key venue stock account))
  ([venue stock & [account]] (keyword (format "%s-%s-%s" venue stock account))))

(defn restart-api-websockets [on?]
  (swap! common-state assoc :restart-websockets on?))

(defn restart-api-websockets? [] (:restart-websockets @common-state))