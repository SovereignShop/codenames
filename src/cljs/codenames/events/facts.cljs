(ns codenames.events.facts
  "Events for adding datalog facts to the datascript database."
  (:require
   [re-posh.core :as re-posh]
   [taoensso.timbre :refer-macros [debug info warn]]))

(re-posh/reg-event-ds
 ::add-facts
 (fn [_ [_ facts dont-save?]]
   (debug :add-facts :facts/count (count facts))
   (if dont-save?
     (vary-meta facts merge {:db.transaction/no-save true})
     (vary-meta facts merge {:db.transaction/no-save false}))))
