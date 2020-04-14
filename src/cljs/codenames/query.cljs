(ns codenames.query
  (:require
   [codenames.constants.ui-idents :as idents]
   [codenames.constants.ui-views :as views]
   [cljs.core.async :refer [chan put!]]
   [clojure.string :as s]
   [datascript.transit :as dt]
   [goog.events :as ev]
   [goog.net.EventType]
   [goog.string :as gstr]
   [re-posh.core :as re-posh]
   [taoensso.timbre :refer-macros [debug info warn error]]))

(defn url-encode
  [string]
  (some-> string str
          (js/encodeURIComponent)
          (.replace "+" "%20")))

(defn format-query-params
  [params]
  (when params
    (->> params
         (mapv (fn [[k v]]
                 (str (if-let [n (namespace k)]
                        (str n "/")
                        "")
                      (name k)
                      "="
                      (url-encode v)
                      "&")))
         (apply str "?"))))

(defn request
  [{:keys [uri method data params success error headers edn? sign-key]
    :or {edn? true method :get}}]
  (let [req     (goog.net.XhrIo.)
        method  (s/upper-case (name method))
        uri     (str uri (format-query-params params))]
    (info :request params)
    (when success
      (ev/listen req goog.net.EventType/SUCCESS #(success req)))
    (when error
      (ev/listen req goog.net.EventType/ERROR #(error req))
      (ev/listen req goog.net.EventType/TIMEOUT #(error req)))
    (println "headers:" headers)
    (.send req uri method data
           (when headers (clj->js headers))
           30)))

(defn do-request
  ([query]
   (do-request query false))
  ([query datoms?]
   (let [q-str (prn-str query)
         result (chan)]
     (request
      {:uri (gstr/format "%squery" js/window.location.href)
       :method :get
       :edn? false
       :params {:query q-str :datoms? datoms?}  ;; TODO: GET request body?
       :success (fn [resp]
                  (info :success)
                  (let [results-str (.getResponseText resp)]
                    (info ::do-query :string/count (count results-str))
                    (let [results (dt/read-transit-str results-str)]
                      (info "TESTING" (type results) (keys (first results)))
                      (put! result results)
                      (re-posh/dispatch [:codenames.events.queries/cache-query query]))))
       :error (fn [resp]
                (let [err (.getResponseText resp)]
                  (js/console.log err)
                  (put! result err)))})
     result)))

(defn send-request
  ([{:keys [url endpoint]
     :or {url js/window.location.href
          endpoint "/query"}
     :as opts}]
   (let [result (chan)]
     (request
      (merge {:uri (gstr/format "%s%s" url endpoint)
              :method :get
              :edn? false
              :success (fn [resp]
                         (info :success)
                         (let [results-str (.getResponseText resp)]
                           (info ::do-query :string/count (count results-str))
                           (put! result results-str)))
              :error (fn [resp]
                       (let [err (.getResponseText resp)]
                         (js/console.log err)
                         (put! result err)))}
             opts))
     result)))

(defn do-login [credentials]
  (re-posh/dispatch [:codenames.events.app-state/login-waiting])
  (request
   {:uri     (gstr/format "%slogin" js/window.location.href)
    :method  :get
    :edn?    false
    :params  credentials
    :success (fn [resp]
               (re-posh/dispatch [:codenames.events.app-state/login-success])
               (let [datoms (into [[:db/retractEntity [:swig/ident :swig/root-view]]
                                   [:db/retractEntity [:swig/ident idents/modal-dialog]]
                                   [:db/retractEntity [:swig/ident idents/modal-dialog]]]
                                  (dt/read-transit-str (.getResponseText resp)))
                     datoms (conj datoms
                                  {:swig/ident       idents/main-popover
                                   :swig/type        :swig.type/window
                                   :swig.ref/parent  [:swig/ident views/root-view]
                                   :popover/showing? false})]
                 (info "Login succeeded: " datoms)
                 (re-posh/dispatch [:codenames.events.facts/add-facts datoms true])))
    :error   (fn [resp]
               (re-posh/dispatch [:codenames.events.app-state/login-fail])
               (warn "Login failed"))}))
