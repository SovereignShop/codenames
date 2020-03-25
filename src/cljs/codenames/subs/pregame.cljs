(ns codenames.subs.pregame
  "Subs associdate with pre-game"
  (:require
   [datascript.core :as d]
   [codenames.db :as db]
   [swig.macros :refer [def-pull-many-sub]]))

(def-pull-many-sub ::players
  [:player/name
   :player/color
   :player/tpye])
