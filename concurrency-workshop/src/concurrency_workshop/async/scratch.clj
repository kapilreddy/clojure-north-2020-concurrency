(ns concurrency-workshop.async.scratch
  (:require [concurrency-workshop.async.util :as util]
            [clojure.core.async :as async]
            [taoensso.nippy :as nippy]))


(comment
  ;; Lets work through the FIXMEs below to arrive
  ;; at a core.async version of the solution


  ;; Our solution will look like the following

  "The pipeline can look like this :
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

  ;; Step 1 : Lets convert our source of data into
  ;; a channel

  (defn data-source []
    (let [out (async/chan 1024)]
      ;; Start a thread and loop
      ;; till you get data
      (FIXME (let [data (nippy/freeze (util/make-data))]
         (when data
           ;; do something with the data
           (FIXME)
           (Thread/sleep 1000)
           (recur))))
      out))

  ;; Step 2 : Lets decide on a way to specify
  ;; the details of the processors in the pipeline
  ;; Lets start with a vector of [num-processors fn ...]
  ;; For example : [2 inc 3 dec] will mean 2 parallel fns
  ;; for increment followed by 3 for decrement

  ;; Can you convert the original pipeline to new spec
  (def pipeline-spec [FIXME])


  ;; Step 3 : Lets build a function which will process the pipeline spec

  (defn pipeline<
  "Take a description of operations
  and an input channel
  Returns : a channel from which we can
  take results of the pipeline of operations.
  So data put on the input channel, may flow through
  any one of the parallel processors.
  "
  [desc in]
    (let [pairs FIXME]
      ;; We need some kind of reduction
      ;; so that we can process the pairs
      ;; and accumulate / connect them together
    (FIXME
     (fn [prev-c [num-processors processor-fn]]
       ;; We will create an output channel to receive
       ;; data returned by processor-fns
       ;; What would be a good buffer size of this chan ?
       (let [out-c (async/chan FIXME)]
         (dotimes [_ num-processors]
           ;; Here we need to map the processor-fn
           ;; from all data coming from previous channel
           ;; to the output channel
           (FIXME prev-c processor-fn out-c))
         out-c))
     in
     pairs)))


  ;; Step 4 Lets run !
  (defn map-chan [in f out]
    ;; Loop through all data on the in channel
    ;; and map it to the out channel
    ;; if we get nil, it indicates end of the stream
    FIXME)



  (defn run []
    (let [c (data-source)
          out (pipeline< pipeline-spec
                         c)]
      (async/go-loop []
        (async/<! out)
        (recur))))

  )
