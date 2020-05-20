(ns codenames.facts
  "Small utilities on top of datahike to handle mapping to/from datascript
   facts."
  (:require
   [codenames.constants.ui-tabs :as tabs]
   [swig.core :as swig]
   [datahike.api :as d]
   [datahike.datom :as datom]
   [codenames.db :as datascript-db]
   [swig.parser :refer [hiccup->facts]]
   [clojure.java.io :as io]
   [datascript.core :as ds]
   [datascript.transit :as dt]
   [taoensso.timbre :as timbre :refer [error]]))

(def ^{:dynamic true} *db-directory* nil)
(def ^{:dynamic true} *default-uri* nil)

(def initial-user-facts
  (into []
        cat
        [(mapv #(select-keys % [:db/ident :db/valueType :db/cardinality :db/unique])
               (into swig/full-schema (remove :prop/group datascript-db/schema)))
         (hiccup->facts datascript-db/pregame-layout)
         [[:db.fn/retractAttribute [:swig/ident tabs/game] :swig.ref/parent]]]))

(def initial-group-facts
  (into []
        cat
        [(mapv #(select-keys % [:db/ident :db/valueType :db/cardinality :db/unique])
               (filter :prop/group datascript-db/schema))]))

(defn create-db! [uri initial-tx]
  (try (let [db-dir (io/file *db-directory*)]
         (when-not (.exists db-dir)
           (.mkdirs db-dir)))
       (d/create-database uri :initial-tx initial-tx)
       (d/connect uri)
       (catch clojure.lang.ExceptionInfo ex
         (case (:type (ex-data ex))
           :db-already-exists (d/connect uri)
           (throw ex)))))

(def key->conn
  (memoize
   (fn
     ([k initial-tx]
      (let [uri (str *default-uri* "/" k ".db")]
        (try (let [db-dir (io/file *db-directory*)]
               (when-not (.exists db-dir)
                 (.mkdir db-dir)))
             (d/connect uri)
             (catch clojure.lang.ExceptionInfo ex
               (case (:type (ex-data ex))
                 :db-does-not-exist (create-db! uri initial-tx)
                 (throw ex)))))))))

(defn map-facts [datom-fn serializer facts]
  (mapv (fn [[e a v t added?]]
          (case a
            (:codenames.character-card/position :codenames.word-card/position)
            (datom-fn e a (serializer v) t added?)
            (datom-fn e a v t added?)))
        facts))

(defn insert-facts! [conn facts]
  (try
    (d/transact conn (map-facts datom/datom dt/write-transit-str facts))
    (catch Exception e
      (error e))))

(defn write-facts-str [facts]
  (dt/write-transit-str (map-facts ds/datom dt/read-transit-str facts)))
