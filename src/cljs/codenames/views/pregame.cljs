(ns ^:figwheel-always codenames.views.pregame
  (:require
   [codenames.constants.ui-tabs :as tabs]
   [codenames.constants.ui-splits :as splits]
   [codenames.subs.pregame :as pregame-subs]
   [codenames.subs.users :as user-subs]
   [codenames.subs.players :as player-subs]
   [codenames.subs.app-state :as app-subs]
   [codenames.subs.session :as session-subs]
   [codenames.events.pregame :as pregame-events]
   [swig.views :as swig-view]
   [re-com.core :refer [h-box v-box button line box scroller gap]]
   [re-posh.core :as re-posh]
   [goog.string :as gstr]))

(defn show-player [player]
  (let [player-type (:codenames.player/type player)
        player-user @(re-posh/subscribe
                      [::user-subs/get-user
                       (:db/id (:codenames.player/user player))])
        username    (:user/name player-user)]
    [h-box
     :gap "20px"
     :children
     [[box :style {:color "black"} :child (str username)]
      [box :style {:color "black"} :child (str player-type)]]]))

(defn show-players [color game-id]
  (let [players     @(re-posh/subscribe [::pregame-subs/players color game-id])
        uid         @(re-posh/subscribe [::session-subs/user])
        uid->player (group-by (comp :db/id :codenames.player/user) players)]
    [v-box
     :children
     [[h-box
       :gap "5px"
       :children
       [[button
         :style {:color (case color :blue "blue" "red")}
         :label (gstr/format "Join %s Team" (case color :red "Red" "Blue"))
         :on-click #(re-posh/dispatch [::pregame-events/join-team color game-id])]
        (when-let [player (first (uid->player uid))]
          (if (= (:codenames.player/type player) :codemaster)
            [button
             :label "Guesser"
             :on-click #(re-posh/dispatch [::pregame-events/choose-player-type :guesser ])]
            [button
             :label (gstr/format "Code Master")
             :on-click #(re-posh/dispatch [::pregame-events/choose-player-type :codemaster])]))]]
      [gap :size "10px"]
      [v-box
       :children
       (map show-player players)]]]))

(defmethod swig-view/dispatch tabs/player-board
  [tab]
  (when-let [game-id @(re-posh/subscribe [::session-subs/game])]
    [v-box
     :children
     [[show-players :blue game-id]
      [show-players :red game-id]]]))

(defn users-view []
  (let [group-id @(re-posh/subscribe [::session-subs/group])
        users    @(re-posh/subscribe [::user-subs/users group-id])]
    [v-box
     :children
     (for [user users]
       [h-box
        :gap "10px"
        :children
        [[box :child (str group-id)]
         [box :child (str (:user/alias user))]]])]))

(defmethod swig-view/dispatch splits/team-selection-split
  [split child]
  (let [games @(re-posh/subscribe [::pregame-subs/open-games])
        my-game @(re-posh/subscribe [::session-subs/game])]
    [scroller
     :style {:flex "1 1 0%"}
     :child
     [h-box
      :style {:flex "1 1 0%"}
      :children
      [[:div {:style {:width "500px" :height "100%"}} "Chat Box"]
       [v-box
        :style {:flex "1 1 0%"}
        :gap "20px"
        :children
        (interpose
         [line]
         (cons [button
                :label "Create Game"
                :on-click #(re-posh/dispatch [::pregame-events/new-game])]
               (for [{game-id :db/id} games]
                 [v-box
                  :style {:flex "1 1 0%"}
                  :gap "20px"
                  :children
                  [[users-view]
                   [button
                    :label "Start Game"
                    :on-click #(re-posh/dispatch [::pregame-events/enter-game game-id])]
                   [line :size "2px"]
                   [show-players :blue game-id]
                   [show-players :red game-id]
                   #_child]])))]]]]))
