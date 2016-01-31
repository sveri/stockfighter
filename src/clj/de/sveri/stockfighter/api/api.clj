(ns de.sveri.stockfighter.api.api
  (:require [de.sveri.stockfighter.api.config :as conf :refer [api-key base-uri gm-uri]]
            [clj-http.client :as client]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [clojure.data.json :as json]
            [taoensso.timbre :as timb]
            [de.sveri.stockfighter.service.helper :as h]))


(defn with-key-and-defaults
  ([] (with-key-and-defaults {}))
  ([m] (merge m {:throw-exceptions false
                 :headers          {"X-Starfighter-Authorization" api-key}})))

(defmulti parse-response (fn [m] (:status m)))
(defmethod parse-response 200 [m]
  (json/read-str (:body m) :key-fn keyword :value-fn h/api->date))

(defmethod parse-response :default [m]
  (let [body (json/read-str (:body m) :key-fn keyword)]
    {:status (:status m) :error (:error body)}))

(defn up? []
  (client/get (str base-uri "heartbeat") (with-key-and-defaults)))

(defn venue-up? [venue]
  (client/get (str base-uri "venues/" venue "/heartbeat") (with-key-and-defaults)))

(s/defn stocks? :- schem/stocks
  [venue :- s/Str]
  (parse-response (client/get (str base-uri "venues/" venue "/stocks") (with-key-and-defaults))))

(s/defn ->orderbook :- schem/order-book
  [venue :- s/Str stock :- s/Str]
  (parse-response (client/get (str base-uri "venues/" venue "/stocks/" stock) (with-key-and-defaults))))

(s/defn new-order :- s/Any [order :- schem/new-order]
  (parse-response (client/post (str base-uri "venues/" (:venue order) "/stocks/" (:stock order) "/orders")
                               (with-key-and-defaults {:body (json/write-str order)}))))

(s/defn ->order-status :- (s/maybe schem/order) [venue stock id :- s/Num]
  (parse-response (client/get (str base-uri "venues/" venue "/stocks/" stock "/orders/" id)
                              (with-key-and-defaults))))

(s/defn delete-order :- (s/maybe schem/order) [venue stock id :- s/Num]
  (parse-response (client/delete (str base-uri "venues/" venue "/stocks/" stock "/orders/" id)
                              (with-key-and-defaults))))


(s/defn ->orders :- (s/cond-pre {:ok s/Bool :orders schem/orders} schem/response-error)
  [venue :- s/Str stock :- s/Str account :- s/Str]
  (timb/info (format "fetching orders for venue: %s - stock: %s - account: %s" venue stock account))
  (parse-response (client/get (str base-uri "venues/" venue "/accounts/" account "/stocks/" stock "/orders")
                              (with-key-and-defaults))))




;;; level

(s/defn start-game :- (schem/error-or-succ schem/levels-response) [name :- s/Str]
  (parse-response (client/post (str gm-uri "levels/" name) (with-key-and-defaults))))

(defn get-level-info [instance]
  (parse-response (client/get (str gm-uri "instances/" instance) (with-key-and-defaults))))

(defn stop-game [instance]
  (parse-response (client/get (str gm-uri "instances/" instance "/stop") (with-key-and-defaults))))