(ns codenames.subs.game
  (:require
   [swig.macros :refer [def-sub]]))

(def-sub ::cards
  [:find (pull ?card [:card/color
                      :card/position
                      :card/word])
   :in $
   :where
   [?card :card/color]])

(def-sub ::cards-played
  [:find (count ?red) (count ?blue)
   :in $
   :where
   [?red  :card/color   :red]
   [?red  :card/played? true]
   [?blue :card/color   :blue]
   [?blue :card/played? true]])

(def-sub ::cards-remaining
  [:find (count ?red) (count ?blue)
   :in $
   :where
   [?red :card/color :red]
   [?red :card/played? false]
   [?blue :card/color :blue]
   [?blue :card/played? false]])
