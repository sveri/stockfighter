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
         [:game/booking m] (swap! state assoc :booking m)
         [:executions/last m] (do (swap! state assoc :executions m)
                                  (swap! state update :executions-full conj
                                         (if-let [new-exec (:last-execution m)]
                                           (let [latest-exe (last (:executions-full @state))]
                                             (if (not= (get-in new-exec [:order :id]) (get-in latest-exe [:order :id]))
                                               new-exec
                                               nil))
                                           nil))
                                  #_(println (count (:executions-full @state))))
         [:order/order-book orderbook] (swap! state update :orderbook conj (:orderbook orderbook))))

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


