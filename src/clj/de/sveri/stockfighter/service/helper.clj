(ns de.sveri.stockfighter.service.helper)

(def common-state (atom {}))

(defn ->unique-key
  ([{:keys [venue state account]}] (->unique-key venue state account))
  ([venue stock & [account]] (keyword (format "%s-%s-%s" venue stock account))))
