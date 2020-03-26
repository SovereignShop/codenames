(ns codenames.events.pregame
  (:require
   [codenames.db :as db]
   [codenames.constants.ui-tabs :as tabs]
   [swig.parser :refer [hiccup->facts]]
   [swig.macros :refer [def-event-ds]]))

(def-event-ds ::enter-game [db _]
  (with-meta
    (into [[:db.fn/retractAttribute [:swig/ident tabs/pregame] :swig.ref/parent]
           [:db/add [:swig/ident :swig/main-view] :swig.view/active-tab [:swig/ident tabs/game]]
           [:db/add [:swig/ident tabs/game] :swig.ref/parent [:swig/ident :swig/main-view]]])
    {:tx/group-update? true}))

(def-event-ds ::enter-pregame [db _]
  (with-meta
    (into [[:db.fn/retractAttribute [:swig/ident tabs/game] :swig.ref/parent]
           [:db/add [:swig/ident :swig/main-view] :swig.view/active-tab [:swig/ident tabs/pregame]]
           [:db/add [:swig/ident tabs/pregame] :swig.ref/parent [:swig/ident :swig/main-view]]])
    {:tx/group-update? true}))

(def-event-ds ::choose-color [db _]
  [])

(def-event-ds ::choose-player-type [db _]
  [])

(def-event-ds ::randomize-teams [db _]
  [])

(def-event-ds ::set-timer-length [db _]
  [])
