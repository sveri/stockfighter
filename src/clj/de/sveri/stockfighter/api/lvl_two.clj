(ns de.sveri.stockfighter.api.lvl-two
  (:require [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.api.api :as api]))


(s/defn autobuy :- s/Any [autobuy-data :- schem/new-batch-order quote :- s/Any]
  (when (and (:bid quote) (<= (:bid quote) (:price autobuy-data)))
    @(api/new-order (dissoc autobuy-data :level :target-qty))))