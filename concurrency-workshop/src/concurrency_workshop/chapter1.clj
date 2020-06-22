(ns concurrency-workshop.chapter1)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Concurrency
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; A way to get safe access to limited resources by multiple
;; actors/threads

;; There are multiple paradigms models to do concurrency

;; Locks are one way of doing it

;; Clojure does an amazing job where there is immutable data by means of
;; persistent data structures provided by Clojure core

;; But what about mutating things?

;; There are four ways,

;; vars
;; refs
;; agents
;; atoms

;; But first a brief introduction to running things in different threads


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Future
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; This will run on a different thread
(comment
  (future 1)

  ;; This will block and wait till it's complete
  (deref (future 1))

  ;; A short hand for deref
  @(future 1)

  (def f (future (println "Running thread")
                 1))

  ;; Guess the output of second deref
  @f
  @f)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Promise
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Different futures can pass on values with futures as well.

(comment
  (def p (promise))

  ;; This will block and execute
  (future (Thread/sleep 5000) (deliver p 1))

  @p)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; vars
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def x 1)

(comment
  (def a 1)
  (future (println a))
  (future (println a)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Atoms
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Use it to represent Shared state

;; But first
;; Problems with shared state and why it's messy

;; int b = 1
;; new Thread(() -> b = 3
;;            Thread.sleep (new Random().nextInt(1000))
;;            System.out.println(b)).start();
;; new Thread(() -> b = 4
;;            Thread.sleep (new Random().nextInt(1000))
;;            System.out.println(b)).start();

;; https://aphyr.com/posts/306-clojure-from-the-ground-up-state

;; A counterpart in Clojure


(comment
  (do
    (def b 1)
    (future (def b 3)
            (Thread/sleep (rand-int 1000))
            (println b))

    (future (def b 4)
            (Thread/sleep (rand-int 1000))
            (println b)))
  (do
    (def b [])

    (doseq [n (range 2000)]
      (future (def b (conj b
                           n))))
    (println (count b))))


;; Let's slow things down to understand

(comment
  (do
    (def b [])

    (doseq [n (range 2000)]
      (future (def b (conj b
                           (do (when (= n 5)
                                 (println (count b))
                                 (Thread/sleep 100))
                               n)))))
    (Thread/sleep 100)
    (println (count b))))

;; We need transformation of state with stronger gaurantees


;; Variables mix State + Identitiy


;; A symbol in clojure is just an identity which points to a value / state

(def a 1)

;; It can point to an atom

(def a (atom 1))

;; Since atom's value can change we need to deref it to access it

(deref a)

;; A short hand to deref is @

@a

(swap! a (constantly 5))

;; @TODO add example of swap! and reset!

;; Let's try this with an atom

(comment
  (do
    (def b (atom []))
    (doseq [n (range 2000)]
      (future (swap! b conj n)))
    (println (count @b))))

(comment
  (do
    (def b (atom []))

    (doseq [n (range 2000)]
      (future (swap! b
                     conj
                     (do (when (= n 5)
                           (println (count @b))
                           (Thread/sleep 100))
                         n))))
    (Thread/sleep 100)
    (println (count @b))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; refs
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Refs ensure safe mutation of multiple shared states using STM

;; STM to me looks something like this

;; https://vimeo.com/106226560#t=10s

;; dosync is macro to start transaction

;; ref-set sets it to a value

;; alter runs a function on the value

;; Values held by refs must be immutable preferably clojure persistent
;; structures

(def a-ref-num (ref 0))
(def b-ref-num (ref 0))

(comment
  (doseq [n (range 10)]
    (future
      (dosync
       (println "Transaction - " n)
       (ref-set a-ref-num n)
       (ref-set b-ref-num n)))))

[@a-ref-num @b-ref-num]


(do
  (def a-ref (ref 0))
  (def b-ref (ref 0))
  (def a-atom (atom 0))
  (def b-atom (atom 0)))

(comment
  (do
    (doseq [n (range 100)]
      (future

        (dosync
         (ref-set a-ref n)
         (Thread/sleep (rand-int 200))
         (ref-set b-ref n))

        (reset! a-atom n)
        (Thread/sleep (rand-int 200))
        (reset! b-atom n)))

    (doseq [n (range 5)]
      (Thread/sleep 1000)
      (println (format "Ref values A - %s,  B - %s\nAtom values A - %s,  B - %s\n"
                       @a-ref
                       @b-ref
                       @a-atom
                       @b-atom)))))


(def a-alter (ref []))
(def b-alter (ref []))

(comment
  (doseq [n (range 10)]
    (future
      (dosync
       (println "Transaction - " n)
       (alter a-alter conj n)
       (Thread/sleep (rand-int 20))
       (alter b-alter conj n)))))

[@a-alter @b-alter]
