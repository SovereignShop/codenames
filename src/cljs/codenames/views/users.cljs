(ns codenames.views.users
  (:require
   [codenames.constants.ui-tabs :as tabs]
   [codenames.subs.session :as session-subs]
   [codenames.subs.users :as user-subs]
   [swig.views :as swig-view]
   [re-posh.core :as re-posh]
   [re-com.core :as rc]))

(defn column [header xs]
  [rc/v-box
   :gap "10px"
   :children
   (cons (if (string? header) [:div {:style {:font-weight "bold"}} header] header)
         (for [x xs]
           [rc/box :child (str x)]))])

(defn users-view []
  (let [group-id                 @(re-posh/subscribe [::session-subs/group])
        user-id                  @(re-posh/subscribe [::session-subs/user])
        users                    @(re-posh/subscribe [::user-subs/users group-id])
        gids                     (take (count users) (repeat group-id))
        usernames                (map :user/name users)
        ping-times               (map :user/last-seen users)]
    [rc/scroller
     :style {:flex "1 1 0%"}
     :child
     [rc/h-box
      :gap "10px"
      :children
      [[column "Group ID"  gids]
       [column "User Name" usernames]
       [column "Last Seen" ping-times]]]]))

(defmethod swig-view/dispatch tabs/users [tab] [users-view])
