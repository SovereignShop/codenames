(ns ^:figwheel-always codenames.subs.popover
  (:require
   [swig.macros :refer [def-pull-sub]]))

(def-pull-sub ::get-popover
  [:popover/content
   :popover/label
   :popover/title
   :popover/showing?])
