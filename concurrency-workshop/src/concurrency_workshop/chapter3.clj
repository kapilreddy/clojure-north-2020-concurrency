(ns concurrency-workshop.chapter3
  (:require [concurrency-workshop.db.mongo :as mongo]
            [clj-time.core :as time]))



;; A circuit breaker is a resliency pattern

;; It protects your upstream dependencies if the errors thrown by them
;; are due to increased load

;; Intention here is to protect the datastore / dependency


;; Lets start simple


(defn req-handler
  [k]
  (try (mongo/fetch-1 k)
       (catch Exception e
         :failure)))

(comment

  ;; The calls have error rate of 20%
  ;; Goal here is avoid extra calls to Mongo

  (do
    (doall
     (frequencies (map deref (map (fn [_]
                                    (future (req-handler :foo)))
                                  (range 1000)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Strategy 1
;; Trip after 20 errors
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def failure-threshold-1 20)
  (def cb-atom-1 (atom 0))

  (defn req-handler-1
    [k]
    (try (if (#_FIXME)
           (mongo/fetch-1 k)
           :tripped)
         (catch Exception e
             (do (#_FIXME cb-atom-1 #_FIXME)
           (if (= (:cause (ex-data e)) :timeout-exception)
                 :failure)
             (do (println "Something wrong with the solution")
                 (throw e))))))


  (do
   (reset! cb-atom-2 0)
   (frequencies (map deref (map (fn [_]
                                  (future (req-handler-1 :foo)))
                                (range 10000)))))
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Strategy 2
;; Trip after 20 errors for 10 seconds
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def failure-threshold-2 20)
  (def tripped-timeout-msecs 200)
  (def cb-atom-2 (atom {:error-count 0
                      ;; :tripped-date-time nil
                      }))


  (defn cb-bump-fail-count
    [c-a]
    (swap! c-a (fn [a]
                 (if (> (get #_FIXME :error-count 0) failure-threshold-2)
                   (#_FIXME #_FIXME
                            :error-count 0
                            :tripped-date-time (time/now))
                   (#_FIXME #_FIXME :error-count #_FIXME)))))

  (defn tripped?
    [c-a]
    (when-let [dt (get #_FIXME :tripped-date-time)]
      (< (time/in-millis (time/interval dt
                                        (time/now)))
         tripped-timeout-msecs)))


  (defn req-handler-2
    [k]
    (try (if (tripped? cb-atom-2)
           :tripped
           (mongo/fetch-1 k))
         (catch Exception e
           (if (= (:cause (ex-data e*)) :timeout-exception)
             (do (cb-bump-fail-count cb-atom-2)
                 :failure)
             (do (println "Something wrong with the solution")
                 (throw e))))))

  (do
    (reset! cb-atom-2 {:error-count 0})
    (frequencies (map deref (map (fn [_]
                                   (Thread/sleep (rand-int 100))
                                   (future (req-handler-2 :foo)))
                                 (range 100)))))
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Strategy 3 - Homework
;;
;; Trip after 20 errors in last x seconds for 10 seconds
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment


  (defn req-handler-2
    [k]
    (try (if (tripped? cb-atom-2)
           :tripped
           (mongo/fetch-1 k))
         (catch Exception e
           (cb-bump-fail-count cb-atom-2)
           :failure)))

  (do

    ;; This should result in no trips
    ))
