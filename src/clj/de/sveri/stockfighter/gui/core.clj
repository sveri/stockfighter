(ns de.sveri.stockfighter.gui.core
  (:require [seesaw.core :refer :all]
            [live-chart :as c]
            [de.sveri.stockfighter.api.state :as state]
            [de.sveri.stockfighter.service.helper :as h]))


(defn rand1 [] (rand 1))


(c/show (c/time-chart [state/best-quote-ask state/best-quote-bid] :repaint-speed 2000) :title "test random funcs")

;(def f (frame :title "Go Go Go"
;              :content "iatern"))
;
;(defn create-view []
;  (-> f pack! show!))
;
;(create-view)
;
;(config! f :content "dtaretrae,")
