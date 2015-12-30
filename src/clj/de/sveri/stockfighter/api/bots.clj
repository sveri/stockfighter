(ns de.sveri.stockfighter.api.bots
  (:require [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.schema-api :as schem]
            [schema.core :as s]
            [de.sveri.stockfighter.api.lvl-two :as two]
            [de.sveri.stockfighter.api.lvl-three :as three]))


(def autobuy-state (atom {}))

(s/defn enable-autobuy :- s/Any
  [venue :- s/Str stock :- s/Str account :- s/Str order :- schem/new-batch-order level :- schem/levels]
  (println "enabling autobuy for: " venue stock account " and level: " level)
  (swap! autobuy-state assoc (h/->unique-key venue stock account) (assoc order :level level)))

(s/defn disable-autobuy :- s/Any [vsa :- schem/vsa]
  (let [key (h/->unique-key vsa)
        lvl (:level (key @autobuy-state))]
    (println "disabling autobuy for: " vsa " and level: " lvl)
    (swap! autobuy-state dissoc key)))

(s/defn start-bot [{:keys [venue stock account] :as vsa} :- schem/vsa quote :- schem/quote orderbook :- schem/orderbooks
                   booking :- (s/atom schem/booking)]
  (let [key (h/->unique-key venue stock account)
        lvl (:level (key @autobuy-state))]
    (when-let [autobuy-data (key @autobuy-state)]
      (cond
        (= lvl "chock_a_block") (two/autobuy autobuy-data quote)
        (= lvl "sell_side") (three/start-lvl-three vsa orderbook booking)))))

