(ns concurrency-workshop.csp_assignment
  (:require [taoensso.nippy :as nippy]
            [concurrency-workshop.csp_util :as util]))


(comment

  ;; Imagine that there is a source / stream of data
  ;; like a kafka topic which is delivering weather related
  ;; data from different IoT sensors deployed in the wild.

  ;; This data is mainly of 3 types, i.e temperatures, humidity
  ;; and wind speed. And this data is coming from 4 locations,
  ;; the cities of Pune, Toronto, SanFrancisco and NewYork

  ;; This data is also protected by hmac signatures to provide
  ;; data security and is nippy serialised to reduce bandwidth usage
  ;; Another bandwidth reduction is that the keys in the map being sent
  ;; are compacted, for example, :s -> source, :l location etc

  ;; What we need to do is
  ;; 1. deserialise the data,
  ;; 2. make sure its authorized, meaning the hmac is valid
  ;; 3. transform the data to make it more readable by expanding the keys
  ;; 4. run some kind of validation, for example, wind-speeds cannot be
  ;;    negative, or humidity cannot be more than 100% etc
  ;; 5. Send it off to different kafka topics depending on the type

  ;; Have a look at the csp_util namespace

  (defn get-data []
    (try
      (let [data (nippy/freeze (util/make-data))]
        (when data
          data))
      (catch Throwable ex
        (println "Error: " ex)
        nil)))


  (-> (get-data)
      (util/deserialise)
      (util/authorize)
      (util/populate)
      (util/validate)
      (util/stream))


  ;; Assignment : Convert this synchronous pipeline of data
  ;; transformations into a parallely executing asynchronous
  ;; pipeline using core.async channels
  )
