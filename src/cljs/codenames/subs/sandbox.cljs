(ns codenames.subs.sandbox
  (:require
   [codenames.db :as db]
   [codenames.subs.app-state :as app-state]
   [codenames.subs.game :as game]
   [codenames.subs.pregame :as pregame]
   [datascript.core :as d]))

(comment
  (def current-game (d/q app-state/current-game @db/conn))
  (def cards-remaining (d/q game/cards-remaining @db/conn current-game))

  )
