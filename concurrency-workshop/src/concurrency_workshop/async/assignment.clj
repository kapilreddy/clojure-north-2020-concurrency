(ns concurrency-workshop.async.assignment
  (:require [taoensso.nippy :as nippy]
            [concurrency-workshop.async.util :as util]))


(defn get-data []
  (try
    (let [data (nippy/freeze (util/make-data))]
      (when data
        data))
    (catch Throwable ex
      (println "Error: " ex)
      nil)))


(comment
  (-> (get-data)
      (util/deserialise)
      (util/authorize)
      (util/populate)
      (util/validate)
      (util/stream))
  )
