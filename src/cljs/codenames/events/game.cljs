(ns codenames.events.game
  "Events that occure during a game"
  (:require
   [codenames.utils :refer [make-game]]
   [codenames.db :as db]
   [datascript.core :as d]
   [swig.macros :refer [def-event-ds]]))

(def-event-ds ::new-game [db _]
  (make-game db/words db/board-dimensions))

(def-event-ds ::choose-word [db _] [])

(def-event-ds ::end-turn [db _] [])
