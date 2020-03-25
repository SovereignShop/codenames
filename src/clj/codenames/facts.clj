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

(defn create-db! [uri]
  (try (let [db-dir (io/file db-directory)]
           (when-not (.exists db-dir)
             (.mkdir db-dir)))
       (d/create-database uri
                          :initial-tx
                          (into []
                                cat
                                [(mapv #(select-keys % [:db/ident :db/valueType :db/cardinality :db/unique])
                                       (into swig/full-schema datascript-db/schema))
                                 (hiccup->facts datascript-db/board-layout)]))
       (d/connect uri)
       (catch clojure.lang.ExceptionInfo ex
         (case (:type (ex-data ex))
           :db-already-exists (d/connect uri)
           (throw ex)))))

(def key->conn
  (memoize
   (fn [username]
     (let [uri (str default-uri "/" username ".db")]
       (try (let [db-dir (io/file db-directory)]
              (when-not (.exists db-dir)
                (.mkdir db-dir)))
            (d/connect uri)
            (catch clojure.lang.ExceptionInfo ex
              (case (:type (ex-data ex))
                :db-does-not-exist (create-db! uri)
                (throw ex))))))))

(defn to-float-array [^bytes bytes]
  (let [dst (float-array (/ (alength bytes) Float/BYTES))
        src (bs/convert bytes java.nio.ByteBuffer)]
    (dotimes [i (alength dst)]
      (aset dst i (.getFloat src)))
    dst))

(defn to-byte-array [^floats floats]
  (let [dst (byte-array (* (alength bytes) Float/BYTES))
        src (bs/convert bytes java.nio.ByteBuffer)]
    (dotimes [i (alength dst)]
      (aset dst i (.getByte src)))
    dst))

(defn map-facts [datom-fn serializer facts]
  (mapv (fn [[e a v t added?]]
         (case a
           (:card/position :board-card/position)
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

  (d/datoms @(key->conn "collins") :eavt 99 :swig.split/split-percent)

  )
