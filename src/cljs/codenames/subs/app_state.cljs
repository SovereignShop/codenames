(ns codenames.subs.app-state
  (:require
   [datascript.core :as d]
   [codenames.db :as db]
   [swig.macros :refer-macros [def-sub]]
   [taoensso.timbre :refer-macros [debug info warn]]))

(def-sub ::group-id
  [:find ?group-id .
   :in $
   :where
   [?id :codenames.app-state/group ?group-id]])

(def-sub ::current-game
  [:find ?current-game-id .
   :in $
   :where
   [?id :codenames.app-state/current-game ?current-game-id]])

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

(comment
  (d/q current-game @db/conn)

  )
