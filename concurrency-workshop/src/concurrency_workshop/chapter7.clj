(ns concurrency-workshop.chapter7
  (:require [clojure.core.async :as async]
            [clj-time.core :as cc]))


(comment

  ;; Design patterns with core.async

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Generators : functions which return channels

  ;; Lets say you have a function which fetches
  ;; some data over the network.

  (defn fetch-from [url]
    (Thread/sleep (rand-int 1000))
    {:url url :status 200 :time (cc/now)})

  ;; Now you want to turn that
  ;; into a stream of data. One way would be to hand
  ;; the fetch function a channel on which to write
  ;; the fetched data

  (defn fetch [url ch]
    (future
      (dorun
       (for [_ (range 5)]
         (FIXME ch (fetch-from url))))))


  ;; Lets run a test
  (let [out (async/chan)]
    (fetch "https://clojure.github.io/core.async/#clojure.core.async/pub" out)
    (loop []
      (when-some [d (async/<!! out)]
        (println "Got data from server : " d)
        (recur)))
    (println "End of stream"))


  ;; But now you see your loop is stuck because no-one closed the channel
  ;; The fn which knows when to stop doesnt own the channel so it cant call
  ;; close on it.

  ;; Instead a better pattern is a function which returns
  ;; a channel on which data will be available

  (defn fetch< [url]
    (let [out (FIXME)]
      (future
        (dorun
         (for [_ (range 5)]
           ;; Fetch data a write it to the out channel
           (async/>!! out (FIXME))))
        (println "Closing the channel")
        (async/close! out))
      out))

  (let [out (fetch< "https://clojure.github.io/core.async/#clojure.core.async/pub")]
    (loop []
      (when-some [d (async/<!! out)]
        (println "Got data from server : " d)
        (recur)))
    (println "End of stream"))


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; fan-in : merge channels

  ;; Now lets say you want to send off a bunch fetches to multiple
  ;; http URLs in parallel and combine the results into 1 stream

  #_(fetch-from "https://duckduckgo.com/?q=core.async")
  #_(fetch-from "https://duckduckgo.com/?q=clojure+refs")
  #_(fetch-from "https://duckduckgo.com/?q=clojure+atoms")

  ;; Using the pattern above we could do something like
  ;; and this works !
  (async/<!! (fetch< "https://duckduckgo.com/?q=core.async"))

  ;; So now we can have multiple channels and we want to
  ;; keep reading from any one which is available.
  ;; Sounds like a good use for alts!!

  (let [c1 (fetch< "https://duckduckgo.com/?q=core.async")
        c2 (fetch< "https://duckduckgo.com/?q=clojure+refs")
        c3 (fetch< "https://duckduckgo.com/?q=clojure+atoms")]
    (loop []
      (let [[val _] (async/alts!! [FIXME])]
        (when val
          (println "Got data from : " (:url val))
          (recur))))
    (println "Done"))

  ;; But now you see that you are able to wait only for 1 channel to close
  ;; alts will always select the closed channel and nil as the value.

  ;; So a better fan-in pattern is to use the async/merge function
  ;; we saw earlier

  (let [c1 (fetch< "https://duckduckgo.com/?q=core.async")
        c2 (fetch< "https://duckduckgo.com/?q=clojure+refs")
        c3 (fetch< "https://duckduckgo.com/?q=clojure+atoms")
        merged (FIXME)]
    (loop []
      (when-some [val (async/<!! merged)]
        (println "Got data from : " (:url val))
        (recur)))
    (println "Done"))


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; fan-out
  ;; Imagine that you have a set of processor threads to which you
  ;; want to pass on messages one by one. So for every message
  ;; that comes in, you want to see which processor is idle and
  ;; pass on the message to that thread.
  ;; We can use channels for doing to message passing / distribution
  ;; here with a simple fan-out pattern.
  ;; As we saw earlier, alts!! and its variants also support writing
  ;; to any one of the available channels.

  (defn process [in pid]
    (future
      (loop []
        (when-some [m (async/<!! in)]
          (println (format "Received message %s on processor %s" m pid))
          (Thread/sleep (rand-int 400))
          (recur)))))


  (def c (async/chan))
  (process c 1)
  (async/>!! c "Message 42")

  ;; Now lets create a system which has 1 input channel
  ;; and 3 processors which can process any message coming in

  (def input (async/chan))

  (let [c1 (async/chan)
        c2 (async/chan)
        c3 (async/chan)]
    ;; Lets start our processor loops
    (process c1 :first)
    (process c2 :second)
    (process c3 :third)

    ;; Now we have to read from the input channel
    ;; and write to any channel which is available
    (async/go-loop []
      (let [m (async/<! input)]
        (async/alts! [[c1 m] [c2 m] [c3 m]])
        (recur))))

  ;; Now lets send some messages
  (future
    (loop [i 0]
      (when (< i 10)
        (async/>!! input {:job i :msg "New message to process " :time (cc/now)})
        (Thread/sleep 100)
        (recur (inc i)))))

  ;; Imagine now that our rate of messages on the input channel
  ;; far exceeds the processing capacity of 3 processors

  (defn slow-process [in pid]
    (future
      (loop []
        (when-some [m (async/<!! in)]
          (println (format "Received message %s on processor %s" m pid))
          (Thread/sleep 2000)
          (recur)))))


  (let [c1 (async/chan)
        c2 (async/chan)
        c3 (async/chan)]
    ;; Lets start our processor loops
    (slow-process c1 :first)
    (slow-process c2 :second)
    (slow-process c3 :third)

    ;; Now we have to read from the input channel
    ;; and write to any channel which is available
    ;; but if any write takes more than 500 ms, we exit the loop
    (async/go-loop []
      (let [m (async/<! input)
            t (FIXME)
            [val port] (async/alts! [[c1 m] [c2 m] [c3 m] t])]
        (if (identical? port t)
          (println "Consumers cant keep up ! Exiting")
          (recur)))))

  (future
    (loop [i 0]
      (when (< i 100)
        (async/>!! input {:job i :msg "New message to process " :time (cc/now)})
        (Thread/sleep 100)
        (recur (inc i)))))

  ;; The way to solve this is to use buffered channels
  ;; This way the writer doesn't have to block on the readers

  (def input (async/chan))

  (let [c1 (async/chan 10)
        c2 (async/chan 10)
        c3 (async/chan 10)]
    ;; Lets start our processor loops
    (slow-process c1 :first)
    (slow-process c2 :second)
    (slow-process c3 :third)

    ;; Now we have to read from the input channel
    ;; and write to any channel which is available
    (async/go-loop []
      (let [m (async/<! input)
            t (async/timeout 200)
            [val port] (async/alts! [[c1 m] [c2 m] [c3 m] t])]
        (if (identical? port t)
          (do
            (println "Consumers cant keep up !")
            (println "Exiting the consumer loop"))
          (do
            (println "Message written " (:job m))
            (recur))))))

  ;; Lets increase the rate of inputs and see what happens

  (future
    (loop [i 0]
      (when (< i 100)
        (async/>!! input {:job i :msg "New message to process " :time (cc/now)})
        (recur (inc i)))))

  ;; As you can see, we can process about 30 to 35 messages
  ;; before the consumer loop exits because of timeout.
  ;; And this is a good thing. This is called backpressure
  ;; and it lets us deal with different rates of writes and reads
  ;; cleanly !


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Quit channels
  ;;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


  ;; So lets say we have a long running processor loop
  ;; based off a core.async/channel

  (def context {:chan (async/chan 20) :db :mongodb})

  ;; This is the reader loop for the input channel
  (defn process [in]
    (future
      (loop []
        ;; Read only while the channel is open
        (FIXME [m (async/<!! in)]
          (println (format "Received message %s on processor" m ))
          (Thread/sleep (rand-int 1000))
          (recur)))
      (println "Exit")))

  (defn start-processors [context]
    (process (:chan context)))

  ;; Now lets say we want to have stop functionality
  ;; which indicates that something somewhere has gone wrong,
  ;; and we want to exit the channel reader. One obvious way would
  ;; be to just call close on the channel. Lets see how that works

  (defn stop-processors [context]
    (async/close! (:chan context)))

  (do
    (start-processors context)
    (future
      (doall
       (for [i (range 30)]
         (async/>!! (:chan context) i))))
    (Thread/sleep 1000)
    (stop-processors context)
    (println "Stopped the processor"))


  ;; What do you observe ?
  ;; As you can see, our messages are still being processed well after
  ;; we called stop. Why is that ?

  ;; So what's our option ?
  ;; Well another channel ofcourse !

  ;; Lets rewrite the process method to take a stop channel
  ;; Whenever we receive data on this control channel, we exit.

  (defn process [in stop]
    (future
      (loop []
        (let [[val port] (FIXME [in stop])]
          (if (identical? in port)
            (do
              (println (format "Received message %s on processor" val))
              (Thread/sleep (rand-int 1000))
              (recur))
            (do
              (println "We have been stopped. Exit")
              ;; A long cleanup
              (Thread/sleep 2000)
              (async/>!! stop :done)))))))


  (def context {:chan (async/chan 20)
                :db :mongodb
                :stop (async/chan)})

  (defn start-processors [context]
    (process (:chan context) (:stop context)))

  (defn stop-processors [context]
    ;; Now instead of calling close!, we just send
    ;; a stop signal on the control channel
    (FIXME)
    ;; A small addition here that you can do is wait for the
    ;; process being stopped to ack and give it time to clean up
    (println "Status of the clean up " (async/<!! (:stop context))))

  (do
    (start-processors context)
    (future
      (doall
       (for [i (range 30)]
         (async/>!! (:chan context) i))))
    (Thread/sleep 1000)
    (stop-processors context)
    (println "Stopped the processor"))
  )
