(ns codenames.views.login
  (:require [codenames.query :as q]
            [swig.views :as swig-view]
            [codenames.subs.app-state :as app-state]
            [codenames.constants.ui-idents :as idents]
            [re-com.core :refer [h-box v-box box gap line border title label modal-panel
                                 alert-box throbber progress-bar input-text checkbox button p]]
            [re-com.modal-panel :refer [modal-panel-args-desc]]
            [re-posh.core :as re-posh]
            [reagent.core :as reagent]))

(defn dialog-markup
  [login-state form-data process-ok process-cancel]
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
                       [v-box
                        :class    "form-group"
                        :children [[:label {:for "pf-password"} "Password"]
                                   [input-text
                                    :model       (:password @form-data)
                                    :on-change   #(swap! form-data assoc :password %)
                                    :placeholder "Enter password"
                                    :class       "form-control"
                                    :attr        {:id "pf-password" :type "password"}]]]
                       [checkbox
                        :label     "Forget me"
                        :model     (:remember-me @form-data)
                        :on-change #(swap! form-data assoc :remember-me %)]
                       [line :color "#ddd" :style {:margin "10px 0 10px"}]
                       [h-box
                        :gap      "12px"
                        :children [[button
                                    :label    "Join group"
                                    :class    "btn-primary"
                                    :on-click process-ok]
                                   [button
                                    :label    "Create group"
                                    :class    "btn-primary"
                                    :on-click process-ok]
                                   [button
                                    :label    "Cancel"
                                    :on-click process-cancel]]]
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
        show?          (reagent.ratom/reaction (not= @login-state :authenticated))
        form-data      (reagent/atom {:username "Nicole"
                                      :groupname "testing"
                                      :password "abc123"
                                      :remember-me true})
        save-form-data (reagent/atom nil)
        process-ok     (fn [event]
                         (q/do-login @form-data)
                         false)
        process-cancel (fn [& _]
                         (re-posh/dispatch [:codenames.events.app-state/login-success])
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
                                        process-ok
                                        process-cancel]])]])))

(defmethod swig-view/dispatch idents/login-window [_]
  [:img {:src "assets/Inspiring_sunset.jpg"
         :style {:width "100vw"
                 :height "100vh"}}])

(defmethod swig-view/dispatch idents/modal-dialog [_]
  [modal-dialog])
