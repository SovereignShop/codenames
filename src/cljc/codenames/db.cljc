(ns codenames.db
  (:require
   [codenames.constants.ui-idents :as idents]
   [codenames.constants.ui-views :as views]
   [datascript.core :as d]
   [swig.core :as swig :refer [view tab split window full-schema]]
   [taoensso.timbre :as timbre :refer [debug info warn]]))

#?(:cljs (goog-define HOSTNAME "http://localhost"))
#?(:cljs (goog-define PORT "3001"))

(def board-dimensions [5 5])
(def board-size (apply * board-dimensions))

(def deck #{"Home" "Dog"})

(def game-state
  [{:player/name "John"
    :player/color "blue"
    :player/type "guesser"}
   {:player/name "David"
    :player/color "blue"
    :plyaer/type ""}
   {:card/word "Texas"
    :card/color "Blue"
    :card/played? false
    :card/position [0 0]}])

(def extras
  [{:swig/ident              idents/fullscreen-view
    :fullscreen-view/view-id views/main-view}
   {:app/type      :type/app-meta
    :app-meta/name "Assist Analysis"}
   {:swig/ident       :user-login
    :app/type         :type/user-login
    :user-login/state :unauthenticated}
   {:app/type       :type/ui.scale
    :swig/ident     :routes-scale-factor
    :ui.scale/value 100}
   {:swig/ident        :query-error
    :app/type          :type/query-error
    :query-error/error ""
    :query-error/tag   ::none}
   {:swig/ident :selection
    :app/type   :type/selection}
   {:swig/ident         :query-status
    :app/type           :type/query-status
    :query-status/state :received}
   {:swig/ident idents/server-events}])

(def login-layout
  (swig/view {:swig/ident :swig/main-view}
             (swig/window {:swig/ident idents/login-window})
             (swig/window {:swig/ident idents/modal-dialog})))

(def team-selection-layout
  (swig/view {:swig/ident :swig/main-view}
             (swig/split {})))

(def board-layout
  (swig/view  {:swig/ident :swig/main-view}
              (swig/split {:swig/ident :splits/main-split}
                          (swig/split {}
                                      (swig/tab {})
                                      (swig/tab {}))
                          (swig/tab {}))))

(def schema-keys
  (into #{} (map :db/ident) full-schema))

(defonce default-db (into [] cat [game-state extras]))

(def schema
  [{:db/ident :transaction/tx-meta :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :transaction/tx-added :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :transaction/tx-entities :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :ui/type :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :popover/showing? :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}
   {:db/ident :app/type :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}])

(def ds-schema {:db/ident                 {:db/unique :db.unique/identity}
                :swig/ident               {:db/unique :db.unique/identity}
                :swig.ref/parent          {:db/valueType :db.type/ref}
                :swig.ref/previous-parent {:db/valueType :db.type/ref}
                :swig.view/active-tab     {:db/valueType :db.type/ref}
                :swig.tab/label           {:db/valueType :db.type/ref}
                :swig.split/view-1        {:db/valueType :db.type/ref}
                :swig.split/view-2        {:db/valueType :db.type/ref}
                :swig.tab/view-id         {:db/valueType :db.type/ref}
                :swig.view/tab-ids        {:db/cardinality :db.cardinality/many}
                :swig.tab/ops             {:db/cardinality :db.cardinality/many
                                           :db/valueType   :db.type/ref}
                :swig.view/ops            {:db/cardinality :db.cardinality/many
                                           :db/valueType   :db.type/ref}
                :swig.split/ops           {:db/cardinality :db.cardinality/many
                                           :db/valueType   :db.type/ref}})

(defonce conn (d/create-conn ds-schema))
