(ns rsxp.dal-test
  (:use rsxp.dal
        rsxp.util
        rsxp.protocols
        clojure.test)
  (:import [java.net SocketException]))

(defn redis-setup-and-teardown 
  "Takes each test and flushes the database before it runs"
  [f]
  (if (is-thrown? SocketException (db-ping))
    (println "!!! REDIS database is DOWN. Skipping these tests !!!!")
    (do 
      (db-flush)
      (f))))

(use-fixtures :each redis-setup-and-teardown)

(deftest test-save-scalars
  (testing "string"
           (is (= "OK" (db-save "foo" "bar")))
           (is (= :string (db-val-type "foo")))
           (is (= "bar" (db-read "foo"))))
  (testing "integer"
           (db-save "foo" 1)
           (is (= 1 (db-read "foo"))))
  (testing "float"
           (db-save "foo" 1.1)
           (is (= 1.1 (db-read "foo"))))
  (testing "keyword"
           (db-save :foo :bar)
           (is (= :bar (db-read :foo)))))

(deftest test-save-lists
  (testing "string"
           (is (= "OK" (db-save "foo" '("bar" "baz" "blrfl" "quux"))))
           (is (= '("bar" "baz" "blrfl" "quux") (db-read "foo")))
           (is (list? (db-read "foo"))))
  (testing "integer"
           (db-del "foo")
           (is (not (db-exists "foo")))
           (is (= "OK" (db-save "foo" [1 2 3 4])))
                    (is (= [1 2 3 4] (db-read "foo"))))
  (testing "float"
           (db-del "foo")
           (is (not (db-exists "foo")))
           (is (= "OK" (db-save "foo" [1.2 3.4])))
           (is (= [1.2 3.4] (db-read "foo")))
           (is (list? (db-read "foo"))))
  (testing "keyword"
           (db-save :foo '(:loo :fosos))
           (is (= '(:loo :fosos)) (db-read :foo)))
  (testing "mixture"
           (db-del "foo")
           (is (not (db-exists "foo")))
           (is (= "OK" (db-save "foo" ["bar" 3.4 "baz" 1 :goo])))
           (is (= ["bar" 3.4 "baz" 1 :goo] (db-read "foo")))
           (is (list? (db-read "foo")))))

(deftest test-save-sets
  (testing "string"
           (is (= "OK" (db-save "foo" #{"bar" "baz" "blrfl" "quux"})))
           (is (= #{"bar" "baz" "blrfl" "quux"} (db-read "foo"))))
  (testing "integer"
           (db-del "foo")
           (is (not (db-exists "foo")))
           (is (= "OK" (db-save "foo" #{1 2 3 4})))
           (is (= #{1 2 3 4} (db-read "foo"))))
  (testing "float"
           (db-del "foo")
           (is (not (db-exists "foo")))
           (is (= "OK" (db-save "foo" #{1.2 3.4})))
           (is (= #{1.2 3.4} (db-read "foo"))))
  (testing "mixture"
           (db-del "foo")
           (is (not (db-exists "foo")))
           (is (= "OK" (db-save "foo" #{"cat" 3.4 1 "seven"})))
           (is (= #{"cat" 3.4 1 "seven"} (db-read "foo")))))

(deftest test-save-maps
  (testing "string"
           (is (= "OK" (db-save "foo" {"bar" "baz" "blrfl" "quux"})))
           (is (= {"bar" "baz" "blrfl" "quux"} (db-read "foo"))))
  (testing "integer"
           (db-del "foo")
           (is (not (db-exists "foo")))
           (is (= "OK" (db-save "foo" {1 2 3 4})))
           (is (= {1 2 3 4} (db-read "foo"))))
  (testing "float"
           (db-del "foo")
           (is (not (db-exists "foo")))
           (is (= "OK" (db-save "foo" {1.2 3.4})))
           (is (= {1.2 3.4} (db-read "foo"))))
  (testing "mixture"
           (db-del "foo")
           (is (not (db-exists "foo")))
           (is (= "OK" (db-save "foo" {:fifty "seven" 1 3.4})))
           (is (= {:fifty "seven" 1 3.4} (db-read "foo")))))

(deftest test-save-hybrid-datastructures
  (testing "A map of maps and lists"
           (is (= "OK" (db-save "foo" {"fifty" {:something 1 :anotherthing 2} "sixty" '(1 2 3 "foo")})))
           (is (= {"fifty" {:something 1 :anotherthing 2} "sixty" '(1 2 3 "foo")} (db-read "foo"))))
  
  
  (testing "A map whose keys are integers, whose values are strings, vectors, lists, and those lists are comprised of strings and maps"
           (is (= "OK" (db-save "foo" {1 [1 2 3 4 5] 2 '("bar" {:la 1 :te 2 :da 3} "foo")})))
           (is (= {1 [1 2 3 4 5] 2 '("bar" {:la 1 :te 2 :da 3} "foo")} (db-read "foo"))))
  (testing "Make sure symbols are converted into correct primitives"
           (let [a 1
                 b 2]
             (is (= "OK" (db-save "foo" {a b}))))
           ; a and b are no longer around
           (is (= {1 2} (db-read "foo")))))

(deftest test-find
           (is (empty? (db-find (fn[value] (if (map? value) (some #(= "fifty" %) (keys value)))))))
           (is (= "OK" (db-save "foo" {"fifty" {:something 1 :anotherthing 2} "sixty" '(1 2 3 "foo")})))
           (is (not (empty? (db-find (fn[value] (if (map? value) (some #(= "fifty" %) (keys value)))))))))

(deftest test-protocols
  (let [nick (rsxp.protocols.GoofyPerson. "Nikilek" "Gebagobi")]
    (is (empty? (db-find (fn[value] (if (map? value) (some #(= :fname %) (keys value)))))))
    (is (= "OK" (db-save (type nick) nick)))
    (let [read-result (db-find (fn[value] (if (map? value) (some #(= :fname %) (keys value)))))]
      (is (not (empty? read-result)))
      (is (= rsxp.protocols.GoofyPerson  (type (first read-result)))))))

(defn bench
  "Call this to bench the server"
  []
  (println "number of inserts / reads:")
  (time (loop [tries (range 1 5001)
               c 0]
          (if-let [tri (first tries)]
            (let [data {(rand tri) [(rand tri) 3 4 5] (inc tri) ["bar" {:la 1 :te 2 :da (rand tri)} "foo"]}]
              (db-save tri data )
              (is (= data (db-read tri)))
              (db-del tri)
              (recur (rest tries) tri))
            c))))