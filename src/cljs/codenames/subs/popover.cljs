(ns codenames.subs.popover
  (:require
   [swig.macros :refer-macros [def-pull-sub]]))

(def-pull-sub ::get-popover
  [:popover/content
   :popover/label
   :popover/title
   :popover/showing?])
