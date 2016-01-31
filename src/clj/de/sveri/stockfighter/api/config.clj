(ns de.sveri.stockfighter.api.config
  (:require [environ.core :refer [env]]))

(def api-key (env :stock-key))

;(def base-uri "https://api.stockfighter.io/ob/api/")
;(def ws-uri "wss://api.stockfighter.io/ob/api/ws/")
;(def gm-uri "https://www.stockfighter.io/gm/")
(def base-uri "http://stackfooter.rjsamson.org/ob/api/")
(def ws-uri "ws://stackfooter.rjsamson.org/ob/api/ws/")
;(def gm-uri "https://stackfooter.rjsamson.org/gm/")

(def pvp-account "sveri1717")

(def pvp-stock "NYC")
(def pvp-venue "OBEX")

(def vsa {:venue pvp-venue :stock pvp-stock :account pvp-account })