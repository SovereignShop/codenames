(ns codenames.comms-common
  (:require
   [taoensso.sente.interfaces :as interfaces]
   [datascript.transit :as dt]))

(deftype TransitPacker []
  interfaces/IPacker
  (pack [_ x] (dt/write-transit-str x))
  (unpack [_ x] (dt/read-transit-str x)))
