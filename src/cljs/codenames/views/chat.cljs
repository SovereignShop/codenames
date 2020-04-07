(ns codenames.views.chat
  (:import
   goog.ui.KeyboardShortcutHandler)
  (:require
   [swig.views :as swig-view]
   [codenames.subs.chat :as chat-subs]
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
   [re-com.core :refer [box scroller h-box throbber gap]]))

(defn hl-clj-str
  "Returns a string containing HTML that highlights the message. Takes a string
  of clojure syntax. Such as map, set etc.
  Ex:
  (hl-clj-str \"{:foo :bar}\")"
  [msg]
  (.-value (.highlight js/hljs "clojure" msg true)))

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
  (h "span" #js{"dangerouslySetInnerHTML" #js{"__html" (hl-clj-str (msg->str msg))}}))

(defn severity->color
  "Returns a color for the given severity
   http://www.w3schools.com/cssref/css_colornames.asp"
  [severity]
  (println "severity:" severity)
  (case (name (or severity ""))
    "DEBG" "gray"
    "TRAC" "darkgray"
    "INFO" "steelblue"
    "info" "steelblue"
    "ERRO" "darkred"
    "error" "darkred"
    "CRIT" "red"
    "FATA" "firebrick"
    "WARN" "orange"
    nil))

(defn server-log-tab []
  (let [processing? (re-posh/subscribe [::chat-subs/processing? [:swig/ident idents/chat]])
        msg @(re-posh/subscribe [::chat-subs/get-current-msg [:swig/ident idents/chat]])
        level (or (:level msg) "")
        vargs (:vargs msg)]
    [h-box
     :gap "10px"
     :children
     [[:div {:style {:width "18px"}}]
      [throbber
       :style {:visibility (if @processing? "visible" "hidden")}
       :size :smaller :color "red"]
      [:span {:style {:color (severity->color level)}} (.toUpperCase (name level))]
      [:span {:style {:flex "1 1 0%"
                      :height "18px"
                      :max-width "1200px"
                      :overflow "hidden"}} (render-msg vargs)]
      [:div {:style {:width "40px"}}]]]))

(defmethod swig-view/dispatch idents/chat
  []
  [server-log-tab])

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
  (let [constr (fn [props]
                 (this-as this
                   ;; Call parent constructor:
                   (.call js/React.Component this props)
                   (set! (.-props this) props)
                   (set! (.-state this) #js{:comp this})
                   this))
        should-update (aget lcm "should-update") ;; old-state state -> boolean
        will-unmount (aget lcm "will-unmount") ;; state -> state
        will-mount (aget lcm "will-mount") ;; state -> state
        will-update (aget lcm "will-update") ;; state -> state
        did-update (aget lcm "did-update") ;; state -> state
        did-mount (aget lcm "did-mount") ;; state -> state
        class-props (aget lcm "class-properties")] ;; custom properties+methods
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

(defn toggle-freeze
  "Freezes the UI. Toggle on no param"
  []
  (!! update :frozen-at (fn [idx]
                          (if (some? idx)
                            nil
                            id-counter))))

(defn toggle-showing!
  "Makes the overlay show/hide. Toggle on no param"
  ([]
   (!! update :showing? not))
  ([tf]
   (!! assoc :showing? tf)))

(defn parent?
  "Return true if the namespace p is a parent of c. Expects two string"
  [p c]
  (let [pd (str p ".")]
    (= (subs c 0 (count pd)) pd)))

(defn self-or-parent?
  "Return true if the namespace p==c or p is a parent of c. Expects two string"
  [p c]
  (or (= c p)
      (parent? p c)))

(defn format-time
  [d]
  (when (instance? js/Date d)
    (.slice (aget (.split (.toJSON d) "T") 1) 0 -1)))

(defn dump-to-console!
  "Takes a log event and dumps all kinds of info about it to the developer
  console. Works under chrome. Probably also under firefox."
  [lg-ev]
  (js/console.group
    (gstring/format ;; firefox can't deal with format style stuff
      "%s%s%s%s -- %s"
      (:ns lg-ev)
      (if (empty? (:ns lg-ev)) "" "/")
      (:type lg-ev)
      (if-some [lnum (:line (:meta lg-ev))] (str ":" lnum) "")
      ;; We can't dered DB here to get the formatter
      ;; or reagent will re-render everything always
      (format-time (:time lg-ev))))
  ;; The meta data contains things like filename and line number of the original
  ;; log call. It might also catch the local bindings so we print them here.
  (when-some [meta (:meta lg-ev)]
    (.group js/console "Meta Data")
    (some->> (:file meta) (.log js/console "Filename: %s"))
    (when-some [trace (:trace meta)]
      (js/console.log "TRACE:")
      ;; We CANNOT add other text to the following log call since otherwise
      ;; the trace will not be source mapped properly and we'll end up with JS source links.
      (js/console.log trace))
    (when-some [env (seq (:env meta))]
      ;; 3rd level nested group. Oh yeah
      (js/console.group "Local Bindings")
      (doseq [[k v] env]
        (.log js/console "%s : %o" (pr-str k) v))
      (js/console.groupEnd))
    ;; The rest of the meta info, not sure if this should ever happen:
    (when-some [meta' (seq (dissoc meta :file :line :env :trace))]
      (doseq [[k v] meta']
        (js/console.log "%s : %o" (pr-str k) v)))
    (js/console.groupEnd))
  ;; console.dir firefox & chrome only?
  ;; %o calls either .dir() or .dirxml() if it's a DOM node
  ;; This means we get a real string and a real DOM node into
  ;; our console. Probably better than always calling dir
  (doseq [v (:msg lg-ev)]
    ;; truncate adds the elippsis...
    (if (string? v)
      (js/console.log "%o" v)
      (js/console.log "%o --- %s" v (-> v pr-str (gstring/truncate 20)))))
  (js/console.groupEnd))

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
           (h "span" #js{:style #js{:color (severity->color type)}} type)
           " "
           (h "span" #js{:style #js{:cursor "pointer"}
                         :onClick #(dump-to-console! lg-ev)}
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
     ;; Fast array reverse:
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
         (let [lg-ev (aget logs (- last-to-start i 1))]
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
        (if (:showing? @db)
          (@render-search-box (:search @db ""))
          (h "span" #js{}))
        (h "button" #js{:style   #js{:cursor "pointer"
                                     :color  (if (:frozen-at @db) "orange" "green")}
                        :onClick #(toggle-freeze)}
           (if (:frozen-at @db) "Thaw" "Freeze")))
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


(defn install-toggle-shortcut!
  "Installs a Keyboard Shortcut handler that toggles the visibility of the log overlay.
   Call the return function to unregister."
  [shortcut]
  ;; If previous one exist just unregister it:
  (when-some [prev (:shortcut-toggle-keys @db)]
    (prev))
  (let [handler (KeyboardShortcutHandler. js/window)]
    (.registerShortcut handler "klang.toggle" shortcut)
    (gevents/listen
      handler
      KeyboardShortcutHandler.EventType.SHORTCUT_TRIGGERED
      (fn [e] (toggle-showing!)))
    (js/console.info "Klang: Toggle overlay keyboard shortcut installed:" shortcut)
    (!! assoc :shortcut-toggle-keys #(.unregisterShortcut handler shortcut))))

(defn install-hide-shortcut!
  "Installs a Keyboard Shortcut handler that hides the log overlay.
    Call the return function to unregister."
  [shortcut]
  ;; If previous one exist just unregister it:
  (when-some [prev (:shortcut-hide-keys @db)]
    (prev))
  (let [handler (KeyboardShortcutHandler. js/window)]
    (.registerShortcut handler "klang.hide" shortcut)
    (gevents/listen
      handler
      KeyboardShortcutHandler.EventType.SHORTCUT_TRIGGERED
      (fn [e] (toggle-showing! false)))
    (js/console.info "Klang: Hide overlay keyboard shortcut installed:" shortcut)
    (!! assoc :shortcut-hide-keys #(.unregisterShortcut handler shortcut))))

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
    (install-toggle-shortcut! "m")
    (install-hide-shortcut! "ESC")
    (set-max-logs! 300)
    (add-watch db :rerender request-rerender!)
    (gstyle/installStyles (css-molokai))))

(defn add-log!
  "This is the main log functions:
  - ns - string
  - severity - string, like \"INFO\" or \"WARN\"
  - msg0 - If the map {::meta-data {...}} attaches this to the msg
    Otherwise the first message"
  [ns severity msg0 & msg]
  (deref ensure-klang-init)
  (let [db @db
        meta (::meta-data msg0)
        msg (if (some? meta) (vec msg) (into [msg0] msg))]
    (.push (:logs db) {:time (js/Date.)
                       :id (gens)
                       :ns (str ns)
                       :type (name severity)
                       :meta meta ;; Potentially nil
                       :msg msg})
    (request-rerender!)
    (if (pos? (count msg))
      (last msg)
      msg0)))

(defn clear!
  "Clears all logs"
  []
  (set! (:logs @db) -length 0)
  (request-rerender!))
