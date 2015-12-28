(ns de.sveri.stockfighter.executions)

(defn exec-page [state]
  [:table.table
   [:thead [:tr [:td "Bids Avg"] [:td "Asks Avg"] [:td "Spread"]]]
   [:tbody [:tr
            [:td (get-in @state [:executions :bids-avg])]
            [:td (get-in @state [:executions :asks-avg])]
            [:td (get-in @state [:executions :spread])]]]])
