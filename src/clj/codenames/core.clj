(ns codenames.core
  (:require
   [codenames.comms :as sente]
   [codenames.handlers :as handlers]
   [compojure.core :refer [GET POST defroutes]]
   [compojure.route :as route]
   [nrepl.server :refer [start-server]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [taoensso.timbre :as timbre :refer [info]]
   [org.httpkit.server :refer [run-server]]
   [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defroutes routes
  (GET  "/" [& params] handlers/home-page)
  (GET "/login" [& params] handlers/join-or-create-group)
  (GET  "/chsk"  ring-req (sente/*ring-ajax-get-or-ws-handshake* ring-req))
  (POST "/chsk"  ring-req (sente/*ring-ajax-post*                ring-req) 1 2)
  (route/resources "/")
  (route/not-found (handlers/four-oh-four)))

(def application
  (wrap-defaults routes
                 (-> site-defaults
                     (update :security assoc :anti-forgery true)
                     (assoc-in [:params :keywordize] {:parse-namespaces? true}))))


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
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond (seq errors)    (pr-str errors)
          (:help options) (prn-str summary)
          :else
          (let [{:keys [nrepl-port port]} options]
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
  (-main "--port" "3001")

  (parse-opts ["--port" "3001" "--nrepl-port" "7888"] cli-options)

  @server
  (stop-http-server)
  (@server)
  )
