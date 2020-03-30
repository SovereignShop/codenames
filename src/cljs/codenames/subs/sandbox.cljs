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
  (def game    (:session/game session))
  (def user    (:session/user session))
  (def user-id (:db/id user))


  (def-sub ::game-over
    (fn [[]] 
      [(re-posh/subscribe [::cards-remaining :blue])]
      :<- [::cards-remaining :red])
    [:find ?
     :in $ ?game-id
     :wehre
     [?]])

  (let [])

  (d/q '[:find ?team-id .
        :in $ ?game-id
        :where
         (or (and [?game-id :game/teams ?team-id]
                  [?team-id :codenames.team/color ?color]
                  [?id :codenames.character-card/played? true]
                  [?team-id :codenames.team/deck-size ?game-id])
             (and [?id :codenames.character-card/played? true]
                  [?id :codenames.character-card/role :assassin]
                  [?id :codenames.piece/game ?game-id]
                  [?game-id :game/current-team ?team-id]))]
       @db/conn
       (:db/id game))

  (d/q '[:find [?team-id ?color]
         :in $ ?game-id
         :where
         (or (and [?game-id :game/teams ?team-id]
                  [?id :codenames.character-card/role]
                  [?team-id :codenames.team/color ?color]
                  (or (and [?game-id :game/blue-cards-count 0]
                           [?team-id :codenames.team/color :blue])
                      (and [?game-id :game/red-cards-count 0]
                           [?team-id :codenames.team/color :red])))
             (and [?game-id :game/teams ?team-id]
                  [?team-id :codenames.team/color ?color]
                  [?id :codenames.character-card/played? true]
                  [?id :codenames.character-card/role :assassin]
                  [?id :codenames.piece/game ?game-id]
                  (not [?game-id :game/current-team ?team-id])))]
       @db/conn
       (:db/id game))

  (d/q '[:find [?team-id ?color]
         :in $ ?game-id
         :where
         [?game-id :game/teams]
         [?id :codenames.character-card/role]
         [?team-id :codenames.team/color]
         [?id :codenames.character-card/played?]
         (or (and [?game-id :game/teams ?team-id]
                  [?id :codenames.character-card/role]
                  [?team-id :codenames.team/color ?color]
                  (or (and [?game-id :game/blue-cards-count 0]
                           [?team-id :codenames.team/color :blue])
                      (and [?game-id :game/red-cards-count 0]
                           [?team-id :codenames.team/color :red])))
             (and [?game-id :game/teams ?team-id]
                  [?team-id :codenames.team/color ?color]
                  [?id :codenames.character-card/played? true]
                  [?id :codenames.character-card/role :assassin]
                  [?id :codenames.piece/game ?game-id]
                  (not [?game-id :game/current-team ?team-id])))]
       @db/conn
       (:db/id game))

  (d/entity @db/conn idents/session)

  (d/q game/word-cards @db/conn 43)
  (d/q game/cards-remaining @db/conn 43)

  (def game    (:session/game session))


  (def teams   (:game/teams game))

  @db/conn
  (keys (:session/user (d/entity @db/conn [:swig/ident idents/session])))

  (d/q users/users @db/conn  #uuid "4b48f6e0-1b74-4ced-8cf3-984c3a2a551b")

  )
