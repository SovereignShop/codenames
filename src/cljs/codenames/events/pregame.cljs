(ns ^:figwheel-always codenames.events.pregame
  (:require
   [codenames.db :as db]
   [codenames.constants.ui-tabs :as tabs]
   [codenames.constants.ui-idents :as idents]
   [codenames.utils :as utils]
   [codenames.subs.session :as session]
   [datascript.core :as d]
   [swig.parser :refer [hiccup->facts]]
   [swig.macros :refer [def-event-ds]]
   [taoensso.timbre :refer-macros [debug info warn error]]))

(def-event-ds ::enter-game [db [_ game-id]]
  (with-meta
    (into [[:db.fn/retractAttribute [:swig/ident tabs/pregame] :swig.ref/parent]
           [:db/add [:swig/ident :swig/main-view] :swig.view/active-tab [:swig/ident tabs/game]]
           [:db/add [:swig/ident tabs/game] :swig.ref/parent [:swig/ident :swig/main-view]]
           [:db/add [:swig/ident idents/session] :session/game game-id]])
    {:tx/group-update? true}))

(def-event-ds ::enter-pregame [db _]
  (with-meta
    (into [[:db.fn/retractAttribute [:swig/ident tabs/game] :swig.ref/parent]
           [:db/add [:swig/ident :swig/main-view] :swig.view/active-tab [:swig/ident tabs/pregame]]
           [:db/add [:swig/ident tabs/pregame] :swig.ref/parent [:swig/ident :swig/main-view]]])
    {:tx/group-update? true}))

(def-event-ds ::new-game [db _]
  (let [session (d/entity db [:swig/ident idents/session])
        user    (:session/user session)
        teams   [(assoc (utils/make-team "Blue Team"
                                         :blue
                                         [(utils/make-player (:db/id user) :codemaster)])
                        :db/id -2)
                 (assoc (utils/make-team "Red Team" :red [])
                        :db/id -3)]]
    (into [{:game/finished?    false
            :game/current-team (-> teams shuffle first :db/id)
            :game/teams        teams
            :game/id           (utils/make-random-uuid)
            :db/id             -1}
           {:swig/ident   idents/session
            :session/game -1}]
          (utils/make-game-pieces -1 db/words db/board-dimensions))))

(defn retract-avs [db attr value]
  (vec (for [{:keys [e a v]} (d/datoms db :avet attr value)]
         [:db/retract e a v])))

(def-event-ds ::join-team [db [_ color game-id]]
  (let [session (d/entity db [:swig/ident idents/session])
        user    (:session/user session)
        user-id (:db/id user)
        [tid pid]
        (d/q '[:find [?tid ?pid]
               :in $ ?uid ?gid
               :where
               [?gid :game/teams ?tid]
               [?tid :codenames.team/players ?pid]
               [?pid :codenames.player/user ?uid]]
             db
             user-id
             game-id)
        team-id
        (d/q '[:find ?tid .
               :in $ ?gid ?color
               :where
               [?gid :game/teams ?tid]
               [?tid :codenames.team/color ?color]]
             db
             game-id
             color)]
    (if (and tid pid team-id (not= tid team-id))
      [[:db/retract tid :codenames.team/players pid]
       [:db/add team-id :codenames.team/players pid]]
      (if (and team-id (not= tid team-id))
        [(assoc (utils/make-player user-id :guesser) :db/id -1)
         [:db/add team-id :codenames.team/players -1]]
        (do (info "No update")
            [])))))

(comment
  (join-team @db/conn [::join-team :red 40])
  (def session (d/entity @db/conn [:swig/ident idents/session]))
  (def user (:session/user session))
  (def user-id (:db/id user))
  (def game-id 40)


  )

(def-event-ds ::choose-player-type [db [_ player-type]]
  (let [session (d/entity db [:swig/ident idents/session])
        player  (d/q '[:find ?pid .
                       :in $ ?uid
                       :where
                       [?pid :codenames.player/user ?uid]]
                     db
                     (:db/id (:session/user session)))]
    (js/console.log "WTF" (:session/user session))
    [[:db/add player :codenames.player/type player-type]]))


(def-event-ds ::randomize-teams [db _]
  (let [session (d/entity db idents/session)
        game    (d/entity db (:session/game session))
        teams   (:game/teams game)
        players (d/pull-many db [:codenames.team/players] teams)
        [a b]   (split-at (/ (count players) 2) (shuffle (apply concat players))) ]
    (concat
     (for [tid teams]
       [:db.fn/retractAttribute tid :codenames.team/players])
     [[:db/add (first teams) :codenames.team/players a]]
     [[:db/add (first teams) :codenames.team/players b]])))

(def-event-ds ::set-timer-length [db _]
  [])
