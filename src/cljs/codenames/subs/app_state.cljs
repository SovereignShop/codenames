(ns codenames.subs.app-state
  (:require
   [swig.macros :refer-macros [def-sub]]
   [taoensso.timbre :refer-macros [debug info warn]]))

(def-sub ::active-panel
  [:find ?active-panel .
   :where
   [?id :spa-state/active-panel ?active-panel]])

(def-sub ::query-status
  '[:find ?status .
    :where
    [?id :query-status/state ?status]])

(def-sub ::authenticated?
  [:find ?login-state .
   :where
   [?id :user-login/state ?login-state]])
