(ns de.sveri.stockfighter.gui.core
  (:require [seesaw.core :refer :all]))


(def f (frame :title "Go Go Go"
              :content "iatern"))

(defn create-view []
  (-> f pack! show!))

(create-view)

(config! f :content ",")
