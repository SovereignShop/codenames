(ns codenames.events.server
  (:require
   [swig.macros :refer-macros [def-event-ds]]))

(def-event-ds ::set-server-status
  [db [_ id status]]
  [[:db/add id :server/processing? status]])

(def-event-ds ::set-current-msg
  [db [_ id msg]]
  [[:db/add id :server/current-msg msg]])
