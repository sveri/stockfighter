(ns de.sveri.stockfighter.executions)

(defn exec-page [state]
  [:table.table
   [:thead [:tr [:td "Executed"] [:td "Avg Bid"]]]
   [:tbody [:tr
            [:td (get-in @state [:executions :total-filled])]
            [:td (get-in @state [:executions :filled-avg])]]]])
