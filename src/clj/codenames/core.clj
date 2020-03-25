(ns codenames.core
  (:require
   [codenames.sente :as sente]
   [codenames.facts :as facts]
   [codenames.handlers :as handlers]
   [compojure.core :refer [defroutes]]
   [compojure.core :refer [GET POST]]
   [compojure.route :as route]
   [nrepl.server :as nrepl-server]
   [ring.adapter.jetty :as ring]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
   [nrepl.server :refer [start-server stop-server]]
   [taoensso.timbre.appenders.carmine :as car-appender]
   [taoensso.timbre.appenders.core :refer [println-appender]]
   [taoensso.timbre :as timbre :refer [debug info warn]]
   [taoensso.timbre.appenders.carmine :as car-appender]
   [taoensso.timbre.appenders.core :refer [println-appender]]
   [taoensso.timbre :as timbre :refer [debug info warn]]
   [org.httpkit.server :refer [run-server]]))

(defroutes routes
  (GET  "/" [& params] handlers/common)
  (GET "/login" [& params] handlers/join-group)
  (GET  "/chsk"  ring-req (sente/*ring-ajax-get-or-ws-handshake* ring-req))
  (POST "/chsk"  ring-req (sente/*ring-ajax-post*                ring-req) 1 2)
  (route/resources "/")
  (route/not-found (handlers/four-oh-four)))

(defn sente-route-wrapper [& params]
  (binding [sente/*current-uid* (-> params first :session :uid)
            sente/*current-gid* (-> params first :session :gid)]
    (when-not (nil? sente/*current-uid*)
      (sente/*chsk-send!* sente/*current-uid* [:codenames.sente/started-processing]))
    (let [ret (apply routes params)]
      (when-not (nil? sente/*current-uid*)
        (sente/*chsk-send!* sente/*current-uid* [:codenames.sente/finished-processing]))
      ret)))

(def application (wrap-cors (wrap-defaults sente-route-wrapper
                                           (-> site-defaults
                                               (update :security assoc :anti-forgery true)
                                               (assoc-in [:params :keywordize] {:parse-namespaces? true}))) 
                            :access-control-allow-origin [#".*"]
                            :access-control-allow-methods [:get :put :post :delete]))

(defonce server (atom nil))

(defn -main [& args]
  (let [port 3001
        nrepl-port 7888]
    (reset! server (run-server #'application {:port port}))
    (start-server :port nrepl-port)
    (info (format "started nrepl. Port=%s" nrepl-port))
    (sente/init-sente!)))

(defn stop-http-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(comment
  (-main)
  (stop-http-server)
  (@server)
  )
