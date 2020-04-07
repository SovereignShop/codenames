(ns codenames.clock
  (:require
   [codenames.events.clock :as clock-events]
   [cljs.core.async :refer [go-loop timeout <!]]
   [re-posh.core :as re-posh]
   [cljs-time.core :as time]))

(defn start-clock! []
  (re-posh/dispatch-sync [::clock-events/set-time (time/now)])
  (go-loop []
    (<! (timeout (* 1000 60)))
    (re-posh/dispatch [::clock-events/set-time (time/now)])
    (recur)))
