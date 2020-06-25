(ns concurrency-workshop.csp_bonus
  (:require [clojure.core.async :as async]
            [clj-time.core :as cc]))

(comment
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
  ;; Mix
  ;; In most aspects merge and mix are the same but there are some
  ;; critical differences.
  ;;
  ;; It introduces an intermediary component - the mixer
  ;; It is configurable, you can add and remove input channels
  ;; Channels can be muted, paused and solo'ed on demand

  ;; :mute - keep taking from the input channel but discard any taken values
  ;; :pause - stop taking from the input channel
  ;; :solo - listen only to this (and other :soloed channels).

  ;; Lets start by creating channels for logs at
  ;; different levels
  (def debug-logger (async/chan))
  (def info-logger (async/chan))
  (def error-logger (async/chan))

  ;; Question write a function which
  ;; writes given log to a channel continously
  ;; after sleeping a random amount of time
  (defn log-constantly
    "Constantly publishes the given log on the channel
     at intervals every 0-5 seconds."
    [channel log]
    FIXME)

  ;; Lets create an output channel and a mixer
  ;; based on the channel
  (def log-dashboard (async/chan))
  (def log-mixer (async/mix log-dashboard))


  ;; Question : Lets write a consumer for the log
  ;; output channel and closes when channel is closed
  (defn get-logs
    []
    FIXME)

  (async/admix log-mixer debug-logger)
  (async/admix log-mixer info-logger)
  (async/admix log-mixer error-logger)

  (defn start-logging
    []
    (log-constantly debug-logger {:type :debug
                                  :msg (str "Debug log at " (cc/now)) })
    (log-constantly info-logger {:type :info
                                 :msg (str "Info log at " (cc/now)) })
    (log-constantly error-logger {:type :error
                                  :msg (str "Error log at " (cc/now)) }))

  ;; mute info logs for now : info will be taken but thrown away

  (async/toggle log-mixer {  info-logger {:mute true} })

  ;; pause the debug logs too : debug will not be taken from channel
  (async/toggle log-mixer {  debug-logger {:mute false
                                           :pause true} })

  ;; now only look at debug logs

  (async/toggle log-mixer {  debug-logger {:solo true
                                           :pause false} })


  )
