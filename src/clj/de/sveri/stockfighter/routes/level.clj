(ns de.sveri.stockfighter.routes.level
  (:require [compojure.core :refer [defroutes POST GET]]
            [schema.core :as s]
            [ring.util.response :refer [response status]]
            [de.sveri.stockfighter.api.api :as api]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.schema-api :as schem]))



(s/defn start :- s/Any [params :- {:name s/Str}]
  (let [game-info @(api/start-game (:name params))]
    (if (:ok game-info)
      (do (swap! h/common-state assoc :game-info game-info)
          (response game-info))
      (status (response game-info) 500))))

(defn get-level-state [instance]
  (response @(api/get-level-info instance)))


(defroutes level-routes
           (POST "/level/start" req (start (:params req)))
           (GET "/level/state/instance/:instance" [instance] (get-level-state instance))

           #_(POST "/level/three/start" req (start-lvl-three (:params req)))
           #_(POST "/level/three/stop" req (stop-lvl-three (:params req))))
