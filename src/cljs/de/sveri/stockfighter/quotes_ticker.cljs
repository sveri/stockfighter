(ns de.sveri.stockfighter.quotes-ticker
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require
    [cljs.core.async :as async :refer [<! >! put! chan]]
    [taoensso.sente  :as sente :refer [cb-success?]] ; <--- Add this
    [cljs.core.match :refer-macros [match]]
    ))



(defmulti event-msg-handler (fn [_ event] (:id event)))

(defmethod event-msg-handler :default
  [_ {:keys [event]}]
  (println "Unhandled event: %s" event))

(defmethod event-msg-handler :chsk/recv
  [state {:keys [?data]}]
  (match ?data
         [:quotes/averages m] (swap! state assoc :ticker m)
         [:game/info m] (swap! state assoc :game-info m)
         [:executions/last m] (swap! state assoc :executions m)))

(def     router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))


(defn start-router! [state-atom]
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! "/stockfighter/qoutes/ws" ; Note the same path as before
                                    {:type :auto ; e/o #{:auto :ajax :ws}
                                     })]
    (def chsk       chsk)
    (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
    (def chsk-send! send-fn) ; ChannelSocket's send API fn
    (def chsk-state state)   ; Watchable, read-only atom
    (stop-router!)
    (reset! router_ (sente/start-chsk-router! ch-chsk (fn [event] (event-msg-handler state-atom event))))
    ))


