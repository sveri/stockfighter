(ns de.sveri.stockfighter.service.jobs
  (:require
    ;[de.sveri.stockfighter.components.quartzite :refer [quartzite]]
    ;        [de.sveri.stockfighter.components.websockets :refer [ws]]
            [de.sveri.stockfighter.api.websockets :as api-ws]
            [de.sveri.stockfighter.api.api :as stock-api]
            [schema.core :as s]
            [de.sveri.stockfighter.schema-api :as schem]
            [de.sveri.stockfighter.service.helper :as h]
            [immutant.scheduling :refer :all]))

(s/defn start-pass-averages* :- s/Any
  [{:keys [venue stock account]} :- schem/vsa send-fn :- s/Any conn-uids :- s/Any]
  (doseq [uid (:any @conn-uids)]
    (send-fn uid [:quotes/averages
                  {:bid-avg          (api-ws/get-avg-bid venue stock account)
                   :bid-avg-last-10  (api-ws/get-avg-bid venue stock account 10)
                   :bid-avg-last-100 (api-ws/get-avg-bid venue stock account 100)}])))

(s/defn start-pass-averages :- s/Any [vsa :- schem/vsa {:keys [send-fn connected-uids]} :- s/Any]
  (let [key (keyword (str "quot-avg-" (h/->unique-key vsa)))]
    (schedule  #(start-pass-averages* vsa send-fn connected-uids) (-> (id key) (every 5 :seconds)))))

(defn delete-pass-averages [vsa]
  (stop (id (keyword (str "quot-avg-" (h/->unique-key vsa))))))




(s/defn start-pass-executions* :- s/Any
  [{:keys [venue stock account]} :- schem/vsa send-fn :- s/Any conn-uids :- s/Any]
  (doseq [uid (:any @conn-uids)]
    (send-fn uid [:executions/last (api-ws/->accumulated-executions venue stock account)])))

(s/defn start-pass-executions :- s/Any [vsa :- schem/vsa {:keys [send-fn connected-uids]} :- s/Any]
  (let [key (keyword (str "executions" (h/->unique-key vsa)))]
    (schedule  #(start-pass-executions* vsa send-fn connected-uids) (-> (id key) (every 5 :seconds)))))

(defn delete-executions [vsa]
  (stop (id (keyword (str "executions" (h/->unique-key vsa))))))




(s/defn game-info* :- s/Any
  [instance-id :- s/Num send-fn :- s/Any conn-uids :- s/Any]
  (let [game-info (stock-api/get-level-info instance-id)]
    (swap! h/common-state assoc :game-state game-info)
    (doseq [uid (:any @conn-uids)]
     (send-fn uid [:game/info game-info]))))

(s/defn start-game-info :- s/Any [instance-id :- s/Num vsa :- schem/vsa {:keys [send-fn connected-uids]} :- s/Any]
  (let [key (keyword (str "game-info" (h/->unique-key vsa)))]
    (schedule #(game-info* instance-id send-fn connected-uids) (-> (id key) (every 60 :seconds)))))

(defn delete-game-info [vsa]
  (stop (id (keyword (str "game-info" (h/->unique-key vsa))))))
