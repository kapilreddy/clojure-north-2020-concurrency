(ns concurrency-workshop.async.chapter1
  ;; Let's start by requiring the appropriate namespace
  (:require [clojure.core.async :as async]))

(comment
  ;; The basics

  ;; A channel is essentially a buffer with
  ;; magic synchronisation properties

  (def achan (async/chan)) ;; this is a single item channel


  ;; lets try and put something on the channel
  ;; Here the port refers to the channel
  ;; Why the ! you ask ? Lets find out

  (async/put! achan 42 #(println "Wrote on the channel : " %))

  ;; Does this execute ?
  (println "Done putting on the channel")


  ;; Yes it does. Because put! is an async operation.
  ;; It puts a value on the channel and returns true
  ;; if the channel was not closed! false otherwise

  ;; Now what has happened to the value we just put
  ;; in the channel ?

  (async/take! achan #(println "Here is the value " %))

  ;; Is there anything else on the channel ?

  (async/take! achan #(println "Another value " %))

  ;; Clearly there is nothing on the channel right now.
  ;; But then what about the callback ?
  ;; Let's signal

  ;; Question : Can you make the above take's callback print a value

  (async/put! achan :done)

  ;; But this doesnt look like too much fun. Where is the
  ;; sychronisation ?

  ;; For synchronisation of puts and takes on a channel, there are a
  ;; couple of other variants.

  ;; Put a value on a channel. !! indicates that this is blocking
  (future
    (async/>!! achan :sync)
    ;; Do you think this gets printed?
    (println "Done putting value on the channel"))

  ;; Take a value from a channel. !! indicates blocking
  (println "Value from the channel is " (async/<!! achan))

  ;; So in this way a simple (chan) can we used as a synchronisation
  ;; point between 2 or more threads of execution

  ;; Now, we just discussed that (async/chan) creates a single
  ;; buffer channel. Then what does this create ?

  ;; Question : Change the code below to pass the assertion
  (assert (= :yes (let [one-chan (async/chan 1)]
                     (async/>!! one-chan :test)
                     :yes)))

  ;; Interesting ! So a (chan 1) seems to be different
  ;; than a (chan). But how is that useful ?
  ;; If you don't want total lock step synchronisation, a (chan 1)
  ;; is a good option

  ;; So then can I specify a higher number ? Sure you can

  (def buffer-chan (async/chan 20))
  (future
    (doall
     (map #(async/>!! buffer-chan %) (range 20)))
    (async/>!! buffer-chan :last)
    (println "I was able to put the 21st value!"))

  ;; As you can see, only 20 puts were successful
  ;; Question : Can you make the 21st put succeed ?

  (async/<!! buffer-chan)

  ;; Ok, so now we know that run async operations
  ;; and wait for results via channels. But our code is
  ;; always more complicated right ?
  ;; My app actually shoots off 4 operations but I am okay with
  ;; proceeding after any one completes

  ;; lets say we have an operation which
  ;; runs something on a thread and writes the value
  ;; to the out-channel
  (defn run-long-operation
    [f out-chan]
    (future
      (async/>!! out-chan (f))))

  (let [db-chan (async/chan)
        file-chan (async/chan)
        http-chan (async/chan)]
    (run-long-operation (fn []
                          (Thread/sleep (rand-int 500))
                          :file) file-chan)
    (run-long-operation (fn []
                          (Thread/sleep (rand-int 500))
                          :db) db-chan)
    (run-long-operation (fn []
                          (Thread/sleep (rand-int 500))
                          :http) http-chan)
    (let [[val port] (async/alts!! [db-chan file-chan http-chan])]
      (println "Operation which completed first returned : " val)))


  ;; But now my requirements have changed, I cannot wait
  ;; for more than 300 ms, otherwise I want to take some
  ;; other action

  ;; core.async provides the timeout fn for exactly this kind of use-case
  ;; it returns a channel which becomes available to read after specified
  ;; amount of time
  (println "This will wait 1 second and print " (async/<!! (async/timeout 1000)))

  ;; so it returned nil? This is because the channel is actually closed
  ;; after the timeout msecs expire and reads from closed channels
  ;; return instantly

  (def closed-chan (async/chan))
  (async/close! closed-chan)
  (println "Closed channel says what ? " (async/<!! closed-chan))

  ;; But what about writes ?
  (println "Writing to a closed channel done!" (async/>!! closed-chan :hi))

  ;; Question : Can you modify the code below to satisfy the changed
  ;; requirements in running operations ?

  (let [db-chan (async/chan)
        file-chan (async/chan)
        http-chan (async/chan)
        quit-chan (async/timeout 300)]
    (run-long-operation (fn []
                          (Thread/sleep (rand-int 500))
                          :file) file-chan)
    (run-long-operation (fn []
                          (Thread/sleep (rand-int 500))
                          :db) db-chan)
    (run-long-operation (fn []
                          (Thread/sleep (rand-int 500))
                          :http) http-chan)
    (let [[val port] (async/alts!! [db-chan file-chan http-chan quit-chan])]
      (println "Operation which completed first returned : " val)))

  ;; But alts!! are more intelligent too. They can essentially
  ;; choose between a set of read-write operations to channels

  ;; alts!! can also support options for default value and priority
  ;; if default is uncommented, we always get no-op

  (let [read-chan (async/chan)
        write-chan (async/chan)]
    (run-long-operation (fn []
                          (Thread/sleep (rand-int 200))
                          :read) read-chan)
    (future
      (Thread/sleep (rand-int 200))
      (async/<!! write-chan)
      :write)

    (let [[val port] (async/alts!! [read-chan
                                    [write-chan :write]]
                                   ;;:default :no-op
                                   ;;:priority true
                                   )]
      (if (identical? write-chan port)
        (println "We wrote a value to " port)
        (println "We read a value : " val))))

  ;; The way run-long-operation is written can
  ;; be improved!

  ;; Question : Can you rewrite long operation to return
  ;; the channel on which we can get the result?

  (defn channel-operation
    [f]
    (let [c (async/chan)]
      (future (async/>!! c (f)))
      c))

  (async/<!! (channel-operation (fn []
                                  42)))

  ;; This is exactly what async/thread provides!
  ;; A channel on which value of body is returned.
  ;; Body runs on a separate thread from a pool
  (async/thread 42)

  (async/<!! (async/thread 42))

  )
