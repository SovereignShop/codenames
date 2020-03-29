(ns codenames.events.game
  "Events that occure during a game"
  (:require
   [codenames.utils :refer [make-game]]
   [codenames.db :as db]
   [datascript.core :as d]
   [swig.macros :refer [def-event-ds]]
   [taoensso.timbre :as timbre :refer-macros [debug info warn]]))

(def-event-ds ::card-click [db [_ card]]
  (info "card" card)
  (let [player (d/entity )])
  [])

(def-event-ds ::choose-word [db _] [])

(def-event-ds ::end-turn [db _] [])
