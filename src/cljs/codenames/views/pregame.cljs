(ns ^:figwheel-always codenames.views.pregame
  (:require
   [codenames.constants.ui-tabs :as tabs]
   [codenames.constants.ui-splits :as splits]
   [codenames.subs.pregame :as pregame-subs]
   [codenames.subs.users :as user-subs]
   [codenames.subs.players :as player-subs]
   [codenames.subs.app-state :as app-subs]
   [codenames.events.pregame :as pregame-events]
   [swig.views :as swig-view]
   [re-com.core :refer [h-box v-box button line]]
   [re-posh.core :as re-posh]))

(defmethod swig-view/dispatch tabs/red-team-tab
  [tab]
  (let [group-id (re-posh/subscribe [::app-subs/group-id])
        users    @(re-posh/subscribe [::user-subs/users group-id])]
    [:div {:style {:width "100%"}} (str "users " users)]))

(defmethod swig-view/dispatch tabs/blue-team-tab
  [tab]
  [:div {:style {:width "100%"}} "Blue Team"])

(defmethod swig-view/dispatch splits/team-selection-split
  [split child]
  [h-box
   :style {:flex "1 1 0%"}
   :children
   [[:div {:style {:width "500px" :height "100%"}} "Chat Box"]
    [v-box
     :style {:flex "1 1 0%"}
     :gap "20px"
     :children
     [[button
       :label "Start Game"
       :on-click #(re-posh/dispatch [::pregame-events/enter-game])]
      [line :size "2px"]
      child]]]])
