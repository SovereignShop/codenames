(ns codenames.subs.stats
  (:require
   [swig.macros :refer-macros [def-sub]]))

(def-sub ::average-words-asked-per-turn
  [:find (mean ?n) .
   :in $ ?uid
   :where
   [?turn-id :codenames.turn/player ?payer-id]
   [?player-id :codenames.player/user ?uid]
   [?turn-id :codenames.turn/number ?n]])

(def-sub ::count-answers
  [:find [(count-distinct ?correct-guess) (count-distinct ?neutral-miss)
          (count-distinct ?assassin-miss) (count-distinct ?opponent-miss)
          (count-distinct ?turn-id)]
   :in $ ?user-id
   :where
   [?turn-id :codenames.turn/player ?player-id]
   [?team-id :codenames.team/players ?player-id]
   [?team-id :codenames.team/color ?color]
   [?player-id :codenames.player/user ?user-id]
   [?match-id :codenames.character-card/played? ?turn-id]
   [?match-id :codenames.character-card/color ?color]
   [?neutral-miss :codenames.character-card/played? ?turn-id]
   [?neutral-miss :codenames.character-card/role :neutral]
   [?assassin-miss :codenames.character-card/played? ?turn-id]
   [?assassin-miss :codenames.character-card/role :assassin]])

(def-sub ::count-opponent-answers
  [:find (count-distinct ?card-id)
   :in $ ?user-id
   ])
