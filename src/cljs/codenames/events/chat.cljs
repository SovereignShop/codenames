(ns codenames.events.chat
  (:require
   [cljs.core.async :refer [go timeout <!]]
   [codenames.constants.ui-idents :as idents]
   [swig.macros :refer [def-event-ds]]
   [cljs-time.core :as time]
   [cljs-time.coerce :as tc]
   [datascript.core :as d]))

(defn update-scroller! []
  (let [scroller        (.getElementById js/document "chat-scroller")
        chatbox         (.getElementById js/document "chat-box-textarea")
        chatbox-height  (.-offsetHeight chatbox)
        scroll-distance (- (.-scrollHeight scroller)
                           (.-scrollTop scroller)
                           (.-offsetHeight scroller)
                           chatbox-height)]
    (when (< scroll-distance 40)
      (go (<! (timeout 25))
          (set! (.-scrollTop scroller) (.-scrollHeight scroller))))))

(def-event-ds ::new-message
  [db [_ msg]]
  (let [user (-> db (d/entity [:swig/ident idents/session]) :session/user :db/id)]
    (with-meta
      [{:chat/message msg :chat/time (tc/to-date (time/now)) :chat/user user}]
      {:db.transaction/log-message? true})))
