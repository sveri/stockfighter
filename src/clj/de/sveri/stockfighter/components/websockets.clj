(ns de.sveri.stockfighter.components.websockets
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [mount.core :refer [defstate]]))

(defn start-ws []
  (sente/make-channel-socket! sente-web-server-adapter {})
  ;ajax-post-fn)
  ;ajax-get-or-ws-handshake-fn)
  ;ch-recv)                                    ; ChannelSocket's receive channel
  ;send-fn)                                 ; ChannelSocket's send API fn
  ;connected-uids)                      ; Watchable, read-only atom
  )

(defstate ws :start (start-ws))

