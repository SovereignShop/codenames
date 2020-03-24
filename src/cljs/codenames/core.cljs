(ns ^:figwheel-hooks codenames.core
  (:require
   [codenames.views.login]
   [codenames.views.game]
   [codenames.views.pregame]
   [codenames.db :as db]
   [codenames.config :as config]
   [swig.core :as swig]
   [swig.views :as swig-view]
   [reagent.core :as reagent]
   [re-posh.core :as re-posh]))

(defonce _ (re-posh/connect! db/conn))

(defonce init-db
  (do (swig/init db/login-layout)
      (re-posh/dispatch-sync [::initialize-db])))

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
  (swig/render [:swig/ident :swig/main-view]))

(defonce initialization-block (init))

(defn ^:after-load re-render []
  (swig/render [:swig/ident :swig/main-view]))
