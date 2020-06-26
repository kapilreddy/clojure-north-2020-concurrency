(ns concurrency-workshop.csp_solution
  (:require [concurrency-workshop.csp_util :as util]
            [clojure.core.async :as async]))

(comment
  ;; To convert our original synchronous sequence of operations
  ;; into a pipeline based on channels, we need to start with a
  ;; source of data which acts like a stream of messages.
  ;; To achieve this, lets use the generator pattern and use the
  ;; same original util/make-data function

  (defn data-source []
    ;; Do we need a buffered channel here ? Why ?
    (let [out (async/chan FIXME)]
      (async/thread
        (try
          (loop []
            (let [data (util/serialise (util/make-data))]
              (when data
                ;; Write the on the output channel
                (FIXME)
                (Thread/sleep (rand-int 500))
                (recur))))
          (catch Throwable ex
            (println "Error: " ex)))
        ;; Lets be good citizens and close the channel
        ;; while exiting
        (async/close! out)
        (println "Closing the consumer loop"))
      ;; What should we return from here ?
      FIXME))


  ;; Next lets see how we can specify the pipeline
  ;; of operations.
  ;; A simple specification could be pairs of [num & function]
  ;; and all operations specified in a vector.
  ;; For example [[2 inc] [3 dec] [4 filter]] will mean
  ;; we need to have 2 parallel processors for inc, 3 for dec
  ;; and 4 which can filter
  ;; At the output of every stage, we need to merge the
  ;; output and pass it on to the next stage, emulating
  ;; a series of fanout-fanin steps

  (defn pipeline
    "Take a description of operations
  and an input channel
  Returns : a channel from which we can
  take results of the pipeline of operations.
  So data put on the input channel, may flow through
  any one of the parallel processors.
  So the shape looks like this :
  input-channel
  |
  |
  fn1 fn1 fn1 fn1
  | merge
  |
  fn2 fn2
  | merge
  |
  out"
    [desc in]
    ;; Here we want to process all pairs
    ;; from the description, create input and output channels
    ;; and connect them down to one another. Sounds just like
    ;; a reduction!
    (FIXME
     ;; The accumulator for the reduction is the output of the
     ;; previous steps which serves as the input channel for the next stage
     (fn [prev-c [n f]]
       ;; First lets create the output channels.
       ;; What should be the size of the output channel at each stage ?
       (let [out-c (async/chan n)]
         (dotimes [_ n]
           ;; Here we have to apply function f to every
           ;; element coming out of prev-c and pass it to out-c
           ;; Lets come back to this in a bit
           (FIXME prev-c f out-c))
         out-c))
     ;; What is the initial channel here ?
     FIXME
     desc))


  ;; This is a simple transformation reading data from in
  ;; applying a fn f to it and writing it to out
  ;; Do this only until in channel is open
  (defn transform [in f out]
    (FIXME))


  (defn run []
    (let [c (data-source)
          out (pipeline [[2 util/deserialise]
                         [2 util/authorize]
                         [2 util/populate]
                         [2 util/validate]
                         [2 util/stream]]
                        c)]
      (async/go-loop []
        (async/<! out)
        (recur))))

  ;; Now you might be wondering if all of this is really
  ;; running in parallel ! Lets find out

  ;; Lets add a pipeline function which adds random delays
  (defn pause-rnd [x]
    (Thread/sleep (rand-int 3000))
    x)

  ;; Now lets add one more stage in the pipeline
  #_(pipeline [[2 util/deserialise]
               [2 util/authorize]
               [2 util/populate]
               [2 util/validate]
               [1 pause-rnd]
               [2 util/stream]]
              c)
  ;; What this will do is slow down the entire pipeline
  ;; by creating a bottleneck. Run and check the behavior

  ;; Now see what happens when you increase the number of processors
  ;; in the pause-rnd stage ? Did you notice the speed up ?


  ;; The above definition of transform, though sufficient
  ;; is rather naive right now. For one, it doesn't handle
  ;; any exceptions, secondly, it also doesnt handle closing
  ;; of the input channel and thus the pipeline

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; First lets look at exception handling
  ;; Currently the only source of external exceptions
  ;; in the pipeline is ...
  ;; ...
  ;; ...
  ;; that's right, the function f in each stage.
  ;; So lets start by wrapping try catch handling around the invocation

  (defn transform [in f out]
    (async/go (loop []
                (when-some [val (async/<! in)]
                  (let [val (FIXME
                              (f val)
                              (FIXME
                               :error))]
                    (cond
                      ;; If we get a nil from the fn f
                      ;; or if we caught an exception,
                      ;; read again
                      (or (nil? val)
                          (identical? val :error))
                      (recur)

                      :else (do
                              (async/>! out val)
                              (recur))))))))


  ;; Lets write a mock pipeline function
  ;; which psuedo-randomly throws an exception
  ;; as is tradition !
  (defn error-rnd [x]
    (if (= 0 (mod (rand-int 10) 5))
      (throw (Throwable. "A random error in the pipeline"))
      x))

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Now lets deal with the closing of the input channel
  ;; To handle that situation, we need to do 2 things,
  ;; 1. Remember that go blocks return a channel on which
  ;; they write the value of the final expression and then close!
  ;; 2. Wait on the closing of all channels in one stage
  ;; and close the output channel in response

  ;; Firstly we need to deal with the return channel from
  ;; the transform function
  (defn pipeline [desc in]
    (reduce
     (fn [prev-c [n f]]
       (let [out-c (async/chan n)
             ;; Lets get a handle on all
             ;; go blocks returned by transform
             ;; in this stage
             procs (for [_ (range n)]
                     (FIXME))
             ;; This channel will act as a merge
             ;; for all closes since go blocks close
             ;; when they return a value
             close-chan (async/merge procs)]
         ;; Now lets take from the close channel and
         ;; in the callback close the output channel
         ;; thus propagating the close forward
         (async/take! close-chan (fn [_]
                                   (println "Closing the output channel")
                                   (async/close! FIXME)))
         out-c))
     in
     desc))

  ;; Now if you look at our definition of transform,
  ;; it isnt explicitly returning a value.
  ;; What return value makes most sense here ?
  (defn transform [in f out]
    (async/go (loop []
                (when-some [val (async/<! in)]
                  (let [val (try
                              (f val)
                              (catch Throwable ex
                                (println "Exception in the pipeline" ex)
                                :error))]
                    (cond
                      (or (nil? val)
                          (identical? val :error))
                      (recur)

                      :else (do
                              (async/>! out val)
                              (recur))))))
              FIXME))

  ;; And we are done. Lets go and modify our original
  ;; data source to be able to close the stream of data.

  (def running (atom true))

  (defn data-source []
    ;; Do we need a buffered channel here ? Why ?
    (let [out (async/chan 1024)]
      (async/thread
        (try
          (loop []
            (let [data (util/serialise (util/make-data))]
              (when data
                ;; Write the on the output channel
                (async/>!! out data)
                (Thread/sleep (rand-int 500))
                (when @running
                  (recur)))))
          (catch Throwable ex
            (println "Error: " ex)))
        ;; Lets be good citizens and close the channel
        ;; while exiting
        (async/close! out)
        (println "Closing the consumer loop"))
      ;; What should we return from here ?
      out))

  (reset! running false)

  ;; This kind of pipeline pattern is so natural
  ;; to CSP that core.async has a built in function to
  ;; do exactly what we wrote above ! And ofcourse it uses
  ;; transducers ! :D

  #_(async/pipeline)

  (let [in (async/chan)
        out (async/chan)]
    (async/go-loop [i 0]
      (async/>! in i)
      (async/<! (async/timeout 300))
      (recur (inc i)))
    (async/go-loop []
      (when-some [v (async/<! out)]
        (println "Got a value in out channel " v)
        (recur)))
    (async/pipeline 2 out (comp (filter even?)
                                (map #(* 2 %))) in))
  )
