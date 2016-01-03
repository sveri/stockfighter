(ns de.sveri.stockfighter.web.setup
  (:require
    ;[reloaded.repl :refer [go stop]]
            [clj-webdriver.taxi :as w]
            #_[com.stuartsierra.component :as component]
            [mount.core :as mount]
            [joplin.core :as j]
            [taoensso.tower :as tower]
            [de.sveri.stockfighter.components.config]
            [de.sveri.stockfighter.components.locale :refer [get-tconfig]]
            ))

(def db-uri "jdbc:sqlite:./db/stockfigherui-integ-test.sqlite")
(def migrators "resources/migrators/sqlite")

; custom config for configuration
(def test-config
  {:hostname                "http://localhost/"
   :mail-from               "info@localhost.de"
   :mail-type               :test
   :activation-mail-subject "Please activate your account."
   :activation-mail-body    "Please click on this link to activate your account: {{activationlink}}
Best Regards,

Your Team"
   :activation-placeholder  "{{activationlink}}"
   :smtp-data               {}                                ; passed directly to postmap like {:host "postfix"}
   :jdbc-url                db-uri
   :env                     :dev
   :registration-allowed?   true
   :captcha-enabled?        false
   :captcha-public-key      "your public captcha key"
   :private-recaptcha-key   "your private captcha key"
   :recaptcha-domain        "yourdomain"
   :port                    3001})



(def test-base-url (str "http://localhost:3001/"))

(defn start-browser [browser]
  (j/reset-db
    {:db       {:type :sql,
                :url  db-uri}
     :migrator migrators})
  (w/set-driver! {:browser browser}))

(defn stop-browser []
  (w/quit))

;(defn start-server []
;  (reloaded.repl/set-init! test-system)
;  (go))
;
;(defn stop-server []
;  (stop))

(defn server-setup [f]
  ;(start-server)
  ;(mount/start)

  ;(mount/start-with {#'de.sveri.stockfighter.components.config/config
  ;                   #'de.sveri.stockfighter.web.setup/test-config})
  (f)
  ;(stop-server)
  ;(mount/stop)
  )

(defn browser-setup [f]
  (start-browser :htmlunit)
  (f)
  (stop-browser))

;; locale stuff

;(def t nil)
(def t (tower/make-t (:tconfig (get-tconfig))))
