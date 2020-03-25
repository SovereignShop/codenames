(ns ^:figwheel-always codenames.views.game
  (:require
   [codenames.subs.game :as game-subs]
   [codenames.subs.app-state :as app-state]
   [codenames.constants.ui-tabs :as tabs]
   [codenames.constants.ui-splits :as splits]
   [codenames.events.game :as game-events]
   [codenames.events.pregame :as pregame-events]
   [codenames.db :as db]
   [swig.views :as swig-view]
   [re-posh.core :as re-posh]
   [re-com.core :refer [h-box v-box box button gap]]))

(defn display-card
  [{:keys [:codenames.word-card/word
           :codenames.word-card/position]
    :as   card}]
  [box
   :attr  {:on-click #(re-posh/dispatch [::game-events/card-click card])}
   :style {:width            "200px"
           :height           "70px"
           :text-align       "center"
           :background-color "red"}
   :child [:h4 {:style {:text-align :center}} word]])

(defn game-score [game-id]
  (let [[red-remaining blue-remaining] @(re-posh/subscribe [::game-subs/cards-remaining game-id])]
    [:div (str red-remaining " - " blue-remaining)]))

(defn board-info [game-id]
  [h-box
   :gap "20px"
   :children
   [[button
     :on-click #(re-posh/dispatch [::game-events/new-game])
     :label "New Round"]
    [button
     :on-click #(re-posh/dispatch [::pregame-events/enter-pregame])
     :label "New Game"]]])

(defn board-grid [game-id]
  (let [cards (->> @(re-posh/subscribe [::game-subs/board-cards game-id])
                   (sort-by :codenames.word-card/position)
                   (partition (first db/board-dimensions)))]
    [:div {}
     [game-score game-id]
     (for [row cards]
       [h-box :children (mapv display-card row)])]))

(defmethod swig-view/dispatch tabs/game-board
  [tab]
  (let [game-id @(re-posh/subscribe [::app-state/current-game])]
    [v-box
     :children
     [[board-info game-id]
      (when game-id
        [board-grid game-id])]]))

(defmethod swig-view/dispatch tabs/leader-board
  [tab]
  [:div "Leader Board"])

(defmethod swig-view/dispatch tabs/player-board
  [tab]
  [:div "Player Board"])

(defmethod swig-view/dispatch tabs/score-board
  [tab]
  [:div "Score Board"])
