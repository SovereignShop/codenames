(ns  codenames.comms
  "Codenames "
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require
   [codenames.constants.ui-idents :as idents]
   [codenames.events.server :as server-events]
   [codenames.comms-common :refer [TransitPacker]]
   [re-frame.core :as re-frame]
   [re-posh.core :as re-posh]
   [datascript.transit :as dt]
   [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
   [cljs.core.async :as async :refer (<! >! put! chan take!)]
   [taoensso.sente  :as sente :refer (cb-success?)]
   [cljs.core.match :refer [match]]))

(defonce ^:dynamic *chsk* nil)
(defonce ^:dynamic *ch-chsk* nil)
(defonce ^:dynamic *chsk-send!* nil)
(defonce ^:dynamic *chsk-state* nil)
(defonce running (atom true))

(defmulti server-event first)

(defmethod server-event ::facts
  [[_ {:keys [datoms tx-meta]}]]
  (re-posh/dispatch [:codenames.events.facts/add-facts (with-meta datoms tx-meta) true]))

(defmethod server-event :codenames.comms/started-processing
  [_]
  (re-posh/dispatch [::server-events/set-server-status [:swig/ident idents/server-events] true]))

(defmethod server-event :codenames.comms/finished-processing
  [_]
  (re-posh/dispatch [::server-events/set-server-status [:swig/ident idents/server-events] false]))

(defmethod server-event :chsk/ws-ping [_] nil)

(defn init-sente! []
  (let [?csrf-token
        (when-let [el (.getElementById js/document "app")]
          (.getAttribute el "data-csrf-token")) 
        {:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! "/chsk" ?csrf-token {:type :auto
                                                         :packer (TransitPacker.)
                                                         })]
    (set! *chsk*       chsk)
    (set! *ch-chsk*    ch-recv) ; ChannelSocket's receive channel
    (set! *chsk-send!* send-fn) ; ChannelSocket's send API fn
    (set! *chsk-state* state)   ; Watchable, read-only atom
    (go-loop []
      (let [x (<! *ch-chsk*)]
        (try
          (match (:event x)
                 [:chsk/recv event] (server-event event)
                 :else (println "received:" (:event x)))
          (catch js/Error e
            (js/console.error e)))
        (when @running (recur))))))

(defn send-event! [event]
  (if *chsk-send!*
    (*chsk-send!* event)
    (warnf "No send function")))

(defn server-event-loop []
  (let [receive-channel (async/chan 1000)
        force-send-ch (async/chan 10)]
    (go-loop []
      (let [results-channel (chan 1000)
            results (async/pipe receive-channel results-channel)
            tx-data (do (<! (async/alts! (async/timeout 10000)
                                         force-send-ch))
                        (async/into [] results-channel))]
        (async/close! results-channel)
        (send-event! [::facts tx-data])
        (recur)))
    [receive-channel force-send-ch]))

(timbre/merge-config!
 {:middleware []})

(timbre/set-level! :info)
