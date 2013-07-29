(ns trident-clj.persister
  (:import [java.io FileReader]
           [java.util Map Map$Entry List ArrayList Collection Iterator HashMap])
  (:import [storm.trident.operation TridentCollector Function Filter]
           [storm.trident.tuple.TridentTuple]
           [backtype.storm.tuple Values])
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:require [clj-redis.client :as redis])     ; bring in redis namespace
  (:gen-class  ; gen-class method prefix by -
    :name com.colorcloud.trident.Persister  ; convert this ns to class Tweet
    :implements [storm.trident.operation.Function 
                 storm.trident.operation.Filter]))  ; this ns impl Function

; 
(defn -prepare
  "called once, better for init global var and db conn "
  [this conf context]
  (def redis-db (redis/init :url "redis://localhost")))  ; shall use dynamic binding

(defn -isKeep
  "predicate to whether keep the tuple"
  [^storm.trident.tuple.TridentTuple tuple]
  true)

(defn -execute  ;
  "process each tuple, persist to redis"
  [this ^storm.trident.tuple.TridentTuple tuple ^storm.trident.operation.TridentCollector collector]
  (let [loc (.getString tuple 0)]
    (prn "TweetAggregator : execute " loc)
    (redis/hset redis-db "location" loc)
    (redis/rpush "tweetloc" loc)
    (.emit collector (Values. loc))))