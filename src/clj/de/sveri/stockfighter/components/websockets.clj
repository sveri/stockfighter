(ns de.sveri.stockfighter.components.websockets
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
    #_[mount.core :refer [defstate]]
            [com.stuartsierra.component :as component]))

;(defn start-ws []
;  (sente/make-channel-socket! sente-web-server-adapter {})
;  ;ajax-post-fn)
;  ;ajax-get-or-ws-handshake-fn)
;  ;ch-recv)                                    ; ChannelSocket's receive channel
;  ;send-fn)                                 ; ChannelSocket's send API fn
;  ;connected-uids)                      ; Watchable, read-only atom
;  )

;(defstate ws :start (start-ws))


(defrecord Websockets []
  component/Lifecycle
  (start [component]
      (assoc component :websockets (sente/make-channel-socket! sente-web-server-adapter {})))
  (stop [component] (dissoc component :websockets)))

(defn new-websockets []
  (map->Websockets {}))
