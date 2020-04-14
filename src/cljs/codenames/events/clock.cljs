(ns codenames.events.clock
  (:require
   [codenames.constants.ui-idents :as idents]
   [swig.macros :refer [def-event-ds]]))

(def-event-ds ::set-time
  [db [_ t]]
  [{:swig/ident idents/clock
    :clock/latest-time t}])

