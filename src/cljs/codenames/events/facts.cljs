(ns codenames.events.facts
  "Events for adding datalog facts to the datascript database."
  (:require
   [re-posh.core :as re-posh]
   [cljs-http.client :as http]
   [cljs.core.async :refer [chan go <! put!]]
   [re-frame.core :as re-frame]
   [datascript.transit :as dt]
   [taoensso.timbre :refer-macros [debug info warn]]))

(re-posh/reg-event-ds
 ::add-facts
 (fn [_ [_ facts save?]]
   (debug :add-facts :facts/count (count facts))
   (if save?
     (with-meta facts {:db.transaction/save true})
     facts)))

(re-frame/reg-fx
 ::save-datoms
 (fn save-datoms
   [type id datoms]
   (js/alert "saving!")
   (go (let [body (-> (http/post "http://localhost:8081/session/save-datoms"
                                 {:body         (dt/write-transit-str datoms)
                                  :query-params {:type type
                                                 :id   id}})
                      (<!))]
         (info "Saved file successfully.")))))
