(ns de.sveri.stockfighter.routes.stockfighter
  (:require [compojure.core :refer [routes GET POST defroutes]]
            [clojure.core.memoize :as mem]
            [ring.util.response :refer [response status]]
            [schema.core :as s]
            [clojure.core.async :as a :refer [<! >! put! close! go]]
            [de.sveri.stockfighter.layout :as layout]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.components.websockets :as ws-comp]
            [de.sveri.stockfighter.api.api :as api]
            [de.sveri.stockfighter.api.websockets :as ws]
            [de.sveri.stockfighter.service.jobs :as qh]
            [de.sveri.stockfighter.service.jobs :as jobs]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.api.orders :as o]))

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
  (o/better-new-order order)
  (response {:ok "ok"}))

(s/defn start-quote-ticker :- s/Any [vsa :- schem/vsa]
  (ws/connect-quotes vsa)
  (qh/start-pass-averages vsa ws-comp/ws)
  (response {:ok "ok"}))

(s/defn stop-quote-ticker :- s/Any [vsa :- schem/vsa]
  (ws/close-sockets-by-key vsa)
  (qh/delete-pass-averages vsa)
  (response {:ok "ok"}))

(s/defn start-ticker :- s/Any [vsa :- schem/vsa]
  (ws/connect-quotes vsa)
  (ws/connect-executions vsa)
  (qh/start-pass-averages vsa ws-comp/ws)
  (qh/start-pass-executions vsa ws-comp/ws)
  (jobs/start-game-info (get-in @h/common-state [:game-info :instanceId]) vsa ws-comp/ws)
  (response {:ok "ok"}))

(s/defn stop-ticker :- s/Any [vsa :- schem/vsa]
  (ws/close-sockets-by-key vsa)
  (qh/delete-pass-averages vsa)
  (jobs/delete-game-info vsa)
  (jobs/delete-executions vsa)
  (response {:ok "ok"}))

(s/defn new-autobuy :- s/Any
  [{:keys [venue symbol account price qty] :as order} :- schem/new-batch-order]
  (ws/enable-autobuy venue symbol account price qty)
  (response {:ok "ok"}))

(s/defn new-autobuy-stop :- s/Any
  [vsa :- schem/vsa]
  (ws/disaple-autobuy vsa)
  (response {:ok "ok"}))

(defn stockfighter-routes [config]
  (routes (GET "/stockfighter" [] (index-page))
          (GET "/stockfighter/orders/venue/:venue/stock/:stock/account/:account"
               [venue stock account] (orders venue stock account))
          (POST "/stockfighter/orders" req (new-order (:params req)))
          (POST "/stockfighter/autobuy" req (new-autobuy (:params req)))
          (POST "/stockfighter/autobuy/stop" req (new-autobuy-stop (:params req)))
          (POST "/stockfighter/quoteticker/start" req (start-quote-ticker (:params req)))
          (POST "/stockfighter/quoteticker/stop" req (stop-quote-ticker (:params req)))
          (POST "/stockfighter/ticker/start" req (start-ticker (:params req)))
          (POST "/stockfighter/ticker/stop" req (stop-ticker (:params req)))

          ))

(defroutes ws-routes
           (GET  "/stockfighter/qoutes/ws" req ((:ajax-get-or-ws-handshake-fn ws-comp/ws) req))
           (POST "/stockfighter/qoutes/ws" req ((:ajax-post-fn ws-comp/ws) req)))