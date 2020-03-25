(ns codenames.sente
  (:require
   [codenames.db :as db]
   [codenames.facts :as facts]
   [codenames.sente-common :as sente-common]
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

(timbre/merge-config! {:middleware [(fn [data]
                                      (when-not (nil? *current-uid*)
                                        (*chsk-send!* *current-uid*
                                                      [::log (select-keys data [:instant
                                                                                :vargs
                                                                                :?ns-str
                                                                                :level])]))
                                      data)]})

(defmulti client-event (comp first :event))

(defmethod client-event :default
  [{:keys [event]}]
  (warn "Unknown client event: " (first event)))

(defmethod client-event :chsk/ws-ping
  [_]
  (info "ping event"))

(defn insert-facts! [uid datoms group-update?]
  (info "group-update?" group-update? uid)
  (let [user-facts  (filter (comp db/user-attributes :a) datoms)
        group-facts (filter (comp db/group-attributes :a) datoms)
        gid         (@uid->gid uid)]
    (when (seq user-facts)
      (if group-update?
        (doseq [other-uid (@gid->uids gid)]
          (facts/insert-facts! (facts/key->conn other-uid facts/initial-user-facts) user-facts)
          (when (not= other-uid uid)
            (info "SENDING FACTS: " other-uid)
            (*chsk-send!* other-uid [::facts user-facts])))
        (facts/insert-facts! (facts/key->conn uid facts/initial-user-facts) user-facts)))
    (when (and (seq group-facts) gid)
      (facts/insert-facts! (facts/key->conn gid facts/initial-group-facts) group-facts)
      (if *chsk-send!*
        (doseq [other-uid (@gid->uids uid)
                :when     (not= uid other-uid)]
          (*chsk-send!* other-uid [::facts group-facts])
          (warn "Undefined var: *chsk-send!*"))))))

(defmethod client-event ::facts
  [{[_ datoms] :event uid :uid}]
  (insert-facts! uid datoms false))

(defmethod client-event ::group-facts
  [{[_ datoms] :event uid :uid}]
  (insert-facts! uid datoms true))

(defn init-sente! []
  (info "starting client receive loop")
  (go-loop []
    (let [x (<! *ch-chsk*)
          event (:event x)]
      (if (= event ::stop)
        (info "Exiting")
        (do (client-event x)
            (recur))))))

(timbre/set-level! :info)

