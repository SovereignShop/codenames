(ns codenames.db
  (:require
   [datascript.core :as d]
   [swig.core :as swig :refer [view tab split window full-schema]]
   [taoensso.timbre :as timbre :refer [debug info warn]]))

(def board-dimensions [5 5]) 
(def board-size (apply * board-dimensions))

(def deck #{"Home" "Dog"})

(def game-state
  [{:player/name "John"
    :player/color :red
    :player/type :guesser}
   {:player/name "David"
    :player/color :blue
    :plyaer/type :codemaster}
   {:card/word "Tekxas"
    :card/color "Blue"
    :card/played? false
    :card/position [0 0]}])

;; # Layouts

;; ## Player Login Layout

(def login-layout
  (swig/view {:swig/ident :views/main-view}
             (swig/window {:swig/ident :modal/login})))

;; ## Team selection Layout

(def team-selection-layout
  (swig/view {:swig/ident :views/main-view}
             (swig/split {})))

;; ## Primary Game Layout

(def board-layout
  (swig/view  {:swig/ident :views/main-view}
              (swig/split {:swig/ident :splits/main-split}
                          (swig/split {}
                                      (swig/tab {})
                                      (swig/tab {}))
                          (swig/tab {}))))

(def schema-keys
  (into #{} (map :db/ident) full-schema))

(defonce default-db (into [] cat [game-state]))

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
