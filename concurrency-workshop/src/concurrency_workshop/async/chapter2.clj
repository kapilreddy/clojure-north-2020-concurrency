(ns concurrency-workshop.async.chapter2
  (:require [clojure.core.async :as async]
            [clojure.core.async.impl.protocols :as impl])
  (:import [java.util LinkedList]))

;; this is about go blocks and buffers.
;; also write a custom buffer for debugging / metrics

(comment
  ;; Threads are great and all but what if we want something
  ;; more lightweight. Something which doesnt hold up a dedicated
  ;; thread but simulates async + callback style behavior
  ;; This is exactly what go blocks are. callbacks and statemachines
  ;; which can operate in conjunction with channels

  (def achan (async/chan))
  (async/go
    (async/>! achan :from-go)
    (println "Wrote on the channel from go block"))

  (async/go
    (println "From the go block : " (async/<! achan)))

  ;; Go blocks are also referred to as IOC threads
  ;; The reason being that go blocks are actually a kind of deep
  ;; walking macro which examine any channel operations inside it
  ;; and whenever the channel operation is encountered, this execution
  ;; is parked and registered as a callback on the channel.
  ;; Once the channel operation succeeds, rest of the block is resumed for
  ;; execution on a thread pool.

  ;; This inversion of control is crucial for CLJS
  ;; to provide an illusion of threads.

  ;; Go blocks also return channels !

  ;; Question : Can you guess what would be returned by the channel
  ;; and when ?

  (async/go
    (println "From the go block : " (async/<! achan)))

  (assert (= FIXME (async/<!! (async/go
                                (async/>! achan :from-go)
                                (println "Wrote to the channel")
                                42))))


  ;; You may have noticed that we have used >! and <! in the go blocks
  ;; These are known as parking channel operations. They help the go macro
  ;; implement IOC without blocking the thread underneath.

  ;; for normal threads use >!! and <!!
  ;; for go blocks use >! and <!
  ;; core.async uses similar conventions for all channel operations
  ;; which have a parking equivalent


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  ;; Some gotchas while using go blocks

  ;; Go macros stop translating at function boundaries
  ;; So the below code will throw an error to say >! was used outside
  ;; of a go block

  (defn write-in-go [chan]
    (async/>! chan 42))

  (async/go (write-in-go (async/chan)))

  ;; Go blocks are not suitable if you have a large number of
  ;; blocking IO calls. This is because Go blocks are backed by
  ;; by a fixed size thread pool which will quickly be exhausted
  ;; by blocks waiting for IO to finish

  ;; So instead of doing this
  (async/go
    (async/>! output (blocking-http-call url)))


  ;; Do this

  (async/go
    (http-call-with-callback url (fn [data]
                                   (async/>! output data))))

  ;; And so it follows that you should not use
  ;; Thread/sleep in go blocks. Prefer using async/timeout instead

  ;; Go blocks and GC
  ;; A channel is puts + buffer + takes

  ;; puts -> [callbacks values]
  ;; buffer -> queue of values
  ;; takes -> queue of callbacks

  ;; there can only be pending takes or pending puts, not both

  ;; go blocks -> set of callbacks + local state + return channel
  ;; channels -> callbacks -> local state -> return channel

  ;; go blocks are either attached to a channel or in a thread-pool
  ;; So for example,

  (async/go
    (let [v (async/<! achan)
          v2 (inc v)]
      (async/>! anotherChan)))

  ;; here the go block and its local state is held by achan
  ;; if achan is GC'd, the attached go block and local state is also GC'd


  (def foo (async/go
             (let [c (async/chan)]
               (async/>! c 42))))

  ;; Here since no one holds a reference to c, it gets GC'd
  ;; along with the callback i.e the go block and the local state
  ;; The only thing that remains is a dangling return channel
  ;; held by foo


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  ;; Buffers
  ;; As previously discussed, channels can have buffers.
  ;; But even these buffers will blocks writers once the capacity
  ;; is reached.
  ;; But what if my use-case is that I care only about the most
  ;; recent data ? Or what if I want writers to not block but dropping new
  ;; new data is okay ?
  ;; core.async has a couple of solutions for us here

  (def slider (async/chan (async/sliding-buffer 10)))
  (for [v (range 20)]
    (async/>!! slider v))

  (assert (= 10 (async/<!! slider)))


  (def dropper (async/chan (async/dropping-buffer 10)))
  (for [v (range 40)]
    (async/>!! dropper v))

  (assert (= 0 (async/<!! dropper)))


  ;; But wait, there's more! What if you want to look at the
  ;; value on a channel but not take it out, or if you want to add
  ;; some metrics on average channel capacity etc ?
  ;; Well ofcourse you can define your own custom buffers too !

  ;; This is the implementation of a standard fixed buffer found
  ;; in core.async.impl.buffers

  #_(deftype FixedBuffer [^LinkedList buf ^long n]
    impl/Buffer
    (full? [this]
      (>= (.size buf) n))
    (remove! [this]
      (.removeLast buf))
    (add!* [this itm]
      (.addFirst buf itm)
      this)
    (close-buf! [this])
    clojure.lang.Counted
    (count [this]
      (.size buf)))

  ;; So lets say we want to implement a Transparent buffer with an inspect
  ;; method

  (defprotocol TransparentBuffer
    (inspect [this]))


  (deftype TransparentFixedBuffer [^LinkedList buf ^long n]
    impl/Buffer
    (full? [this]
      (= (.size buf) n))
    (remove! [this]
      (.removeLast buf))
    (add!* [this itm]
      (assert (not (impl/full? this)) "Can't add to a full buffer")
      (.addFirst buf itm))
    clojure.lang.Counted
    (count [this]
      (.size buf))

    TransparentBuffer
    (inspect [this]
      (seq buf)))


  (defn transparent-buffer [^long n]
    (TransparentFixedBuffer. (LinkedList.) n))

  ;; Now lets use this buffer
  ;; Question : Can you create a channel which can be
  ;; inspect'd ?

  (def transparent-chan (async/chan FIXME))
  (for [v (range 10)]
    (async/>!! transparent-chan v))

  (println "Data on the channel " (inspect FIXME))

  )
