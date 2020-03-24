(ns codenames.views.pregame
  (:require
   [swig.views :as swig-view]))

(defmethod swig-view/dispatch ::pre-game
  [tab]
  [:div "Pre game"])



