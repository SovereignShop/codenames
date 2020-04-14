(ns codenames.subs.app-state
  (:require
   [swig.macros :refer-macros [def-sub]]))

(def-sub ::authenticated?
  [:find ?login-state .
   :where
   [?id :user-login/state ?login-state]])
