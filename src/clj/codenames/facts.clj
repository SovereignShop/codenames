(ns codenames.facts
  (:require
   [swig.core :as swig]
   [datahike.api :as d]
   [datahike.datom :as datom]
   [codenames.db :as datascript-db]
   [swig.parser :refer [hiccup->facts]]
   [byte-streams :as bs]
   [clojure.java.io :as io]
   [datascript.core :as ds]
   [datascript.transit :as dt]
   [taoensso.timbre :as timbre :refer [debug info warn error]])
  (:import
   [org.apache.commons.io FilenameUtils]))

(def db-directory "/var/lib/brain/fact-db")
(def default-uri (format "datahike:file://%s" db-directory))

(def initial-user-facts
  (into []
        cat
        [(mapv #(select-keys % [:db/ident :db/valueType :db/cardinality :db/unique])
               (into swig/full-schema (remove :prop/group datascript-db/schema)))
         (hiccup->facts datascript-db/pregame-layout)]))

(def initial-group-facts
  (into []
        cat
        [(mapv #(select-keys % [:db/ident :db/valueType :db/cardinality :db/unique])
               (filter :prop/group datascript-db/schema))]))

(defn create-db! [uri initial-tx]
  (try (let [db-dir (io/file db-directory)]
         (when-not (.exists db-dir)
           (.mkdir db-dir)))
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
      (let [uri (str default-uri "/" k ".db")]
        (try (let [db-dir (io/file db-directory)]
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

(comment
  (def base-layout (hiccup->facts datascript-db/layout))
  (d/delete-database default-uri)
  (d/datoms @(key->conn "collins") :eavt 99 :swig.split/split-percent))
