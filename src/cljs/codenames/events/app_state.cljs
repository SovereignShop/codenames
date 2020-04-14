(ns codenames.events.app-state
  (:require
   [codenames.comms :refer [init-sente!]]
   [codenames.db :as db]
   [datascript.core :as d]
   [re-posh.core :as re-posh]
   [goog.dom :as gdom]
   [datascript.transit :as dt]
   [taoensso.timbre :as timbre :refer-macros [debug info warn]]))

(timbre/set-level! :trace)

(re-posh/reg-event-ds
 ::initialize-db
 (fn [_ _]
   (debug ::initialize-db)
   (let [app (gdom/getElement "app")]
     (if-let [db (.getAttribute app "data-ds")]
       (do (.removeAttribute app "data-ds")
           (d/datoms (dt/read-transit-str db) :eavt))
       db/default-db))))

(re-posh/reg-event-ds
 ::login-success
 (fn [_ _]
   (init-sente!)
   [[:db/add [:swig/ident :user-login] :user-login/state :authenticated]]))

(re-posh/reg-event-ds
 ::login-fail
 (fn [_ _]
   [[:db/add [:swig/ident :user-login] :user-login/state :failed]]))

(re-posh/reg-event-ds
 ::login-waiting
 (fn [db _]
   (info (d/pull db '[*] 24))
   [[:db/add [:swig/ident :user-login] :user-login/state :waiting]]))
