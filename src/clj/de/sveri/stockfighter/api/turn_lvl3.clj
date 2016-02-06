(ns de.sveri.stockfighter.api.turn-lvl3
  (:require [de.sveri.stockfighter.api.turnbased :as turn]
            [de.sveri.stockfighter.service.helper :as h]
            [de.sveri.stockfighter.api.state :as state]
            [schema.core :as s]
            [com.rpl.specter :as spec]))



(s/defn ->avg-price [orderbooks ask-or-bid & [last]]
  (println (count orderbooks))
        (let [asks (spec/select [spec/ALL ask-or-bid spec/FIRST :price]
                                (subvec (into [] orderbooks) 0 (count orderbooks)))]
          (if (not-empty asks)
            (int (/ (reduce + asks) (count asks)))
            0))
  )

(defn get-buy-price []
  (let [{:keys [venue stock]} (h/->vsa)]
    (- (turn/get-bid-ask-mean (get @state/order-book (h/->unique-key venue stock)) 1) turn/price-spread)))

(defn entry [vsa]
  )
