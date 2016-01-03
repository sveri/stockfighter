(ns de.sveri.stockfighter.helper
  (:require [cljs.core.async :refer [chan close! put!]]
            [goog.events :as events]
            [goog.fx :as fx]
            [goog.fx.dom :as fx-dom]
            [goog.dom :as gdom]
            [goog.dom.forms :as gforms]
            [goog.net.XhrIo :as xhr]
            [cljs.pprint :as pp]
    ;[ajax.core :as ajax]
            )
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn format-number [n]
  (pp/cl-format nil "~,2f" n))

(defn bool->string [b]
  (if b "true" "false"))

(defn wrap-with-form [label-name form-field]
  [:div
   [:div.form-group
    [:label label-name]
    form-field]])

(defn error-handler [e] (println "Some Error Occured: " e))
(defn success-handler [e] (println "It worked: " e))

(defn update-local-textfield [state e keypath & [extra-map]]
  (if (= "number" (get extra-map :type ""))
    (swap! state assoc-in keypath (js/parseInt (-> e .-target .-value)))
    (swap! state assoc-in keypath (-> e .-target .-value))))

(defn ->local-text-field [state label keypath & [extra-map]]
  (let [val (get-in @state keypath)]
    (wrap-with-form
      label
      [:input.form-control
       (merge extra-map {:on-change   #(update-local-textfield state % keypath extra-map)
                         :placeholder label :value val})])))


(defn get-value [elem]
  (gforms/getValue (gdom/getElement elem)))

(defn get-elem [id] (gdom/getElement id))


(defn fade-out
  ([] (fade-out 1000 nil))
  ([tm] (fade-out tm nil))
  ([tm callback]
   (fn [node]
     (let [anim (fx-dom/FadeOut. node tm)]
       (when callback
         (events/listen anim js/goog.fx.Animation.EventType.END callback))
       (. anim (play))))))

(defn fade-in
  ([] (fade-in 1000 nil))
  ([tm] (fade-in tm nil))
  ([tm callback]
   (fn [node]
     (let [anim (fx-dom/FadeIn. node tm)]
       (when callback
         (events/listen anim js/goog.fx.Animation.EventType.END callback))
       (. anim (play))))))
