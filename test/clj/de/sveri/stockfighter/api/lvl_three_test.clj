(ns de.sveri.stockfighter.api.lvl-three-test
  (:require [clojure.test :refer :all]
            [de.sveri.stockfighter.api.lvl-three :as three]))

(def test-booking (atom {:nav 0 :position 0 :cash 0}))

(defn clean-atom [f] (f) (reset! test-booking {:nav 0 :position 0 :cash 0}))

(use-fixtures :each clean-atom)

(deftest ^:cur booking-buy
  (three/update-booking
    {:price 1 :direction "buy" :qty 50
     :fills [{:price 1 :qty 50}]}
    test-booking)
  (is (= {:nav 0 :position 50 :cash -50} @test-booking)))

(deftest ^:cur booking-2-buy
  (three/update-booking {:price 1 :direction "buy" :qty 50
                         :fills [{:price 1 :qty 50}]}
                        test-booking)
  (three/update-booking {:price 1 :direction "buy" :qty 60
                         :fills [{:price 1 :qty 50}
                                 {:price 1 :qty 10}]}
                        test-booking)
  (is (= {:nav 0 :position 110 :cash -110} @test-booking)))


(deftest ^:cur booking-sell
  (three/update-booking
    {:price 1 :direction "sell" :qty 50
     :fills [{:price 1 :qty 50}]}
    test-booking)
  (is (= {:nav 0 :position -50 :cash 50} @test-booking)))

(deftest ^:cur booking-two-sell
  (three/update-booking {:price 1 :direction "sell" :qty 50
                         :fills [{:price 2 :qty 50}]}
                        test-booking)
  (three/update-booking {:price 1 :direction "sell" :qty 60
                         :fills [{:price 2 :qty 50}
                                 {:price 2 :qty 10}]}
                        test-booking)
  (is (= {:nav 0 :position -110 :cash 220} @test-booking)))


(deftest ^:cur booking-buy-sell
  (three/update-booking
    {:price 1 :direction "sell" :qty 50
     :fills [{:price 1 :qty 50}]}
    test-booking)
  (three/update-booking
    {:price 1 :direction "buy" :qty 50
     :fills [{:price 2 :qty 50}]}
    test-booking))
(is (= {:nav 0 :position 0 :cash -50} @test-booking))

(deftest ^:cur booking-two-buy-sell
  (three/update-booking
    {:price 1 :direction "sell" :qty 50
     :fills [{:price 1 :qty 50}
             {:price 2 :qty 10}]}
    test-booking)
  (three/update-booking
    {:price 1 :direction "buy" :qty 50
     :fills [{:price 2 :qty 50}
             {:price 1 :qty 50}]}
    test-booking))
(is (= {:nav 0 :position 40 :cash -80} @test-booking))