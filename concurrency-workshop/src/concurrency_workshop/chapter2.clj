(ns concurrency-workshop.chapter2
  (:require [concurrency-workshop.db.redis :as redis]
            [concurrency-workshop.db.mongo :as mongo]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Thundering herd
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;; Thundering herd

;; When many readers simultaneously request the same data element, there
;; can be a database read overload,
;; sometimes called the “Thundering Herd” problem.
;; https://www.ehcache.org/documentation/2.8/recipes/thunderingherd.html


;; Read from cache if not present read from database and update cache

(defn read+update-1
  [k]
  (if-let [v (redis/fetch k)]
    [v :redis]
    (do
      (let [v (mongo/fetch k)]
        (redis/set k v)
        [v :mongo]))))

(comment

  (do
    (reset! redis/r-db {})

    (doall
    (frequencies (map second (map (fn [_]
                                    (read+update-1 :foo))
                                  (range 10))))))



  ;; For 1000 concurrent requests there is a possibility of requests
  ;; going to MongoDB

  (do
    (reset! redis/r-db {})

    (def t-h-1
      (doall
       (map (fn [_]
              (future (read+update-1 :foo)))
            (range 1000))))

    ;; To analyze this data first we need to get the values from futures

    (frequencies (map second (map deref t-h-1)))))



;;; Let's introduce atom!

(def ongoing-updates-a (#_FIXME #{}))

(defn read+update-2
  [k]
  (if-let [v (redis/fetch :foo)]
    [v :redis]
    (let [update-ongoing? (#_FIXME ongoing-updates-a)]
      (if-not (update-ongoing? k)
        (do
          (#_FIXME ongoing-updates-a conj k)
          (let [v (mongo/fetch k)]
            (redis/set k v)
            (#_FIXME ongoing-updates-a disj k)
            [v :mongo]))
        [1 :default]))))

(comment

  (do
    (reset! redis/r-db {})

    (def t-h-1
      (doall
       (map (fn [_]
              (future (read+update-2 :foo)))
            (range 10000))))


    (frequencies (map second (map deref t-h-1)))))


;; Let's use ref instead

(def ongoing-updates-ref (#_FIXME #{}))

(defn read+update-3
  [k]
  (if-let [v (redis/fetch :foo)]
    [v :redis]
    (let [update-ongoing? (#_FIXME (when-not (get @ongoing-updates-ref k)
                                     (#_FIXME ongoing-updates-ref conj k)))]
      (if update-ongoing?
        (let [v (mongo/fetch k)]
          (redis/set k v)
          (#_FIXME (#_FIXME ongoing-updates-ref disj k))
          [v :mongo])
        [1 :default]))))



(comment

  (do
    (reset! redis/r-db {})

    (def t-h-2
      (doall
       (map (fn [_]
              (future (read+update-3 :foo)))
            (range 10000))))


    (frequencies (map second (map deref t-h-2)))))



;; But how do we fix the nils?

(def ongoing-updates-ref-p (ref #_FIXME))


(defn read+update-4
  [k]
  (if-let [v (redis/fetch :foo)]
    [v :redis]
    (let [[action p] (dosync (if-let [p (get @ongoing-updates-ref-p k)]
                               [:wait (get @ongoing-updates-ref-p k)]
                               (let [p #_FIXME]
                                 (alter ongoing-updates-ref-p #_FIXME k p)
                                 [:update p])))]
      (case action
        :update (let [v (mongo/fetch k)]
                  (redis/set k v)
                  (dosync (alter ongoing-updates-ref-p #_FIXME k))
                  (#_FIXME p v)
                  [v :mongo])
        :wait [(#_FIXME p) :redis]))))


(comment

  (do
    (reset! redis/r-db {})

    (def t-h-3
      (doall
       (map (fn [_]
              (future (read+update-4 :foo)))
            (range 10000))))


    (frequencies (map second (map deref t-h-3)))))
