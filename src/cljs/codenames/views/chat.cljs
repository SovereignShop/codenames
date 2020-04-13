(ns codenames.views.chat
  "Adapted from: https://github.com/rauhs/klang/blob/master/src/cljs/klang/core.cljs"
  (:require
   [swig.views :as swig-view]
   [cljs.core.async :refer [go timeout]]
   [codenames.subs.chat :as chat-subs]
   [codenames.events.chat :as chat-events]
   [cljsjs.highlight]
   [cljsjs.highlight.langs.clojure]
   [codenames.constants.ui-idents :as idents]
   [codenames.constants.ui-tabs :as tabs]
   [re-posh.core :as re-posh]
   [goog.events :as gevents]
   [goog.object :as gobj]
   [goog.string :as gstring]
   [goog.string.format]
   [goog.style :as gstyle]
   [markdown.core :as md]
   [reagent.core :as r]
   [goog.events.KeyCodes]
   [re-com.core :refer [box scroller h-box v-box input-text input-textarea throbber gap]]))

(defn msg->str
  "Converts a message to a string."
  [msg]
  (let [s (pr-str msg)]
    ;; Remove closing and opening bracket of the vectorized message:
    (.substr s 1 (- (.-length s) 2))))

(defn h [& args]
  (.apply js/React.createElement js/React.createElement (into-array args)))

(defn render-msg
  [msg]
  (h "span" #js{"dangerouslySetInnerHTML" #js{"__html" (md/md->html (first msg))}}))

(defn server-log-tab []
  (let [msg @(re-posh/subscribe [::chat-subs/get-current-msg [:swig/ident idents/chat]])
        level (or (:level msg) "")
        vargs (:vargs msg)]
    [h-box
     :gap "10px"
     :children
     [[:div {:style {:width "18px"}}]
      [throbber
       :size :smaller
       :color "red"]
      [:span {:style {:color "steelblue"}} (.toUpperCase (name level))]
      [:span {:style {:flex "1 1 0%"
                      :height "18px"
                      :max-width "1200px"
                      :overflow "hidden"}} (render-msg (first vargs))]
      [:div {:style {:width "40px"}}]]]))

(defonce db (atom {:showing? true
                   :max-logs 500
                   :search ""
                   :logs #js[]
                   :frozen-at nil}))


(def autofocus? (atom true))

(defn !!
  [& args]
  (apply swap! db args))

(defn dom-el
  "Ensures there is a div element in the body and returns it."
  []
  (let [domid "__klang__id__"
        domel (js/document.getElementById domid)]
    domel))

(defn possibly-set-lifecycle!
  "This is all done for performance... Smaller and more used functions can easier get optimized."
  [spec name f]
  (when-not (nil? f)
    (gobj/set spec name f))
  nil)

(defn build-class
  "The render function will always be called with 1 arg, the rum state.
   It should return [dom rum-state]."
  [render lcm]
  (let [constr        (fn [props]
                        (this-as this
                          ;; Call parent constructor:
                          (.call js/React.Component this props)
                          (set! (.-props this) props)
                          (set! (.-state this) #js{:comp this})
                          this))
        should-update (aget lcm "should-update") ;; old-state state -> boolean
        will-unmount  (aget lcm "will-unmount")  ;; state -> state
        will-mount    (aget lcm "will-mount")    ;; state -> state
        will-update   (aget lcm "will-update")   ;; state -> state
        did-update    (aget lcm "did-update")    ;; state -> state
        did-mount     (aget lcm "did-mount")     ;; state -> state
        class-props   (aget lcm "class-properties")] ;; custom properties+methods
    (goog/inherits constr js/React.Component)
    ;; Displayname gets set on the constructor itself:
    (gobj/set constr "displayName" (aget lcm "name"))
    (let [proto (.-prototype constr)]
      (gobj/extend proto #js{:render (fn []
                                       (this-as this
                                         (apply render (aget (.. this -props) "props"))))})
      (possibly-set-lifecycle! proto "componentWillMount" will-mount)
      (possibly-set-lifecycle! proto "componentDidMount" did-mount)
      (possibly-set-lifecycle! proto "componentDidUpdate" did-update)
      (possibly-set-lifecycle! proto "componentWillUnmount" will-unmount)
      (when (some? will-update)
        (gobj/set proto "componentWillUpdate" will-update))
      (when (some? should-update)
        (gobj/set proto "shouldComponentUpdate" should-update))
      (when (some? class-props)
        (when-some [cp (clj->js (apply merge class-props))]
          (gobj/extend proto cp)))
      constr)))

(defn component
  [lcm render]
  (let [cls (build-class render lcm)
        key-fn (aget lcm "key-fn")]
    (fn component [& props]
      (let [react-props (if (some? key-fn)
                          #js{:props props
                              :key (apply key-fn props)}
                          #js{:props props})]
        (js/React.createElement cls react-props)))))

(defn mount
  "Add component to the DOM tree. Idempotent. Subsequent mounts will just update component"
  [component node]
  (js/ReactDOM.render component node))

(defn unmount
  "Removes component from the DOM tree"
  [node]
  (js/ReactDOM.unmountComponentAtNode node))

(defonce id-counter 0)

(defn gens
  "Generates a new log id."
  []
  (set! id-counter (inc id-counter))
  id-counter)

(defn format-time
  [d]
  (when (instance? js/Date d)
    (.slice (aget (.split (.toJSON d) "T") 1) 0 -1)))

(def render-log-event
  "Renders a single log message."
  (delay
    (component
      #js{:name "LogEvent"
          :key-fn (fn [props] (:id props))
          :should-update #(-> false)}
      (fn [{:keys [time ns type msg] :as lg-ev}]
        (h "li" #js{:style #js{:listStyleType "none"}}
           (format-time time)
           " "
           ns
           (when-not (empty? ns) "/")
           (h "span" #js{:style #js{:color "steelblue"}} type)
           " "
           (h "span" #js{:style #js{:cursor "pointer"}}
              (render-msg msg)))))))

(defn search-filter-fn
  "Returns a transducer that filters given the log messages according to the
  search term given in the database for the current active tab.
  Does a full text search on time, namespace, type and message.
  The format is like:
  11:28:27.793 my.ns/INFO [\"Log msg 0\"]"
  [search]
  (let [search (.replace search " " ".*")
        re (try (js/RegExp. search "i")
                (catch :default _ (js/RegExp "")))]
    (fn [lg-ev]
      (let [log-str (.join
                      #js[(format-time (:time lg-ev))
                          " "
                          (:ns lg-ev)
                          (when-not (empty? (:ns lg-ev)) "/")
                          (:type lg-ev) " "
                          (str (:msg lg-ev))]
                      "")
            test (.test re log-str)]
        ;; The .test might be undefined if the search str is empty?
        (if (undefined? test)
          true ;; Include all of them then
          test)))))

(defn render-logs
  "Renders an array of log messages."
  [logs]
  (h "ul" #js{:style #js{:padding    ".5em"
                         :margin     "0em"
                         :lineHeight "1.06em"}}
     (let [frozen-idx    (:frozen-at @db)
           last-to-start (if (some? frozen-idx)
                           frozen-idx
                           (count logs))
           search        (not-empty (:search @db))
           filter-fn     (if search
                           (search-filter-fn search)
                           identity)

           aout #js[]]
       (dotimes [i last-to-start]
         (let [lg-ev (aget logs i)]
           (when ^boolean (filter-fn lg-ev)
             (.push aout (@render-log-event lg-ev)))))
       aout)))

(def render-search-box
  (delay
    (let [search-box-id "klang-search"]
      (component #js{:name "KlangSearch"
                     :key-fn (fn [props] (:id props))
                     :did-mount (fn [state]
                                  (when @autofocus?
                                    (let [el (js/document.getElementById search-box-id)]
                                      (.select el)))
                                  state)}
                 (fn [default-value]
                   (h "input" #js{:style #js{:background "black"
                                             :color "white"
                                             :width "350px"}
                                  :id search-box-id
                                  :tabIndex 1
                                  :onChange (fn [e] (!! assoc :search (.. e -target -value)))
                                  :autoFocus @autofocus?
                                  :type "text"
                                  :defaultValue default-value
                                  :placeholder "Search"}))))))

(defn- render-overlay
  "Renders the entire log message overlay in a div when :showing? is true."
  []
  (h "div" #js{:style #js{:display    (if (:showing? @db) "block" "none")
                          ;;           :width "calc(100% - 12px)"
                          ;;           :height "calc(100% - 12px)"
                          :fontFamily "monospace"
                          :zIndex     9922 ;; fighweel has 10k
                          :fontSize   "90%"}}
     (h "div" #js{:style #js{:height         "28px"
                             :width          "calc(100% - 12px)"
                             :justifyContent "center"
                             :display        "flex"}}
        (@render-search-box (:search @db "")))
     (h "div" #js{:style #js{:width      "calc(100% - 12px)"
                             :height     "calc(100% - 40px)"
                             :color      "#fff"
                             :padding    0
                             :outline    "none"
                             :background "black"
                             :zIndex     9922 ;; fighweel has 10k
                             :overflowY  "auto"}}
        (render-logs (:logs @db)))))

;; Taken from highlight-js
(defn css-molokai
  []
  ".hljs {
  display: block;
  overflow-x: auto;
  padding: 0.2em;
  background: black;
  -webkit-text-size-adjust: none;
}
.hljs,.hljs-tag,.css .hljs-rule,.css .hljs-value,.css .hljs-function .hljs-preprocessor,
.hljs-pragma {
  color: #f8f8f2;
}
.hljs-strongemphasis,.hljs-strong,.hljs-emphasis {
  color: #a8a8a2;
}
.hljs-bullet,.hljs-blockquote,.hljs-horizontal_rule,.hljs-number,.hljs-regexp,
.alias .hljs-keyword,.hljs-literal,.hljs-hexcolor {
  color: #ae81ff;
}
.hljs-tag .hljs-value,.hljs-code,.hljs-title,.css .hljs-class,
.hljs-class .hljs-title:last-child {
  color: #a6e22e;
}
.hljs-link_url {
  font-size: 80%;
}
.hljs-strong,.hljs-strongemphasis {
  font-weight: bold;
}
.hljs-emphasis,.hljs-strongemphasis,.hljs-class .hljs-title:last-child,.hljs-typename {
  font-style: italic;
}
.hljs-keyword,.hljs-function,.hljs-change,.hljs-winutils,.hljs-flow,.hljs-header,.hljs-attribute,
.hljs-symbol,.hljs-symbol .hljs-string,.hljs-tag .hljs-title,.hljs-value,.alias .hljs-keyword:first-child,
.css .hljs-tag,.css .unit,.css .hljs-important {
  color: #f92672;
}
.hljs-function .hljs-keyword,.hljs-class .hljs-keyword:first-child,.hljs-aspect .hljs-keyword:first-child,
.hljs-constant,.hljs-typename,.hljs-name,.css .hljs-attribute {
  color: #66d9ef;
}
.hljs-variable,.hljs-params,.hljs-class .hljs-title,.hljs-aspect .hljs-title {
  color: #f8f8f2;
}
.hljs-string,.hljs-subst,.hljs-type,.hljs-built_in,.hljs-attr_selector,.hljs-pseudo,.hljs-addition,
.hljs-stream,.hljs-envvar,.hljs-prompt,.hljs-link_label,.hljs-link_url {
  color: #e6db74;
}
.hljs-comment,.hljs-javadoc,.hljs-annotation,.hljs-decorator,.hljs-pi,.hljs-doctype,.hljs-deletion,
.hljs-shebang {
  color: #75715e;
}")


(defn set-max-logs!
  "Only keep the last n logs. If nil: No truncating."
  [n]
  (!! assoc :max-logs n))

(defn possibly-truncate
  [db]
  (when-some [num (:max-logs db)]
    (let [logs (:logs db)]
      (.splice logs 0 (- (alength logs) num)))))

(def  rAF js/window.requestAnimationFrame)
(def scheduled? false)

(defn request-rerender!
  []
  (when-not scheduled?
    (set! scheduled? true)
    (rAF
     (fn []
       (possibly-truncate @db)
       (mount (render-overlay) (dom-el))
       (set! scheduled? false)))))

(def ensure-klang-init
  "This will get DCE'd!"
  (delay
    (when-not (exists? js/React)
      (js/console.error "Klang: Can't find React. Load by yourself beforehand."))
    (set-max-logs! 500)
    (add-watch db :rerender request-rerender!)
    (gstyle/installStyles (css-molokai))))

(defn add-log!
  [ns username msg0 & msg]
  (deref ensure-klang-init)
  (let [db   @db
        meta (::meta-data msg0)
        msg  (if (some? meta) (vec msg) (into [msg0] msg))]
    (.push (:logs db) {:time (js/Date.)
                       :id   (gens)
                       :ns   (str ns)
                       :type (name username)
                       :meta meta ;; Potentially nil
                       :msg  msg})
    (request-rerender!)
    (if (pos? (count msg))
      (last msg)
      msg0)))

(defn clear!
  "Clears all logs"
  []
  (set! (:logs @db) -length 0)
  (request-rerender!))

(defmethod swig-view/dispatch idents/chat
  [tab]
  (let [shift-pressed? (r/atom false)
        model (r/atom "")
        rows (r/atom 1)]
    [(fn []
       (request-rerender!)
       [v-box
        :style {:flex "1 1 0%"}
        :children
        [[scroller
          :style {:flex "1 1 0%"}
          :attr {:id "chat-scroller"}
          :child
          [:div {:id "__klang__id__"
                 :style {:flex "1 1 0%"
                         :background-color "black"}}
           "Hello"]]
         [:div {:id "chat-box-textarea"}
          [input-textarea
           :model model
           :rows @rows
           :width "100%"
           :attr {:on-key-down (fn [event]
                                 (if (and (.getModifierState event "Shift")
                                          (= (.-which event) goog.events.KeyCodes/ENTER))
                                   (swap! rows inc)
                                   (when (= (.-which event) goog.events.KeyCodes/ENTER)
                                         (do (reset! rows 1)
                                             (.preventDefault event)
                                             (re-posh/dispatch [::chat-events/new-message
                                                                (-> event .-target .-value)])
                                             (reset! model "")))))
                  }
           :change-on-blur? false
           :on-change #(reset! model %)]]]])]))
