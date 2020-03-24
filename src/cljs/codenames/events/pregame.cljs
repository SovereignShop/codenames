(ns codenames.events.pregame
  (:require
   [codenames.db :as db]
   [swig.macros :refer [def-event-ds]]))

(def-event-ds ::play [db _] [])
(def-event-ds ::choose-color [db _] [])
(def-event-ds ::choose-player-type [db _] [])
(def-event-ds ::randomize-teams [db _] [])
(def-event-ds ::set-timer-length [db _] [])

