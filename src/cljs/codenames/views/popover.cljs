(ns codenames.views.popover
  (:require
   [codenames.subs.popover :as pop-subs]
   [codenames.events.popover :as pop-events]
   [codenames.constants.ui-idents :as idents]
   [re-posh.core :as re-posh]
   [swig.views :as swig-view]
   [re-com.core :refer [modal-panel]]))

(defn main-popover []
  (let [{:keys [:popover/showing?
                :popover/title
                :popover/label
                :popover/content]}
        @(re-posh/subscribe [::pop-subs/get-popover [:swig/ident idents/main-popover]])]
    (when showing?
      [modal-panel
       :backdrop-color   "grey"
       :backdrop-opacity 0.4
       :backdrop-on-click #(re-posh/dispatch [::pop-events/hide [:swig/ident idents/main-popover]])
       :child content])))

(defmethod swig-view/dispatch idents/main-popover [_]
  (js/console.log "Showing maing popover!!")
  [main-popover])
