(ns codenames.subs.game
  (:require
   [datascript.core :as d]
   [swig.macros :refer [def-sub]]))

(def-sub ::cards
  [:find (pull ?card [:card/color
                      :card/position
                      :card/word])
   :in $
   :where
   [?card :card/color]])

(def-sub ::board-cards
  [:find (pull ?card [:board-card/position
                      :board-card/word])
   :in $ ?game-id
   :where
   [?card :board-card/word]
   [?card :codenames/game ?game-id]])

(def-sub ::cards-played
  [:find (count ?red) (count ?blue)
   :in $ ?game-id
   :where
   [?red  :card/color   :red]
   [?red  :card/played? true]
   [?red  :codenames/game ?game-id]
   [?blue :card/color   :blue]
   [?blue :card/played? true]
   [?blue :codenames/game ?game-id]])

(def-sub ::cards-remaining
  [:find (count ?red) (count ?blue)
   :in $ ?game-id
   :where
   [?red :card/color :red]
   [?red :card/played? false]
   [?red :codenames/game ?game-id]
   [?blue :card/color :blue]
   [?blue :card/played? false]
   [?blue :codenames/game ?game-id]])

(comment
  (d/q '[:find (count ?red) (count ?blue)
         :in $
         :where
         [?red  :card/color   :red]
         [?red  :card/played? true]
         [?blue :card/color   :blue]
         [?blue :card/played? true]]
       @db/conn)

  (d/q '[:find (pull ?card [:board-card/position
                            :board-card/word])
         :in $
         :where
         [?card :board-card/word]]
       @db/conn)


  )
