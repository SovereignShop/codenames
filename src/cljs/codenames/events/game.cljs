(ns codenames.events.game
  "Events that occure during a game"
  (:require
   [datascript.core :as d]
   [swig.macros :refer [def-event-ds]]))

(def-event-ds ::new-board [db _] [])
(def-event-ds ::choose-word [db _] [])
(def-event-ds ::end-turn [db _] [])

