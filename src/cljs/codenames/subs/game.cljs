(ns codenames.subs.game
  (:require
   [datascript.core :as d]
   [codenames.db :as db]
   [swig.macros :refer [def-sub]]))

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
  [:find (count ?red) (count ?blue)
   :in $ ?game-id
   :where
   [?red  :codenames.character-card/color   :red]
   [?red  :codenames.character-card/played? true]
   [?red  :codenames.piece/game ?game-id]
   [?blue :codenames.character-card/color   :blue]
   [?blue :codenames.character-card/played? true]
   [?blue :codenames.piece/game ?game-id]])

(def-sub ::cards-remaining
  [:find [(count-distinct ?red) (count-distinct ?blue)]
   :in $ ?game-id
   :where
   [?red :codenames.character-card/color :red]
   [?red :codenames.character-card/played? false]
   [?red :codenames.piece/game ?game-id]
   [?blue :codenames.character-card/color :blue]
   [?blue :codenames.character-card/played? false]
   [?blue :codenames.piece/game ?game-id]])
