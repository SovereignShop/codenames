(ns ^:figwheel-hooks codenames.core
  (:require
   [codenames.events.app-state :as app-events]
   [codenames.events.facts]
   [codenames.events.game]
   [codenames.events.pregame]
   [codenames.events.server]
   [codenames.views.login]
   [codenames.views.game]
   [codenames.views.pregame]
   [codenames.db :as db]
   [codenames.sente :as sente]
   [codenames.config :as config]
   [datascript.core :as d]
   [swig.core :as swig]
   [swig.views :as swig-view]
   [reagent.core :as reagent]
   [re-posh.core :as re-posh]))

(defonce _ (re-posh/connect! db/conn))

(defn tx-log-listener [{:keys [db-before db-after tx-data tx-meta]}]
  (let [facts (into [] (filter (comp db/schema-keys :a)) tx-data)]
    (when-not (empty? facts)
      (sente/send-tx-data! facts))))

(defonce init-db
  (do (swig/init db/login-layout)
      (re-posh/dispatch-sync [::app-events/initialize-db])))

(methods swig-view/dispatch)

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
  (d/listen! db/conn ::tx-log-listener tx-log-listener)
  (swig/render [:swig/ident :swig/main-view]))

(defonce initialization-block (init))

(defn ^:after-load re-render []
  (swig/render [:swig/ident :swig/main-view]))
