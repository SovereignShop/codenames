(ns codenames.subs.users
  (:require
   [datascript.core :as d]
   [codenames.db :as db]
   [swig.macros :refer-macros [def-sub]]
   [taoensso.timbre :refer-macros [debug info warn]]))

(def-sub ::users
  [:find (pull ?user [:user/alias
                      :user/id
                      :user/name])
   :in $ ?group-id
   :where
   [?group-id :group/users ?user]])
