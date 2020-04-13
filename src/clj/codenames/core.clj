(ns codenames.core
  (:require
   [codenames.comms :as sente]
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
   [org.httpkit.server :refer [run-server]]
   [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defroutes routes
  (GET  "/" [& params] handlers/common)
  (GET "/login" [& params] handlers/join-group)
  (GET  "/chsk"  ring-req (sente/*ring-ajax-get-or-ws-handshake* ring-req))
  (POST "/chsk"  ring-req (sente/*ring-ajax-post*                ring-req) 1 2)
  (route/resources "/")
  (route/not-found (handlers/four-oh-four)))

(defn sente-route-wrapper [& params]
  (let [session (-> params first :session)
        uid (:uid session)
        groupname (:gid session)]
    (binding [sente/*current-uid* uid]
      (when-not (nil? sente/*current-uid*)
        (sente/*chsk-send!* sente/*current-uid* [:codenames.comms/started-processing]))
      (let [ret (apply routes params)]
        (when-not (nil? sente/*current-uid*)
          (sente/*chsk-send!* sente/*current-uid* [:codenames.comms/finished-processing]))
        ret))))

(def application (wrap-cors (wrap-defaults sente-route-wrapper
                                           (-> site-defaults
                                               (update :security assoc :anti-forgery true)
                                               (assoc-in [:params :keywordize] {:parse-namespaces? true})))
                            :access-control-allow-origin [#".*"]
                            :access-control-allow-methods [:get :put :post :delete]))

(defonce server (atom nil))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :default 3001
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-n" "--nrepl-port" "Nrepl Port"
    :default nil
    :parse-fn #(when % (Integer/parseInt %))
    :validate [#(or (nil? %) (< 0 % 0x10000)) "Must be a number between 0 and 65536"]]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [options errors summary] :as args} (parse-opts args cli-options)]
    (cond (not (empty? errors)) (throw (Exception. (pr-str errors)))
          :else
          (let [{:keys [nrepl-port port]} options]
            (println options args port)
            (reset! server (run-server #'application {:port port}))
            (when nrepl-port
              (start-server :port nrepl-port)
              (info (format "started nrepl. Port=%s" nrepl-port)))
            (sente/init-sente!)))))

(defn stop-http-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(comment
  (-main :nrepl-port 7888)
  (stop-http-server)
  (@server)
  )
