(ns ^:figwheel-always codenames.views.game
  (:require
   [codenames.subs.game :as game-subs]
   [codenames.subs.app-state :as app-state]
   [codenames.subs.session :as session-subs]
   [codenames.constants.ui-tabs :as tabs]
   [codenames.constants.ui-splits :as splits]
   [codenames.events.game :as game-events]
   [codenames.events.pregame :as pregame-events]
   [codenames.db :as db]
   [swig.views :as swig-view]
   [re-posh.core :as re-posh]
   [re-com.core :as com :refer [h-box v-box box button gap]]))

(defn display-card
  [game-id
   {:keys [:codenames.word-card/word
           :codenames.word-card/position
           :codenames.word-card/character-card]
    :as   card}]
  (let [character-card-id (:db/id character-card)
        codemaster?       @(re-posh/subscribe [::game-subs/codemaster?])
        card              @(re-posh/subscribe [::game-subs/character-card character-card-id])
        played?           (:codenames.character-card/played? card)
        role              (:codenames.character-card/role card)]
    [box
     :attr  {:on-click #(re-posh/dispatch [::game-events/card-click game-id character-card-id])}
     :style {:width            "200px"
             :height           "70px"
             :text-align       "center"
             :background-color (case (or codemaster? played?) role
                                 :neutral  "tan"
                                 :blue     "blue"
                                 :red      "red"
                                 :assassin "black"
                                 "default")}
     :child [:h4 {:style {:text-align :center
                          :color "black"}} word]]))

(defn game-score [game-id]
  (let [red-remaining @(re-posh/subscribe [::game-subs/red-cards-remaining game-id])
        blue-remaining  @(re-posh/subscribe [::game-subs/blue-cards-remaining game-id])]
    [h-box
     :max-width "60px"
     :style {:width "50px"}
     :children
     [[:div {:style {:color "red" :width "20px"}} red-remaining]
      [:div {:style {:color "blue" :width "20px"}} blue-remaining]]]))

(defn board-info [game-id cards]
  (let [current-team      @(re-posh/subscribe [::game-subs/current-team game-id])
        team-color        (:codenames.team/color current-team)
        [_ winning-color] @(re-posh/subscribe [::game-subs/game-over game-id])]
    [h-box
     :gap "20px"
     :children
     [[button
       :on-click #(re-posh/dispatch [::game-events/new-game])
       :label "New Round"]
      [button
       :on-click #(re-posh/dispatch [::pregame-events/enter-pregame])
       :label "New Game"]
      [button
       :label "End Turn"
       :on-click #(re-posh/dispatch [::game-events/end-turn game-id])]
      (case team-color
        :blue [com/p {:style {:color "Blue"}} "Blue Team's Turn"]
        :red  [com/p {:style {:color "Red"}} "Red Team's Turn"]
        [com/p (str "Error.. " current-team)])
      (case winning-color
        :red  [com/p {:style {:color "red"}} "Red team wins!"]
        :blue [com/p {:style {:color "blue"}} "Blue team wins!"]
        nil)]]))

(defn board-grid [game-id cards]
  (let [cards (->> cards
                   (sort-by :codenames.word-card/position)
                   (partition (first db/board-dimensions)))]
    [:div {}
     [game-score game-id]
     (for [row cards]
       [h-box
        :children
        (for [card row]
          [display-card game-id card])])]))

(defmethod swig-view/dispatch tabs/game-board
  [tab]
  (when-let [game-id @(re-posh/subscribe [::session-subs/game])]
    (let [cards @(re-posh/subscribe [::game-subs/word-cards game-id])]
      [v-box
       :children
       [[board-info game-id cards]
        [board-grid game-id cards]]])))

(defmethod swig-view/dispatch tabs/leader-board
  [tab]
  [:div "Leader Board"])

(defmethod swig-view/dispatch tabs/score-board
  [tab]
  [:div "Score Board"])
