(ns de.sveri.stockfighter.components.db
  (:require #_[mount.core :refer [defstate]]
    [korma.db :as korma]
    ;[de.sveri.stockfighter.components.config :refer [config]]
    [com.stuartsierra.component :as component]))

(defrecord Db [config]
  component/Lifecycle
  (start [component]
    (let [db-url (get-in config [:config :jdbc-url])
          db (korma/create-db db-url)]
          (korma/default-connection db))
    component)
  (stop [component] component))

(defn new-db []
  (map->Db {}))

;(defn new-db [config]
;  (let [db (korma/create-db (get-in config [:jdbc-url]))]
;    (korma/default-connection db)))
;
;(defstate db :start (new-db config))
