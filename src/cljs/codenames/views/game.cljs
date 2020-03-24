(ns codenames.views.game
  (:require
   [codenames.constants.ui-tabs :as tabs]
   [codenames.constants.ui-splits :as splits]
   [codenames.db :as db]
   [swig.views :as swig-view]
   [re-posh.core :as re-posh]
   [re-com.core :refer [h-box v-box box]]))

(defn display-card
  [{:keys [:card/color
           :card/word]
    :as card}]
  [box
   :attr  {:on-click #(re-posh/dispatch [:events.game/card-click card])}
   :style {:background-color color}
   :child [:span word]])

(defn board-info []
  [:div "Board info"])

(defmethod swig-view/dispatch tabs/game-board
  [tab]
  [:div "Game Board"]
  #_(let [cards (->> @(re-posh/subscribe [:subs.game/cards])
                   (sort-by :card/position)
                   (partition db/board-size))]
    [v-box
     :children
     (into [board-info]
           (for [card cards]
             [h-box :children (mapv display-card card)]))]))

(defmethod swig-view/dispatch tabs/leader-board
  [tab]
  [:div "Leader Board"])

(defmethod swig-view/dispatch tabs/player-board
  [tab]
  [:div "Player Board"])

(defmethod swig-view/dispatch tabs/score-board
  [tab]
  [:div "Score Board"])
