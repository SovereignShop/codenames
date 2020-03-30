(ns codenames.subs.game
  (:require
   [datascript.core :as d]
   [codenames.db :as db]
   [swig.macros :refer [def-sub def-pull-sub]]))

(def-sub ::cards
  [:find (pull ?card [:codenames.character-card/color
                      :codenames.character-card/position
                      :codenames.character-card/word])
   :in $
   :where
   [?card :codenames.character-card/color]])

(def-sub ::word-cards
  [:find (pull ?card [:codenames.word-card/position
                      :codenames.word-card/word
                      :codenames.word-card/character-card])
   :in $ ?game-id
   :where
   [?card :codenames.word-card/word]
   [?card :codenames.piece/game ?game-id]])

(def-sub ::cards-played
  [:find [(count-distinct ?red) (count-distinct ?blue)]
   :in $ ?game-id
   :where
   [?red  :codenames.character-card/role   :red]
   [?red  :codenames.character-card/played? true]
   [?red  :codenames.piece/game ?game-id]
   [?blue :codenames.character-card/role   :blue]
   [?blue :codenames.character-card/played? true]
   [?blue :codenames.piece/game ?game-id]])

(def-sub ::codemaster?
  [:find ?uid .
   :in $ ?game-id
   :where
   [?sid :session/user ?uid]
   [?game-id :game/teams ?tid]
   [?tid :codenames.team/players ?pid]
   [?pid :codenames.player/user ?uid]
   [?pid :codenames.player/type :codemaster]])


(def-sub ::red-cards-remaining
  [:find ?rem .
   :in $ ?game-id
   :where
   [?game-id :game/red-cards-count ?rem]])

(def-sub ::blue-cards-remaining
  [:find ?rem .
   :in $ ?game-id
   :where
   [?game-id :game/blue-cards-count ?rem]])

(def-sub ::current-team
  [:find (pull ?tid [:codenames.team/color
                     :codenames.team/name]) .
   :in $ ?game-id
   :where
   [?game-id :game/current-team ?tid]])

(def-pull-sub ::character-card
  [:codenames.character-card/role
   :codenames.character-card/played?])

(def-sub ::game-over
  [:find [?team-id ?color]
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
            (not [?game-id :game/current-team ?team-id])))])
