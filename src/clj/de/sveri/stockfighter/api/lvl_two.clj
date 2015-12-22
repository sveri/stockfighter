(ns de.sveri.stockfighter.api.lvl-two
  (:require [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.api.api :as api]))



(def autobuy-state (atom {}))

(s/defn enable-autobuy :- s/Any [venue :- s/Str stock :- s/Str account :- s/Str order :- schem/new-batch-order]
        (println "enabling autobuy for: " venue stock account)
        (swap! autobuy-state assoc (h/->unique-key venue stock account) order))

(s/defn disable-autobuy :- s/Any [vsa :- schem/vsa]
        (println "disabling autobuy for: " vsa)
        (swap! autobuy-state dissoc (h/->unique-key vsa)))

(s/defn autobuy :- s/Any [{:keys [venue stock account] :as vsa} :- schem/vsa quote :- s/Any quote-history :- s/Any]
        (let [key (h/->unique-key venue stock account)]
          (when-let [autobuy-data (key @autobuy-state)]
            (when (and (:bid quote) (<= (:bid quote) (:price autobuy-data)))
              (api/new-order autobuy-data)))
          (swap! quote-history update key conj quote)))