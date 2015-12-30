(ns de.sveri.stockfighter.routes.stockfighter
  (:require [compojure.core :refer [routes GET POST defroutes]]
            [clojure.core.memoize :as mem]
            [ring.util.response :refer [response status]]
            [schema.core :as s]
            [clojure.core.async :as a :refer [<! >! put! close! go]]
            [de.sveri.stockfighter.layout :as layout]
            [de.sveri.stockfighter.schema-api :as schem]
    ;[de.sveri.stockfighter.components.websockets :as ws-comp]
            [de.sveri.stockfighter.api.api :as api]
            [de.sveri.stockfighter.api.websockets :as ws]
            [de.sveri.stockfighter.service.jobs :as qh]
            [de.sveri.stockfighter.service.jobs :as jobs]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.api.orders :as o]
            [de.sveri.stockfighter.api.bots :as bots]))

(defmulti fail-or-result (fn [result _] (contains? result :error)))
(defmethod fail-or-result false [r path] (response (get-in r path)))
(defmethod fail-or-result true [r _] (status (response {:error (:error r)}) (:status r)))

(def cached-orders (mem/ttl #(api/->orders %1 %2 %3) :ttl/threshold 1200000))

(defn index-page [] (layout/render "stockfighter.html"))

(s/defn orders :- (s/cond-pre {:status s/Num :headers s/Any :body schem/orders}
                              {:status s/Num :headers s/Any :body s/Any})
  [venue stock account]
  (let [orders (cached-orders venue stock account)]
    (fail-or-result orders [:orders])))

(s/defn new-order :- s/Any
  [order :- schem/new-batch-order]
  ;(o/better-new-order order)
  (response {:ok "ok"}))

;(s/defn start-quote-ticker :- s/Any [vsa :- schem/vsa]
;  (ws/connect-quotes vsa)
;  ;(qh/start-pass-averages vsa ws-comp/ws)
;  (response {:ok "ok"}))
;
;(s/defn stop-quote-ticker :- s/Any [vsa :- schem/vsa]
;  (ws/close-sockets-by-key vsa)
;  (qh/delete-pass-averages vsa)
;  (response {:ok "ok"}))

(s/defn start-ticker :- s/Any [{:keys [venue stock] :as vsa} :- schem/vsa websockets :- s/Any]
  (ws/connect-quotes vsa)
  (ws/connect-executions vsa)
  (qh/start-pass-averages vsa websockets)
  (qh/start-pass-executions vsa websockets)
  (jobs/start-game-info (get-in @h/common-state [:game-info :instanceId]) vsa websockets)
  (jobs/start-order-book venue stock ws/order-book websockets)
  (h/restart-api-websockets true)
  (response {:ok "ok"}))

(s/defn stop-ticker :- s/Any [{:keys [venue stock] :as vsa} :- schem/vsa]
  (h/restart-api-websockets false)
  (ws/close-sockets-by-key vsa)
  (qh/delete-pass-averages vsa)
  (jobs/delete-game-info vsa)
  (jobs/delete-executions vsa)
  (jobs/stop-order-book venue stock)
  (response {:ok "ok"}))

(s/defn enable-bots :- s/Any
  [order :- schem/new-batch-order]
  (bots/enable-bots (select-keys order [:venue :stock :account]) order (:level order))
  (response {:ok "ok"}))

(s/defn stop-bots :- s/Any
  [vsa :- schem/vsa]
  (bots/disable-bots vsa)
  (response {:ok "ok"}))

(defn stockfighter-routes [websockets]
  (routes (GET "/stockfighter" [] (index-page))
          (GET "/stockfighter/orders/venue/:venue/stock/:stock/account/:account"
               [venue stock account] (orders venue stock account))
          (POST "/stockfighter/orders" req (new-order (:params req)))
          (POST "/stockfighter/bots/start" req (enable-bots (:params req)))
          (POST "/stockfighter/bots/stop" req (stop-bots (:params req)))
          ;(POST "/stockfighter/quoteticker/start" req (start-quote-ticker (:params req)))
          ;(POST "/stockfighter/quoteticker/stop" req (stop-quote-ticker (:params req)))
          (POST "/stockfighter/ticker/start" req (start-ticker (:params req) websockets))
          (POST "/stockfighter/ticker/stop" req (stop-ticker (:params req)))))

(defn ws-routes [websockets]
  (routes (GET "/stockfighter/qoutes/ws" req ((:ajax-get-or-ws-handshake-fn websockets) req))
          (POST "/stockfighter/qoutes/ws" req ((:ajax-post-fn websockets) req))))