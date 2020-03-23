(ns codenames.core
  (:require
   [swig.core :as swig]
   [swig.macros :refer [def-event-ds reg-sub]]
   [swig.views :as swig-view]
   [re-posh.core :as re-posh]))

;; # Constants

(def *board-dimensions* [5 5])
(def *board-size* (apply * *board-dimensions*))

(def deck #{"Home" "Dog"})

(def game-state
  [{:player/name "John"
    :player/color "blue"
    :player/type "guesser"}
   {:player/name "David"
    :player/color "blue"
    :plyaer/type ""}
   {:card/word "Texas"
    :card/color "Blue"
    :card/played? false
    :card/position [0 0]}])

;; # Layouts

;; ## Player Login Layout

(def login-layout
  (swig/view {:swig/ident :views/main-view}
             (swig/window {:swig/ident :modal/login})))

;; ## Team selection Layout

(def team-selection
  (swig/view {:swig/ident :views/main-view}
             (swig/split {}
                         )))

;; ## Primary Game Layout

(def board-layout
  (swig/view  {:swig/ident :views/main-view}
              (swig/split {:swig/ident :splits/main-split}
                          (swig/split {:splits/}
                                      (swig/tab {})
                                      (swig/tab {}))
                          (siwg/tab {}))))

;; #  Events

;; ## Events before game starts

(def-event-ds :pre-game/play)
(def-event-ds :pre-game/choose-color)
(def-event-ds :pre-game/choose-player-type)
(def-event-ds :pre-game/randomize-teams)
(def-event-ds :pre-game/set-timer-length)

;; ## Events of game

(def-event-ds :game/new-board)
(def-event-ds :game/choose-word)
(def-event-ds :game/end-turn)

;; # Subscriptions

;; ## Pre-game subs

(def-pull-many-sub :pre-game/players
  [:player/name
   :player/color
   :player/tpye])

;; ## Game subs

(def-sub :subs.game/cards
  [:find (pull ?card [:card/color
                      :card/position
                      :card/word])
   :in $
   :where
   [?card :card/color]])

(def-sub :subs.game/cards-played
  [:find (count ?red) (count ?blue)
   :in $
   :where
   [?red  :card/color   :red]
   [?red  :card/played? true]
   [?blue :card/color   :blue]
   [?blue :card/played? true]])

(def-sub :subs.game/cards-remaining
  [:find (count ?red) (count ?blue)
   :in $
   :where
   [?red :card/color :red]
   [?red :card/played? false]
   [?blue :card/color :blue]
   [?blue :card/played? false]])

;; # Views

(defmethod swig-view/dispatch :views.pre-game/pre-game
  [tab])

(defn display-card
  [{:keys [:card/color
           :card/word
           ]}]
  [box
   :attr  {:on-click #(re-posh/dispatch [:events.game/card-click id])}
   :style {:background-color color}
   :child [:span word]])

(defn board-info []
  [:div "Board info"])

(defmethod swig-view/dispatch :views.game/game-board
  [tab]
  (let [cards (->> @(re-posh/subscribe [:subs.game/cards])
                   (sort-by :card/position)
                   (partition *board-size*))]
    [v-box
     :children
     (into [board-info]
           (for [card cards]
             [h-box :children (mapv display-card card)]))]))

(defmethod swig-view/dispatch :views.game/game-players
  [tab]
  )

(defmethod swig-view/dispatch :views.game/game-options
  [tab]
  )
