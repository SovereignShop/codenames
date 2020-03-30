(ns codenames.subs.db
  (:require
   [swig.macros :refer-macros [def-sub]]))

(def-sub ::full-db
  [:find ?e ?a ?v
   :in $
   :where
   [?e ?a ?v]])
