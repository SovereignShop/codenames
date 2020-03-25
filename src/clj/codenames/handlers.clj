(ns codenames.handlers
  (:require
   [codenames.db :as db]
   [codenames.sente :as sente]
   [hiccup.page :as h]
   [codenames.facts :as facts]
   [ring.middleware.anti-forgery :as anti-forgery]
   [datahike.core :as d]
   [taoensso.timbre :as timbre :refer [debug info warn error]]))

(def default-headers
  {"Content-type"                 "application/edn"
   "Access-Control-Allow-Methods" "GET,POST, PATCH, PUT, DELETE, OPTIONS"
   "Access-Control-Allow-Headers" "Content-Type,Access-Control-Allow-Headers,Authorization, X-Requested-With"
   "Access-Control-Allow-Origin"  "*"})

(defn init-app
  ([]
   (h/html5 {}
            [:html {:lang "en"}
             [:meta {:charset "UTF-8"}]
             [:meta {:content "IE=edge" :http-equiv "X-UA-Compatible"}]
             [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
             [:head
              [:link {:type "text/css" :rel "stylesheet" :media "screen" :href "assets/css/json.human.css"}]
              [:link {:type "text/css" :rel "stylesheet" :href "assets/css/bootstrap.css"}]
              [:link {:type "text/css" :rel "stylesheet" :href "assets/css/material-design-iconic-font.min.css"}]
              [:link {:type "text/css" :rel "stylesheet" :href "assets/css/re-com.css"}]
              [:link {:type "text/css" :rel "stylesheet" :href "assets/css/robo.css"}]]
             [:body
              [:div  {:id              "app"
                      :data-csrf-token (force anti-forgery/*anti-forgery-token*)}
               ]
              "\t\t"
              [:script {:type "text/javascript"
                        :src  "js/main.js"}]]])))

(defn common [title & body]
  {:headers {"Content-type" "text/html"
             "Access-Control-Allow-Origin" "*"}
   :status 200
   :body (init-app)})

(defn four-oh-four []
  (common "Page Not Found"
          [:div {:id "four-oh-four"}
           "The page you requested could not be found"]))

(defn create-group
  [{:keys [session params] :as req}]
  (let [{:keys [groupname username password create?]
         }        params
        facts-str (facts/write-facts-str
                   (concat (d/datoms @(facts/key->conn username facts/initial-user-facts) :eavt)
                           (d/datoms @(facts/key->conn groupname facts/initial-group-facts) :eavt)))]
    (swap! sente/gid->uids update groupname conj username)
    (swap! sente/uid->gid assoc username groupname)
    {:status  200
     :session (assoc session :uid username :gid groupname)
     :body    facts-str
     :headers default-headers}))

(defn join-group
  [{:keys [session params] :as req}]
  (let [{:keys [groupname username password create?]} params
        facts-str (facts/write-facts-str
                   (concat (d/datoms @(facts/key->conn username facts/initial-user-facts) :eavt)
                           (d/datoms @(facts/key->conn groupname facts/initial-group-facts) :eavt)))]
    (info "groupname" groupname)
    (swap! sente/gid->uids update groupname conj username)
    (swap! sente/uid->gid assoc username groupname)
    {:status  200
     :session (assoc session :uid username :gid groupname)
     :body    facts-str
     :headers default-headers}))
