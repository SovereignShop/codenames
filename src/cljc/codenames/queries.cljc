(ns codenames.queries
  (:require
   #?(:clj [datahike.core :as d]
      :cljs [datascript.core :as d])))

(defn get-user [db username]
  (d/q '[:find (pull ?id [:user/name :user/alias :user/id])
         :in $ ?username
         :where
         [?id :user/name ?username]]
       db
       username))

(defn get-group [db groupname]
  (d/q '[:find (pull ?id [:group/id :group/name :group/users])
         :in $ ?name
         :where
         [?id :group/name ?name]]
       db
       groupname))
