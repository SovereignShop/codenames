(ns ^:figwheel-always codenames.subs.users
  (:require
   [datascript.core :as d]
   [codenames.db :as db]
   [swig.macros :refer-macros [def-sub]]
   [taoensso.timbre :refer-macros [debug info warn]]))

(def-sub ::users
  [:find (pull ?user [:user/alias
                      :user/id
                      :user/name
                      :user/last-seen])
   :in $ ?gid
   :where
   [?gid :group/users ?user]])

(def-sub ::get-user
  [:find (pull ?uid [:user/alias
                     :user/id
                     :user/name
                     :user/last-seen]) .
   :in $ ?uid
   :where
   [?uid :user/name]])
