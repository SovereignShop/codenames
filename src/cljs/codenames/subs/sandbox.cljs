(ns codenames.subs.sandbox
  (:require
   [codenames.db :as db]
   [codenames.subs.app-state :as app-state]
   [codenames.subs.game :as game]
   [codenames.subs.pregame :as pregame]
   [codenames.subs.users :as users]
   [codenames.subs.session :as session]
   [codenames.constants.ui-idents :as idents]
   [datascript.core :as d]))

(comment
  (def current-game (d/q app-state/current-game @db/conn))
  (def cards-remaining (d/q game/cards-remaining @db/conn current-game))

  (def group (d/q session/group @db/conn))
  (def users (d/q users/users @db/conn 36))

  (def session (d/entity @db/conn [:swig/ident idents/session]))
  (def user    (:session/user session))
  (def user-id (:db/id user))

  (d/q '[:find [tid ?pid]
         :in $ ?uid
         :where
         [?sid :session/game ?tid]
         [?tid :codenames.team/players ?pid]
         [?pid :codenames.player/user ?uid]]
       @db/conn
       user-id)

  (d/q '[:find ?tid .
         :in $ ?color
         :where
         [?id :session/teams ?tid]
         [?tid :codenames.team/color ?color]]
       @db/conn
       user-id
       :blue)


  (d/q '[:find ?tid .
         :in $ ?color
         :where
         [?id :session/game ?gid]
         [?gid :game/teams ?tid]
         [?tid :codenames.team/color ?color]]
       @db/conn
       user-id
       :red)

  (:db/id user)

  (def game    (:session/game session)) 
  (def teams   (:game/teams game))

  @db/conn
  (keys (:session/user (d/entity @db/conn [:swig/ident idents/session])))

  (d/q users/users @db/conn  #uuid "4b48f6e0-1b74-4ced-8cf3-984c3a2a551b")

  )
