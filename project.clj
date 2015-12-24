(defproject stockfigherui "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.189"]

                 ;[org.clojure/core.cache "0.6.4"]
                 ;[org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 [ring "1.4.0"]
                 [lib-noir "0.9.9"]
                 [org.eclipse.jetty/jetty-server "9.3.6.v20151106"]
                 [ring/ring-jetty-adapter "1.4.0" :exclusions [org.eclipse.jetty/jetty-server]]
                 ;[ring-server "0.4.0"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [compojure "1.4.0"]
                 [reagent "0.5.1"]
                 [environ "1.0.1"]
                 [leiningen "2.5.1"]
                 [http-kit "2.1.19"]
                 [selmer "0.9.5"]
                 [prone "0.8.2"]
                 [im.chit/cronj "1.4.4"]
                 [com.taoensso/timbre "4.1.4"]
                 [noir-exception "0.2.5"]

                 [buddy/buddy-auth "0.8.2"]
                 [buddy/buddy-hashers "0.9.1"]

                 [log4j "1.2.17" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]

                 [org.clojure/java.jdbc "0.4.2"]
                 [korma "0.4.2"]
                 [com.h2database/h2 "1.4.190"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]

                 [com.draines/postal "1.11.4"]

                 [jarohen/nomad "0.7.2"]

                 [de.sveri/clojure-commons "0.2.0"]

                 [clojure-miniprofiler "0.5.0"]

                 [datascript "0.13.3"]
                 [cljs-ajax "0.5.2"]
                 [ring-transit "0.1.4"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]

                 [net.tanesha.recaptcha4j/recaptcha4j "0.0.8"]

                 [com.taoensso/tower "3.0.2"]

                 [org.clojure/core.typed "0.3.19"]
                 [prismatic/plumbing "0.5.2"]
                 [prismatic/schema "1.0.4"]

                 [com.rpl/specter "0.9.0"]

                 [clj-http "2.0.0"]
                 [alandipert/storage-atom "1.2.4"]
                 [org.clojure/core.memoize "0.5.8"]

                 [org.clojure/core.async "0.2.374"]
                 [stylefruits/gniazdo "0.4.1"]
                 [com.taoensso/encore "2.29.1"]
                 [com.taoensso/sente "1.7.0"]
                 ;[jarohen/chord "0.7.0"]

                 ;[cheshire "5.5.0"]
                 [org.clojure/data.json "0.2.6"]
                 ;[clojurewerkz/quartzite "2.0.0"]
                 ;[twarc "0.1.8"]
                 [org.immutant/scheduling "2.1.1"]

                 [org.danielsz/system "0.1.8"]

                 ;[org.clojure/tools.namespace "0.2.11"]
                 ;[mount "0.1.6"]
                 [clj-time "0.11.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]]

  :plugins [[de.sveri/closp-crud "0.1.4"]
            [lein-cljsbuild "1.1.1"]]

  ;database migrations
  :joplin {:migrators {:sqlite-mig "resources/migrators/sqlite"
                       :h2-mig "resources/migrators/h2"}}

  :closp-crud {:jdbc-url "jdbc:sqlite:./db/stockfigherui.sqlite"
               :migrations-output-path "./resources/migrators/sqlite"
               :clj-src "src/clj"
               :ns-db "de.sveri.stockfighter.db"
               :ns-routes "de.sveri.stockfighter.routes"
               :ns-layout "de.sveri.stockfighter.layout"
               :templates "resources/templates"}

  :min-lein-version "2.5.0"

  ; leaving this commented because of: https://github.com/cursiveclojure/cursive/issues/369
  ;:hooks [leiningen.cljsbuild]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild
  {:builds {:dev {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                  :figwheel {:css-dirs ["resources/public/css"]             ;; watch and update CSS
                             :on-jsload "stockfigherui.dev/main"}
                  :compiler     {:main           "stockfigherui.dev"
                                 :asset-path     "/js/compiled/out"
                                 :output-to      "resources/public/js/compiled/app.js"
                                 :output-dir     "resources/public/js/compiled/out"}}
            :adv {:source-paths ["src/cljs" "src/cljc"]
                  :compiler     {:output-to     "resources/public/js/compiled/app.js"
                                 ; leaving this commented because of: https://github.com/cursiveclojure/cursive/issues/369
                                 ;:jar           true
                                 :optimizations :advanced
                                 :pretty-print  false}}}}

  :profiles {:dev     {:repl-options {:init-ns          de.sveri.stockfighter.user}

                       :plugins      [[lein-ring "0.9.0"]
                                      [lein-figwheel "0.5.0-2"]
                                      [joplin.lein "0.2.17"]
                                      [test2junit "1.1.1"]]

                       :dependencies [[org.bouncycastle/bcprov-jdk15on "1.53"]

                                      ; use this for htmlunit or an older firefox version
                                      [clj-webdriver "0.7.2"
                                       :exclusions [org.seleniumhq.selenium/selenium-server]]

                                      ; uncomment this to use current firefox version (does not work with htmlunit
                                      ;[clj-webdriver "0.6.1"
                                      ; :exclusions
                                      ; [org.seleniumhq.selenium/selenium-server
                                      ;  org.seleniumhq.selenium/selenium-java
                                      ;  org.seleniumhq.selenium/selenium-remote-driver]]

                                      [org.seleniumhq.selenium/selenium-server "2.48.2"]
                                      [ring-mock "0.1.5"]
                                      [ring/ring-devel "1.4.0"]
                                      [pjstadig/humane-test-output "0.7.1"]
                                      [joplin.core "0.2.17"]
                                      [joplin.jdbc "0.2.17"]]

                       :injections   [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]

                       :joplin {:databases {:sqlite-dev {:type :sql, :url "jdbc:sqlite:./db/stockfigherui.sqlite"}
                                            :h2-dev {:type :sql, :url "jdbc:h2:./db/korma.db;DATABASE_TO_UPPER=FALSE"}}
                                :environments {:sqlite-dev-env [{:db :sqlite-dev, :migrator :sqlite-mig}]
                                               :h2-dev-env [{:db :h2-dev, :migrator :h2-mig}]}}}

             :uberjar {:auto-clean false                    ; not sure about this one
                       :omit-source true
                       :aot         :all}}

  :test-paths ["test/clj" "integtest/clj"]

  :test-selectors {:unit (complement :integration)
                   :integration :integration
                   :cur :cur                                ; one more selector for, give it freely to run only
                                                            ; the ones you need currently
                   :all (constantly true)}

  :test2junit-output-dir "test-results"

  :main de.sveri.stockfighter.core

  :uberjar-name "stockfigherui.jar"

  :aliases {"rel-jar" ["do" "clean," "cljsbuild" "once" "adv," "uberjar"]
            "unit" ["do" "test" ":unit"]
            "integ" ["do" "test" ":integration"]})
