(ns rsxp.serialization
    (:import
      (java.io StringReader StringWriter PrintWriter PushbackReader)))

(declare convert2expression)

(defn serialize
  "Returns the converted 'data-structure' into its canonical, homoiconic string representation"
  [data-structure]
  (let [w (StringWriter. 32)]
    (binding [*print-dup* true
              *out* w] 
      (prn (convert2expression data-structure))
      (str w))))

(defn deserialize
  "Converts 'string-representation' back into its native Clojure form"
  [string-representation]
  (read (PushbackReader. (StringReader. string-representation))))

(defn local2expression
  [my-obj]
  (reduce conj (map
                 convert2expression
                 (map second my-obj))
    (list (class my-obj) 'new)))

;; special types, work in progress
(def ^{:private true} type-conversion-map
  [[#"^rsxp\..*" local2expression]])

(defn- coll2expression
  "converts a collection by converting its elements"
  [coll]
  (let [empty-coll (if (#{clojure.lang.MapEntry
                          clojure.lang.PersistentTreeMap$BlackVal
                          clojure.lang.PersistentTreeMap$BlackBranchVal
                          clojure.lang.PersistentTreeMap$RedBranchVal
                          clojure.lang.PersistentTreeMap$RedVal} (type coll))
                     []
                     (empty coll))
        coll (if (list? empty-coll) (reverse coll) coll)]
    (reduce conj empty-coll (map convert2expression coll))))

(defn convert2expression
  "convert something to an s-expression"
  [obj]
  (if (nil? obj)
    nil
    (let [expression-fn (second (first (filter
                                         #(re-find (first %) (.getName (class obj)))
                                         type-conversion-map)))]
      (cond
        expression-fn (expression-fn obj)
        (coll? obj) (coll2expression obj)
        :else obj))))