(ns concurrency-workshop.chapter6
  (:require [clojure.core.async :as async]
            [clj-time.core :as cc]))

(comment

  ;; Merge
  ;; core.async has a very useful functionality to combine
  ;; results from multiple channels into 1

  ;; The way it works is you can create a merge channel
  ;; which will receive messages from all the merged channels
  ;; until all of them close! This can be used to build interesting
  ;; functionality like the data processing pipeline which we will see
  ;; in the assignment

  (def in-channel-one   (async/chan))
  (def in-channel-two   (async/chan))
  (def in-channel-three (async/chan))

  ;; Question write a function which
  ;; writes given value to a channel continously
  ;; after sleeping a random amount of time
  (defn write-constantly
    "Constantly publishes the given value to the given channel in random
     intervals every 0-5 seconds."
    [channel publish-value]
    (async/go (loop []
                ;; We want to wait a random amount of time
                (async/<! (FIXME (* 1000 (rand-int 5))))
                ;; We keep writing looping only if our writes
                ;; are successful
                (FIXME (async/>! channel publish-value)
                  (recur)))))

  (def merged (async/merge [in-channel-one
                            in-channel-two
                            in-channel-three]))

  ;; Question : Write a consumer for the merged channel
  ;; the consumption should stop once the channel is closed!
  (def merger (async/go-loop []
                ;; if we get non-nil value from channel, recur
                (FIXME)))

  (defn trigger-merges
    []
    (write-constantly in-channel-one   "channel-one")
    (write-constantly in-channel-two   "channel-two")
    (write-constantly in-channel-three "channel-three"))

  (async/close! in-channel-three)

  ;; Pub-Sub
  ;; Lets create a simple message queue with core.async's
  ;; channels

  ;; The basic construct that we need is a publication
  ;; which is the entity that manages the input channel of the
  ;; message queue. To create it, we have to give it an input channel
  ;; and a routing function.
  ;; This routing function will be called on each incoming message
  ;; to decide which subscribers need to get that message.
  ;; Conceptually this is similar to a multi-method dispatch function

  (def input (async/chan))
  (def message-broker (async/pub input :topic))


  ;; To use this broker, we need to add subscribers to it.
  ;; adding a subscriber requires the publication, a possible value
  ;; returned by the routing function and the channel on which to forward
  ;; the message.
  ;; This looks like

  ;; publication(input channel) ---(routing-fn)--> output-chan1
  ;;                                           --> output-chan2


  ;; Lets create channels for our topics
  (def alerts (async/chan))     ;; routing value :alert
  (def audit-logs (async/chan)) ;; routing value :audit
  (def analytics (async/chan))  ;; routing value :analytics


  ;; Question Lets create our subcriptions and write the
  ;; go loops to read from these output channels

  (async/sub FIXME)
  (async/sub FIXME)
  (async/sub FIXME)

  ;; Keep reading from the channels till you get a nil
  (FIXME)
  (FIXME)
  (FIXME)


  (async/>!! input {:topic :alert :msg "This is an alert"})
  (async/>!! input {:topic :analytics :msg "This is an analytics event"})
  (async/>!! input {:topic :audit :msg "This is an audit log"})

  )
