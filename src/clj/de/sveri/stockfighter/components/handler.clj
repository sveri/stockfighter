(ns de.sveri.stockfighter.components.handler
  (:require [compojure.core :refer [defroutes routes]]
            [noir.response :refer [redirect]]
            [noir.util.middleware :refer [app-handler]]
            [ring.middleware.defaults :refer [site-defaults]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [compojure.route :as route]
            [mount.core :refer [defstate]]
            [de.sveri.stockfighter.components.config :refer [config]]
            [de.sveri.stockfighter.components.locale :refer [get-tconfig]]
            [de.sveri.stockfighter.routes.home :refer [home-routes]]
            [de.sveri.stockfighter.routes.cc :refer [cc-routes]]
            [de.sveri.stockfighter.routes.user :refer [user-routes registration-routes]]
            [de.sveri.stockfighter.routes.stockfighter :refer [stockfighter-routes ws-routes]]
            [de.sveri.stockfighter.routes.level :refer [level-routes]]
            [de.sveri.stockfighter.middleware :refer [load-middleware]]))

(defroutes base-routes
  (route/resources "/")
  (route/not-found "Not Found"))

;; timeout sessions after 30 minutes
(def session-defaults
  {:timeout (* 60 30)
   :timeout-response (redirect "/")})

(defn- mk-defaults
       "set to true to enable XSS protection"
       [xss-protection?]
       (-> site-defaults
           (update-in [:session] merge session-defaults)
           (assoc-in [:security :anti-forgery] xss-protection?)))


;(def app-routes
;  (routes
;    #service-routes
;        (wrap-routes #'restricted-service-routes middleware/wrap-auth)
;    (wrap-routes #'home-routes middleware/wrap-csrf)
;    (route/not-found
;      (:body
;        (error-page {:status 404
;                     :title "page not found"})))))

(defn- pre-init [middleware]
  (let [proxy (middleware (fn [req] ((:route-handler req) req)))]
    (fn [handler]
      (fn [request]
        (proxy (assoc request :route-handler handler))))))

(defn wrap-routes
  "Apply a middleware function to routes after they have been matched."
  ([handler middleware]
   (let [middleware (pre-init middleware)]
     (fn [request]
       (let [mw (:route-middleware request identity)]
         (handler (assoc request :route-middleware (comp middleware mw)))))))
  ([handler middleware & args]
   (wrap-routes handler #(apply middleware % args))))


(defn get-handler [config locale]
  (routes
    (-> ws-routes
        (wrap-routes wrap-params)
        (wrap-routes wrap-keyword-params))
    (-> (app-handler
         (into [] (concat (when (:registration-allowed? config) [(registration-routes config)])
                          ;; add your application routes here
                          [level-routes (cc-routes config) home-routes (stockfighter-routes config) (user-routes config) base-routes]))
         ;; add custom middleware here
         :middleware (load-middleware config (:tconfig locale))
         :ring-defaults (mk-defaults false)
         ;; add access rules here
         :access-rules []
         ;; serialize/deserialize the following data formats
         ;; available formats:
         ;; :json :json-kw :yaml :yaml-kw :edn :yaml-in-html
         ;:formats [:transit-json]
         :formats [:json-kw :edn :transit-json]
         )
       ; Makes static assets in $PROJECT_DIR/resources/public/ available.
       (wrap-file "resources")
       ; Content-Type, Content-Length, and Last Modified headers for files in body
       (wrap-file-info)
       )))

;(defrecord Handler [config locale]
;  comp/Lifecycle
;  (start [comp]
;    (assoc comp :handler (get-handler (:config config) locale)))
;  (stop [comp]
;    (assoc comp :handler nil)))
;
;(defn new-handler []
;  (map->Handler {}))

(defstate handler :start (get-handler config (get-tconfig)))