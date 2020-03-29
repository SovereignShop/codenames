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
   [re-posh.core :as re-posh]))

(defn show-player [player]
  (let [player-type (:codenames.player/type player)
        player-user @(re-posh/subscribe
                      [::user-subs/get-user
                       (:db/id (:codenames.player/user player))])
        username    (:user/name player-user)]
    [h-box
     :gap "20px"
     :children
     [[box :child (str username)]
      [box :child (str player-type)]]]))

(defn show-players [color game-id]
  (let [players @(re-posh/subscribe [::pregame-subs/players color game-id])]
    [v-box
     :children
     [[button
       :label "Join Blue Team"
       :on-click #(re-posh/dispatch [::pregame-events/join-team color game-id])]
      [gap :size "10px"]
      [v-box
       :children
       (map show-player players)]]]))


#_(defmethod swig-view/dispatch tabs/blue-team-tab [tab] (show-players :blue))
#_(defmethod swig-view/dispatch tabs/red-team-tab [tab] (show-players :red))

(defn users-view []
  (let [group-id @(re-posh/subscribe [::session-subs/group])
        users    @(re-posh/subscribe [::user-subs/users group-id])]
    [:div "wtf"]
    #_[v-box
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
               (for [game games]
                 [v-box
                  :style {:flex "1 1 0%"}
                  :gap "20px"
                  :children
                  [[users-view]
                   [button
                    :label "Start Game"
                    :on-click #(re-posh/dispatch [::pregame-events/enter-game])]
                   [line :size "2px"]
                   [show-players :blue (:db/id game)]
                   [show-players :red (:db/id game)]
                   #_child]])))]]]]))
