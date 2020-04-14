(ns codenames.views.login
  (:require [codenames.query :as q]
            [swig.views :as swig-view]
            [codenames.subs.app-state :as app-state]
            [codenames.constants.ui-idents :as idents]
            [re-com.core :refer [h-box v-box line border title modal-panel
                                 alert-box throbber input-text button p]]
            [re-posh.core :as re-posh]
            [reagent.ratom :refer [reaction]]
            [reagent.core :as reagent]))

(defn dialog-markup
  [login-state form-data process-ok]
  [border
   :border "1px solid #eee"
   :child  [v-box
            :padding  "10px"
            :style    {:background-color "cornsilk"}
            :children [[title :label "Welcome! Please log in" :level :level2]
                       [v-box
                        :class    "form-group"
                        :children [[:label {:for "pf-username"} "Username"]
                                   [input-text
                                    :model       (:username @form-data)
                                    :on-change   #(swap! form-data assoc :username %)
                                    :placeholder "Enter username"
                                    :class       "form-control"
                                    :attr        {:id "pf-username"}]]]
                       [v-box
                        :class    "form-group"
                        :children [[:label {:for "pf-groupname"} "Group Name"]
                                   [input-text
                                    :model       (:groupname @form-data)
                                    :on-change   #(swap! form-data assoc :groupname %)
                                    :placeholder "Enter group name"
                                    :class       "form-control"
                                    :attr        {:id "pf-groupname"}]]]
                       [line :color "#ddd" :style {:margin "10px 0 10px"}]
                       [h-box
                        :gap      "12px"
                        :children [[button
                                    :label    "Join or Create Group"
                                    :class    "btn-primary"
                                    :on-click process-ok]]]
                       (when (not= @login-state :unauthenticated)
                         [line :color "#ddd" :style {:margin "10px 0 10px"}])
                       (case @login-state
                         :waiting [throbber :size :small :color "blue"]
                         :failed  [alert-box
                                   :alert-type :danger
                                   :heading "Login Failed!"
                                   :body "Incorrect username or password."]
                         nil)]]])

(defn modal-dialog
  "Create a button to test the modal component for modal dialogs"
  []
  (let [login-state    (re-posh/subscribe [::app-state/authenticated?])
        show?          (reaction (not= @login-state :authenticated))
        form-data      (reagent/atom {:username    ""
                                      :groupname   ""
                                      :password    "abc123"
                                      :remember-me true})
        process-ok     (fn [_]
                         (q/do-login @form-data)
                         false)]
    (fn []
      [v-box
       :children [(when @show?
                    [modal-panel
                     :backdrop-color   "grey"
                     :backdrop-opacity 0.0
                     :style            {:font-family "Consolas"}
                     :child            [dialog-markup
                                        login-state
                                        form-data
                                        process-ok]])]])))

(defmethod swig-view/dispatch idents/login-window [_]
  [:img {:src "assets/Inspiring_sunset.jpg"
         :style {:width "100vw"
                 :height "100vh"}}])

(defmethod swig-view/dispatch idents/modal-dialog [_]
  [modal-dialog])
