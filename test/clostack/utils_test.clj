(ns clostack.utils-test
  (:require [clojure.test :refer :all]
            [clostack.utils :refer :all]))

(deftest dash-key-test
  (testing "dash key"
    (is (= (dash-key {:domain-id :val})
           {"domain_id" :val}))))
