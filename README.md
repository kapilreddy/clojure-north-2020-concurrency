# Introduction

The project aims to introduce you to a Clojure concurrency primitives and core.async. It aims to equip Clojure users with knowledge and intuition on how to use the primitives.

# Target audience

Anyone who has been using Clojure regularly but haven't had a chance to apply Concurrency primitives to solve problems

# Prequisites

* A working editor setup with REPL workflow
* Working knowledge of Clojure

# Outline

## Clojure concurrency primitives

1. Introduction
2. Solving thundering herd problem
3. Anti-patterns and Gotchas

## core.async

1. Introduction to CSP and how it differs from shared-memory concurrency
    1. What are the common CSP patterns ?
2. Introduction to core.async library and its capabilities
    1. Basic APIs like take, put, alts, buffers, go routines
    2. Some supplementary stuff like pipelines, mix, mult etc
3. Build an in-memory data processing pipeline using channels


# License

Distributed under the MIT license.
