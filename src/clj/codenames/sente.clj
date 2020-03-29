(ns codenames.sente
  (:require
   [codenames.db :as db]
   [codenames.facts :as facts]
   [codenames.sente-common :as sente-common]
   [codenames.queries :as queries]
   [clojure.core.async :refer [go-loop <! put!]]
   [compojure.core :refer [defroutes]]
   [compojure.core :refer [GET POST]]
   [datahike.datom :as datom]
   [datascript.transit :as dt]
   [datahike.core :as d]
   [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
   [taoensso.sente :as sente]
   [taoensso.timbre :as timbre :refer [debug info warn error]])
  (:import
   [datascript.db Datom]))

(defonce sente-vars
  (let [{:keys [ch-recv send-fn connected-uids
                ajax-post-fn ajax-get-or-ws-handshake-fn]}
        (sente/make-channel-socket! (get-sch-adapter) {:packer (sente-common/->TransitPacker)})]
    (def ^{:dynamic true} *ring-ajax-post* ajax-post-fn)
    (def ^{:dynamic true} *ring-ajax-get-or-ws-handshake* ajax-get-or-ws-handshake-fn)
    (def ^{:dynamic true} *ch-chsk*                       ch-recv) ; ChannelSocket's receive channel
    (def ^{:dynamic true} *chsk-send!*                    send-fn) ; ChannelSocket's send API fn
    (def ^{:dynamic true} *connected-uids*                connected-uids) ; Watchable, read-only atom
    ))

(defonce gid->uids (atom {}))
(defonce uid->gid (atom {}))

(def ^:dynamic *current-uid* nil)

(defmulti client-event (comp first :event))

(defmethod client-event :default
  [{:keys [event]}]
  (warn "Unknown client event: " (first event)))

(defmethod client-event :chsk/ws-ping
  [_]
  (info "ping event"))

(defn insert-facts! [username groupname datoms group-update?]
  (info "group-update?" group-update?)
  (let [user-facts  (filter (comp db/user-attributes :a) datoms)
        group-facts (filter (comp db/group-attributes :a) datoms)
        user-conn   (facts/key->conn username facts/initial-user-facts)
        group-conn  (facts/key->conn groupname facts/initial-group-facts)]
    (when (seq user-facts)
      (info "facts!" (vec user-facts))
      (if group-update?
        (doseq [{other-username :user/name} (queries/groupname->users @group-conn groupname)]
          (facts/insert-facts! (facts/key->conn other-username facts/initial-user-facts) user-facts)
          (when (not= other-username username)
            (*chsk-send!* other-username [::facts user-facts])))
        (facts/insert-facts! user-conn user-facts)))
    (when (seq group-facts)
      (facts/insert-facts! group-conn group-facts)
      (if *chsk-send!*
        (doseq [{other-username :user/name} (queries/groupname->users @group-conn groupname)
                :when                       (not= other-username username)]
          (*chsk-send!* other-username [::facts group-facts]))
        (warn "Undefined var: *chsk-send!*")))))

(defmethod client-event ::facts
  [{[_ {:keys [gid datoms]}] :event uid :uid}]
  (insert-facts! uid gid datoms false))

(defmethod client-event ::group-facts
  [{[_ {:keys [gid datoms]}] :event uid :uid}]
  (insert-facts! uid gid datoms true))

(defn init-sente! []
  (info "starting client receive loop")
  (go-loop []
    (let [x (<! *ch-chsk*)
          event (:event x)]
      (if (= event ::stop)
        (info "Exiting")
        (do (client-event x)
            (recur))))))

#_(defn tx-log-listener [{:keys [db-before db-after tx-data tx-meta]}]
  (let [facts (into [] (filter (comp db/schema-keys :a)) tx-data)
        gid   (d/q '[:find ?groupname .
                     :in $
                     :where
                     [?id :group/name ?groupname]]
                   db-after)]
    (when-not (empty? facts)
      (cond (:tx/group-update? tx-meta) (client-event [:codenames.sente/group-facts {:gid gid
                                                                                     :datoms facts}])
            :else (client-event [:codenames.sente/facts {:gid gid
                                                         :datoms facts}])))))

(timbre/set-level! :info)

