(ns de.sveri.stockfighter.service.helper)

(def common-state (atom {}))

(defn ->unique-key
  ([{:keys [venue stock account]}] (->unique-key venue stock account))
  ([venue stock & [account]] (keyword (format "%s-%s-%s" venue stock account))))
