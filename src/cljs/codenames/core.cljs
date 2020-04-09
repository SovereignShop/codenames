(ns ^:figwheel-hooks codenames.core
  (:require
   [codenames.events.app-state :as app-events]
   [codenames.constants.ui-idents]
   [codenames.constants.ui-tabs]
   [codenames.constants.ui-splits]
   [codenames.constants.ui-views]
   [codenames.subs.users]
   [codenames.subs.session]
   [codenames.subs.game]
   [codenames.subs.pregame]
   [codenames.subs.app-state]
   [codenames.subs.popover]
   [codenames.subs.clock]
   [codenames.events.facts]
   [codenames.events.game]
   [codenames.events.pregame]
   [codenames.events.server]
   [codenames.events.popover]
   [codenames.events.clock]
   [codenames.views.users]
   [codenames.views.login]
   [codenames.views.game]
   [codenames.views.pregame]
   [codenames.views.db]
   [codenames.views.popover]
   [codenames.views.chat :as chat]
   [codenames.subs.db]
   [codenames.utils]
   [codenames.clock :as clock]
   [codenames.db :as db]
   [codenames.comms :as sente]
   [codenames.config :as config]
   [datascript.core :as d]
   [swig.core :as swig]
   [swig.views :as swig-view]
   [reagent.core :as reagent]
   [re-posh.core :as re-posh]))

(defonce _ (re-posh/connect! db/conn))

(defn tx-log-listener [{:keys [db-before db-after tx-data tx-meta]}]
  (when (:db.transaction/log-message? tx-meta)
    (let [log-data (into {} (map (juxt :a :v)) tx-data)
          user-id  (:chat/user log-data)
          user     (d/entity db-after user-id)]
      (chat/add-log! (:chat/user log-data) (:user/name user) (:chat/message log-data))))
  (when-not (:db.transaction/no-save tx-meta)
    (let [facts (into [] (filter (comp db/schema-keys :a)) tx-data)
          gid   (d/q '[:find ?groupname .
                       :in $
                       :where
                       [?id :group/name ?groupname]]
                     db-after)]
      (when-not (empty? facts)
        (cond (:tx/group-update? tx-meta) (sente/send-event! [:codenames.comms/group-facts {:gid    gid
                                                                                            :datoms facts}])
              :else                       (do (js/console.log "sending!") 
                                              (sente/send-event! [:codenames.comms/facts
                                                                  {:gid gid :datoms facts :tx-meta tx-meta}])))))))

(defonce init-db
  (do (swig/init db/login-layout)
      (re-posh/dispatch-sync [::app-events/initialize-db])))

(defmethod swig-view/dispatch :swig.type/cell
  ([{:keys [:db/id
            :swig.cell/element
            :swig.dispatch/handler]
     :as   props}]
   (if handler
     [(get-method swig-view/dispatch handler) props]
     element)))

(defn mount-root [route-specs main-component]
  (reagent/render main-component
                  (.getElementById js/document "app")))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn ^:export init []
  (dev-setup)
  (d/listen! db/conn ::tx-log-listener #'tx-log-listener)
  (clock/start-clock!) 
  (swig/render [:swig/ident :swig/root-view]))

(defonce initialization-block (init))

(defn ^:after-load re-render []
  (swig/render [:swig/ident :swig/root-view]))
