(ns codenames.core
  (:require
   [codenames.comms :as sente]
   [codenames.handlers :as handlers]
   [codenames.facts :as facts]
   [compojure.core :refer [GET POST defroutes]]
   [compojure.route :as route]
   [clojure.java.io :as io]
   [nrepl.server :refer [start-server]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [taoensso.timbre :as timbre :refer [info]]
   [org.httpkit.server :refer [run-server]]
   [clojure.tools.cli :refer [parse-opts]])
  (:gen-class)
  (:import [java.util Date]))

(defroutes routes
  (GET  "/" [& _] handlers/home-page)
  (GET "/login" [& _] handlers/join-or-create-group)
  (GET  "/chsk"  ring-req (sente/*ring-ajax-get-or-ws-handshake* ring-req))
  (POST "/chsk"  ring-req (sente/*ring-ajax-post*                ring-req) 1 2)
  (route/resources "/")
  (route/not-found (handlers/four-oh-four)))

(def application
  (wrap-defaults routes
                 (-> site-defaults
                     (update :security assoc :anti-forgery true)
                     (assoc-in [:params :keywordize] {:parse-namespaces? true}))))

(defn create-app [db-dir]
  (fn [req]
    (binding [facts/*db-directory* db-dir
              facts/*default-uri* (format "datahike:file://%s" db-dir)]
      (application req))))

(create-app "/tmp")

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
   ["-d" "--db-dir" "Root Database Directory"
    :default "/tmp"
    :validate [#(.isDirectory (io/file %)) "Must be an existing directory"]]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond (seq errors)    (pr-str errors)
          (:help options) (prn-str summary)
          :else
          (let [{:keys [nrepl-port port db-dir]} options]
            (reset! server (run-server (create-app db-dir) {:port port}))
            (when nrepl-port
              (start-server :port nrepl-port)
              (info (format "started nrepl. Port=%s" nrepl-port)))
            (sente/init-sente! db-dir)))))

(defn stop-http-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(comment
  (-main "--port" "3001")

  (parse-opts ["--port" "3001"] cli-options)

  @server
  (stop-http-server)
  (@server)
  )
