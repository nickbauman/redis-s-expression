(ns rsxp.dal
  ^{:doc "Data Access Layer over Redis"}
  (:use [rsxp.serialization :only [serialize deserialize]])
  (:require [redis.core :as redis])
  (:import [java.io StringReader PushbackReader StringWriter]))

(def ^{:private true} cx {:cx "127.0.0.1" :port 6379 :db 0})

(declare db-save)

(defn db-value-change
  ^{:private true
    :doc "Remove an entry that has an incompatible value type, if necessary."}
  [akey type-kw]
  (redis/with-server cx
                     (if (and (redis/exists akey) 
                              (not (= type-kw (redis/type akey))))
                         (redis/del akey))))

; Writers
(defn db-save-list
  ^{:private true
    :doc "Save a list. Adds type hints to the values, if neccesary."}
  [akey a-list]
  (redis/with-server cx
                     (do
                       (db-value-change akey :list)
                       (doseq [elem a-list] 
                         (redis/rpush akey (serialize elem)))
                       "OK")))
(defn db-save-set
  ^{:private true
    :doc "Save a set. Adds type hints to the values, if necessary"}
  [akey a-set]
  (redis/with-server cx
                     (do
                       (db-value-change akey :set)
                       (doseq [value a-set] 
                         (redis/sadd akey (serialize value)))
                       "OK")))

(defn db-save-map
  ^{:private true
    :doc "Save a map. Adds type hints to the values, if necessary"}
  [akey a-map]
  (let [akeys (keys a-map)]
    (redis/with-server cx
                       (do
                         (db-value-change akey :map)
                         (let [the-keys (map serialize (keys a-map))
                               the-values (map serialize (vals a-map))
                               entries (interleave the-keys the-values)]
                          (apply redis/hmset akey entries))
                         "OK"))))

;; Readers
(defn db-read-list
  ^{:private true
    :doc "Reads a list from the store. Converts stored representation if items in list into 
          native types as needed."}
  [akey]
  (redis/with-server cx
                     (let [size (redis/llen akey)
                           rang (range size)
                           stored-representation (redis/lrange akey (first rang) (last rang))]
                      (into () (reverse (map deserialize stored-representation))))))

(defn db-read-set
  ^{:private
    :doc "Reads a set from the store. Converts stored representation if items in set into 
          native types as needed."}
  [akey]
  (redis/with-server cx
                     (into #{} (map deserialize (redis/smembers akey)))))

(defn db-read-map
  ^{:private true
    :doc "Reads a map from the store. Converts stored representation if items in map into 
          native types as needed."}
  [akey]
  (redis/with-server cx
                     (let [the-keys (map deserialize (redis/hkeys akey))
                           the-values (map deserialize (redis/hvals akey))]
                         (into {} (into [] (map #(into [] %) (partition 2 (interleave the-keys the-values))))))))

(defn db-val-type
  ^{:private true 
    :doc "Retrieves the type (which is one of :map :string :list :none) of what is stored under 'akey'"} 
  [akey]
  (redis/with-server cx
                     (redis/type akey)))

; Public interface

(defn db-ping
  "Pings the database instance. May throw an exception if the server is down or having problems. May, in rare cases, hang."
  []
  (redis/with-server cx
                     (redis/ping)))

(defn db-flush
  "empties the entire database!"
  []
  (redis/with-server cx
                     (redis/flushall)))

(defn db-del
  "Deletes an entry in the store."
  [akey]
  (redis/with-server cx
                     (redis/del akey)))

(defn db-exists
  "Returns boolean whether a value exists under 'akey'"
  [akey]
  (redis/with-server cx
                     (redis/exists akey)))

(defn db-read
  "Reads item in store, returns value of any type attempting to use any typehints 
  stored with each item, converting into native types, as needed."
  [akey]
  (redis/with-server cx
                     (condp = (redis/type akey)
                       :string ; could be any one of string, integer or float
                       (deserialize (redis/get akey))
                       
                       :list
                       (db-read-list akey)
                       
                       :set
                       (db-read-set akey)
                       
                       :hash
                       (db-read-map akey)
                       
                       :none
                       nil
                       
                       :else
                       (throw (RuntimeException. (str "unsupported key type: " (redis/type akey)))))))

(defn db-find
  "Returns values from the datastore that the function returns logical truth for"
  [func]
  (redis/with-server cx
    (filter func (map db-read (redis/keys "*")))))

(defn db-save
  [akey value]
  "Writes item in store, stores representation of the native value, using typehints 
  stored with each item, as needed."
  (redis/with-server cx
                     (cond

                       (or (string? value) (integer? value) (float? value) (keyword? value))
                       (redis/set akey (serialize value))
                       
                       (or (list? value) (vector? value))
                       (db-save-list akey value)
                       
                       (set? value)
                       (db-save-set akey value)
                       
                       (map? value)
                       (db-save-map akey value)
                       
                       :else
                       (throw (RuntimeException. (str "unsupported value type: " (type value)))))))
