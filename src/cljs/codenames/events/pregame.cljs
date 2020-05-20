(ns ^:figwheel-always codenames.events.pregame
  (:require
   [codenames.db :as db]
   [codenames.constants.ui-tabs :as tabs]
   [codenames.constants.ui-idents :as idents]
   [codenames.utils :as utils]
   [datascript.core :as d]
   [swig.macros :refer [def-event-ds]]
   [swig.events :refer [exit-fullscreen]]
   [taoensso.timbre :refer-macros [debug info warn error]]))

(def-event-ds ::enter-game [db [_ tab-id game-id]]
  (let [tab (d/entity db tab-id)
        parent (:swig.ref/parent tab)
        parent-id (:db/id parent)]
    (into [[:db.fn/retractAttribute tab-id :swig.ref/parent]
           [:db/add parent-id :swig.view/active-tab [:swig/ident tabs/game]]
           [:db/add [:swig/ident tabs/game] :swig.ref/parent parent-id]
           [:db/add [:swig/ident idents/session] :session/game game-id]])))

(def-event-ds ::enter-pregame [db _]
  (let [tab (d/entity db [:swig/ident tabs/game-board])
        game-tab (d/entity db[:swig/ident tabs/game])
        parent (:swig.ref/parent game-tab)
        parent-id (:db/id parent)]
    (into (if (:swig.tab/fullscreen tab)
            (exit-fullscreen db tab)
            [])
          [[:db.fn/retractAttribute (:db/id game-tab) :swig.ref/parent]
           [:db/add parent-id :swig.view/active-tab [:swig/ident tabs/pregame]]
           [:db/add [:swig/ident tabs/pregame] :swig.ref/parent parent-id]])))

(def-event-ds ::new-game [db _]
  (let [session     (d/entity db [:swig/ident idents/session])
        user        (:session/user session)
        teams       [(assoc (utils/make-team "Blue Team"
                                             :blue
                                             [(utils/make-player (:db/id user) :codemaster)])
                            :db/id -2)
                     (assoc (utils/make-team "Red Team" :red [])
                            :db/id -3)]
        first-team  (-> teams shuffle first)
        first-color (:codenames.team/color first-team)]
    (into [{:game/finished?     false
            :game/current-round -4
            :game/rounds        -4
            :game/teams         teams
            :game/id            (utils/make-random-uuid)
            :db/id              -1}
           {:swig/ident   idents/session
            :session/game -1}
           {:codenames.round/number           1
            :codenames.round/current-turn     -5
            :codenames.round/blue-cards-count (case first-color :blue 9 8)
            :codenames.round/red-cards-count  (case first-color :red 9 8)
            :codenames.round/current-team     (:db/id first-team)
            :db/id                            -4}
           {:db/id                     -5
            :codenames.turn/team       (:db/id first-team)
            :codenames.turn/word       ""
            :codenames.turn/submitted? false}]
          (utils/make-game-pieces -4 db/words db/board-dimensions first-color))))

(def-event-ds ::new-round [db [_ game-id]]
  (let [game         (d/entity db game-id)
        teams        (:game/teams game)
        first-team   (-> teams shuffle first)
        first-color  (:codenames.team/color first-team)
        rounds       (:game/rounds game)
        round-number (apply max (map :codenames.round/number rounds))]
    (into [{:codenames.round/number           (inc round-number)
            :codenames.round/turns            -2
            :codenames.round/current-turn     -2
            :codenames.round/blue-cards-count (case first-color :blue 9 8)
            :codenames.round/red-cards-count  (case first-color :red 9 8)
            :codenames.round/current-team     (:db/id first-team)
            :db/id                            -1}
           {:db/id              game-id
            :game/rounds        -1
            :game/current-round -1}
           {:db/id                     -2
            :codenames.turn/team       (:db/id first-team)
            :codenames.turn/word       ""
            :codenames.turn/submitted? false}]
          (utils/make-game-pieces -1 db/words db/board-dimensions first-color))))

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

(def-event-ds ::choose-player-type [db [_ game-id player-type]]
  (let [session (d/entity db [:swig/ident idents/session])
        player  (d/q '[:find ?pid .
                       :in $ ?uid ?gid
                       :where
                       [?gid :game/teams ?tid]
                       [?tid :codenames.team/players ?pid]
                       [?pid :codenames.player/user ?uid]]
                     db
                     (:db/id (:session/user session))
                     game-id)]
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
