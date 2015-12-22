(ns de.sveri.stockfighter.components.components
  (:require
    [com.stuartsierra.component :as component]
    (system.components
      [repl-server :refer [new-repl-server]])
    [de.sveri.stockfighter.components.server :refer [new-web-server-prod]]
    [de.sveri.stockfighter.components.handler :refer [new-handler]]
    [de.sveri.stockfighter.components.config :as c]
    [de.sveri.stockfighter.components.db :refer [new-db]]
    [de.sveri.stockfighter.components.locale :as l]
    [de.sveri.stockfighter.components.websockets :as ws]
    [de.sveri.stockfighter.components.scheduler :as s]))


(defn dev-system []
  (component/system-map
    :locale (l/new-locale)
    :websockets (ws/new-websockets)
    :scheduler (s/new-scheduler)
    :config (c/new-config (c/prod-conf-or-dev))
    :db (component/using (new-db) [:config])
    :handler (component/using (new-handler) [:config :locale :websockets])
    :web (component/using (new-web-server-prod) [:handler :config])))


(defn prod-system []
  (component/system-map
    :locale (l/new-locale)
    :websockets (ws/new-websockets)
    :scheduler (s/new-scheduler)
    :config (c/new-config (c/prod-conf-or-dev))
    :db (component/using (new-db) [:config])
    :handler (component/using (new-handler) [:config :locale :websockets])
    :web (component/using (new-web-server-prod) [:handler :config])))
