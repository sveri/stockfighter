(ns de.sveri.stockfighter.api.config
  (:require [environ.core :refer [env]]))

(def api-key (env :stock-key))

(def base-uri "https://api.stockfighter.io/ob/api/")
(def ws-uri "wss://api.stockfighter.io/ob/api/ws/")
(def gm-uri "https://www.stockfighter.io/gm/")