(ns codenames.handlers
  (:require
   [codenames.db :as db]
   [hiccup.page :as h]
   [codenames.facts :as facts]
   [ring.middleware.anti-forgery :as anti-forgery]
   [datahike.core :as d]))

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
  (let [{:keys [groupname]} params
        facts-str           (facts/write-facts-str (d/datoms @(facts/key->conn "wtf14") :eavt))]
    {:status  200
     :session (assoc session :uid groupname)
     :body    facts-str
     :headers default-headers}))

(defn join-group
  [{:keys [session params] :as req}]
  (let [{:keys [groupname]} params
        facts-str           (facts/write-facts-str (d/datoms @(facts/key->conn groupname) :eavt))]
    {:status  200
     :session (assoc session :uid groupname)
     :body    facts-str
     :headers default-headers}))
