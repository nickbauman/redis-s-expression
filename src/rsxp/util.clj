(ns rsxp.util)

(defmacro is-thrown?
  "Returns boolean whether a particular Exception is thrown for an expression: 
  (is-thrown? Exception (= 1 1)) will return false, whereas 
  (is-thrown? Exception (throw (Exception.))) will return true"
  [& form]
  (let [klass (first form)
        body (second form)]
    `(try
       ~body
       false
       (catch ~klass e#
         true))))