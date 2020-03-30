(ns codenames.events.game
  "Events that occure during a game"
  (:require
   [codenames.constants.ui-idents :as idents]
   [codenames.utils :refer [make-game]]
   [codenames.db :as db]
   [datascript.core :as d]
   [swig.macros :refer [def-event-ds]]
   [taoensso.timbre :as timbre :refer-macros [debug info warn]]))

(defn users-turn? [db game-id]
  (let [game    (d/entity db game-id)
        team    (:game/current-team game)
        players (:codenames.team/players team)
        users   (into #{} (map (comp :db/id :codenames.player/user)) players)
        session (d/entity db [:swig/ident idents/session])
        user    (-> session :session/user :db/id)]
    (contains? users user)))

(def-event-ds ::end-turn [db [_ game-id]]
  (when (users-turn? db game-id)
    (let [game          (d/entity db game-id)
          team          (:game/current-team game)
          teams         (:game/teams game)
          other-team-id (->> teams (remove #(= (:db/id %) (:db/id team))) first :db/id)]
      [[:db/add game-id :game/current-team other-team-id]])))

(def-event-ds ::card-click [db [_ game-id character-card]]
  (when (users-turn? db game-id)
    (let [{:keys [:game/blue-cards-count
                  :game/red-cards-count]
           }         (d/entity db game-id)
          game       (d/entity db game-id)
          team       (:game/current-team game)
          team-color (:codenames.team/color team)
          session    (d/entity db [:swig/ident idents/session])
          card       (d/entity db character-card)
          color      (:codenames.character-card/role card)]
      (when-not (:codenames.character-card/played? card)
        (concat
         [{:db/id                            (:db/id card)
           :codenames.character-card/played? true}]
         (case color
           :blue [[:db/add game-id :game/blue-cards-count (dec blue-cards-count)]]
           :red  [[:db/add game-id :game/red-cards-count (dec red-cards-count)]]
           [])
         (when (not= team-color color)
           (end-turn db [nil game-id])))))))

