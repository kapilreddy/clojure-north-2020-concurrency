(defproject concurrency-workshop "0.1.0-SNAPSHOT"
  :description "Code for the Concurrency workshop"
  :url "https://github.com/kapilreddy/clojure-north-2020"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "1.2.603"]
                 [cheshire "5.10.0"]
                 [com.taoensso/nippy "2.14.0"]
                 [clj-time "0.15.2"]
                 [pandect "0.6.1"]
                 [clj-http "3.10.1"]]
  :repl-options {:init-ns concurrency-workshop.core})
