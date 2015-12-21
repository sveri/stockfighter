(ns de.sveri.stockfighter.routes.level
  (:require [compojure.core :refer [defroutes POST GET]]
            [schema.core :as s]
            [ring.util.response :refer [response]]
            [de.sveri.stockfighter.api.api :as api]
            [de.sveri.stockfighter.service.helper :as h]))



(s/defn start :- s/Any [params :- {:name s/Str}]
  (let [game-info (api/start-game (:name params))]
    (swap! h/common-state assoc :game-info game-info)
    (response game-info)))

(defn get-level-state [instance]
  (response (api/get-level-info instance)))

(defroutes level-routes
           (POST "/level/start" req (start (:params req)))
           (GET "/level/state/instance/:instance" [instance] (get-level-state instance)))
