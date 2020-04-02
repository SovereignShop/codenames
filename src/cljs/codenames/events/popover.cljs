(ns codenames.events.popover
  (:require
   [swig.macros :refer-macros [def-event-ds]]))

(def-event-ds ::hide
  [db [_ id]]
  (with-meta
    [[:db/add id :popover/showing? false]]
    {:tx/group-update? true}))

(def-event-ds ::show
  [db [_ id props]]
  (with-meta
    [(assoc props :db/id id)]
    {:tx/group-update? true}))
