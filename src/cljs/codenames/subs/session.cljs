(ns codenames.subs.session
  (:require
   [swig.macros :refer-macros [def-sub]]
   [taoensso.timbre :refer-macros [debug info warn]]))

(def-sub ::user
  [:find ?user .
   :where
   [?id :session/user ?user]])

(def-sub ::group
  [:find ?group .
   :where
   [?id :session/group ?group]])

(def-sub ::game
  [:find ?game .
   :where
   [?id :session/game ?game]])
