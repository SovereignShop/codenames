(ns codenames.handlers
  (:require
   [codenames.utils :as utils]
   [codenames.queries :as queries]
   [codenames.comms :as sente]
   [datascript.core :as ds]
   [hiccup.page :as h]
   [codenames.facts :as facts]
   [ring.middleware.anti-forgery :as anti-forgery]
   [datahike.core :as d]
   [taoensso.timbre :as timbre :refer [debug info warn error]]))

(defn home-page [_]
  {:headers {"Content-Type" "text/html"}
   :status 200
   :body
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
              [:link {:type "text/css" :rel "stylesheet" :href "assets/css/robo.css"}]
              [:link {:type "text/css" :rel "stylesheet" :href "assets/css/custom-styles.css"}]]
             [:body
              [:div  {:id              "app"
                      :data-csrf-token (force anti-forgery/*anti-forgery-token*)}
               ]
              "\t\t"
              [:script {:type "text/javascript"
                        :src  "js/dev/main.js"}]]])})

(defn four-oh-four []
  {:headers {"Content-type" "text/html"}
   :status 200
   :body
   (h/html5 {}
            [:div {:id "four-oh-four"}
             "The page you requested could not be found"])})

(defn transact!
  "TODO: Fix hack"
  [conn uid gid facts]
  (let [{:keys [tx-data] :as tx} (d/transact! conn facts)]
    (sente/client-event
     {:uid uid
      :event [:codenames.comms/group-facts
              {:gid gid
               :datoms (map (partial apply ds/datom) tx-data)}]})
    tx))

(defn join-or-create-group
  [{:keys [session params]}]
  (let [{:keys [groupname username]
         }                params
        user-conn         (facts/key->conn username facts/initial-user-facts)
        group-conn        (facts/key->conn groupname facts/initial-group-facts)
        user              (queries/get-user @group-conn username)
        group             (queries/get-group @group-conn groupname)
        user-id           (or (:db/id user) -1)
        group-id          (or (:db/id group) -2)
        {:keys [tempids]} (transact! group-conn
                                     username
                                     groupname
                                     [(assoc (or user (utils/make-user username)) :db/id user-id)
                                      (assoc (or group (utils/make-group groupname))
                                             :db/id group-id
                                             :group/users [user-id])])
        _                 (d/transact! user-conn
                                       [(utils/make-session
                                         (or (:db/id user) (tempids user-id))
                                         (or (:db/id group) (tempids group-id))
                                         groupname)])
        facts-str         (facts/write-facts-str
                           (concat (d/datoms @user-conn :eavt)
                                   (d/datoms @group-conn :eavt)))]
    {:status  200
     :session (assoc session :uid username :gid groupname)
     :body    facts-str
     :headers {"Content-type" "application/edn"}}))
