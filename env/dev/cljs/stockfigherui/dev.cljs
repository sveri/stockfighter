(ns stockfigherui.dev
  (:require [schema.core :as s]
            [de.sveri.stockfighter.overview :as core]))

(s/set-fn-validation! false)

(enable-console-print!)

(defn main [] (core/main))
