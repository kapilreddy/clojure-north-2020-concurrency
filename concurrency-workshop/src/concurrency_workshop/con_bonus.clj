(ns concurrency-workshop.con-bonus)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Delay
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Delay the execution. Execute on first deref

(comment
  (def p (delay (Thread/sleep 5000)
                (println "here!")
                1))

  ;; This will block and execute
  @p)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Agents
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Similar to an atom but it is async. Changing value does not run on
;; the same thread

(comment
  (def a (agent 0))

  ;; This will block and execute
  (send a inc)

  @a)
