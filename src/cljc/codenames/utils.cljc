(ns codenames.utils
  (:require
   [codenames.constants.ui-idents :as idents]
   [codenames.db :as db]
   [clojure.string :as string])
  #?(:clj (:import [java.util UUID])))

(defn make-random-uuid
  "Taken from: https://github.com/lbradstreet/cljs-uuid-utils/blob/master/src/cljs_uuid_utils/core.cljs

  (make-random-uuid)  =>  new-uuid
  Arguments and Values:
  new-uuid --- new type 4 (pseudo randomly generated) cljs.core/UUID instance.
  Description:
  Returns pseudo randomly generated UUID,
  like: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx as per http://www.ietf.org/rfc/rfc4122.txt.
  Examples:
  (make-random-uuid)  =>  #uuid \"305e764d-b451-47ae-a90d-5db782ac1f2e\"
  (type (make-random-uuid)) => cljs.core/UUID"
  []
  (letfn #?(:clj [(f [] (Long/toHexString (rand-int 16))) 
                  (g [] (Long/toHexString (bit-or 0x8 (bit-and 0x3 (rand-int 15)))))]
            :cljs [(f [] (.toString (rand-int 16) 16))
                   (g [] (.toString  (bit-or 0x8 (bit-and 0x3 (rand-int 15))) 16))])
    (let [s 
          (string/join (concat (repeatedly 8 f) "-"
                               (repeatedly 4 f) "-4"
                               (repeatedly 3 f) "-"
                               (g)
                               (repeatedly 3 f) "-"
                               (repeatedly 12 f)))]
      #?(:cljs (UUID. s nil) :clj (UUID/fromString s)))))

(defn make-board-cards [word-bank [n-rows n-cols]]
  (let [words (->> (shuffle word-bank)
                   (partition n-cols)
                   (take n-rows)
                   (mapv vec))]
    (for [row (range n-rows)
          col (range n-cols)]
      {:codenames.word-card/position [row col]
       :codenames.word-card/word     (get-in words [row col])})))

(defn make-investigator-cards [color n]
  (repeat n {:codenames.character-card/color color
             :codenames.character-card/played? false}))

(defn make-game [teams word-bank board-dimensions]
  (let [first-player   (nth [:red :blue] (rand-int 2))
        board-cards    (make-board-cards word-bank board-dimensions)
        red-cards      (make-investigator-cards :red (case first-player :red 9 8))
        blue-cards     (make-investigator-cards :blue (case first-player :blue 9 8))
        neutral-cards  (make-investigator-cards :neutral 7)
        assasin-cards  (make-investigator-cards :assassin 1)
        game           {:db/id         -1
                        :game/id       (make-random-uuid)
                        :game/finshed? false
                        :game/teams    teams}
        teams-by-color (group-by :codenames.team/color teams)
        app-state      {:swig/ident                        idents/app-state
                        :codenames.game-state/current-team (first (teams-by-color first-player))}]
    (into [game app-state]
          (comp cat (map #(assoc % :codenames.piece/game -1)))
          [board-cards red-cards blue-cards neutral-cards assasin-cards])))

(comment
  (make-game db/words db/board-dimensions)
  (require '[datascript.core :as d])
  (first (d/rseek-datoms @db/conn :aevt :codename.game/id))

  )
