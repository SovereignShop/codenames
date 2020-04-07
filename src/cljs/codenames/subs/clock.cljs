(ns codenames.subs.clock
  (:require
   [codenames.constants.ui-idents :as idents]
   [swig.macros :refer [def-sub]]))

(def-sub ::latest-time
  [:find ?time .
   :in $
   :where
   [?id :swig/ident :codenames.constants.ui-idents/clock]
   [?id :clock/latest-time ?time]])
