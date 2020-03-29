(ns codenames.subs.pregame
  "Subs associdate with pre-game"
  (:require
   [datascript.core :as d]
   [codenames.db :as db]
   [swig.macros :refer [def-sub]]))

(def-sub ::players
  [:find (pull ?pid [:codenames.player/type
                     :codenames.player/name
                     :codenames.player/user])
   :in $ ?color ?gid
   :where
   [?gid :game/teams ?tid]
   [?tid :codenames.team/color ?color]
   [?tid :codenames.team/players ?pid]])

(def-sub ::games
  [:find (pull ?pid [:game])])

(def-sub ::open-games
  [:find (pull ?id [:game/finished?
                    :game/id
                    :game/name
                    :game/teams])
   :in $
   :where
   [?id :game/finished? false]])
