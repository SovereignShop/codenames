(ns ^:figwheel-always codenames.views.game
  (:require
   [codenames.subs.game :as game-subs]
   [codenames.subs.session :as session-subs]
   [codenames.subs.stats :as stat-subs]
   [codenames.constants.ui-idents :as idents]
   [codenames.constants.ui-tabs :as tabs]
   [codenames.events.game :as game-events]
   [codenames.events.pregame :as pregame-events]
   [codenames.db :as db]
   [swig.views :as swig-view]
   [re-posh.core :as re-posh]
   [re-com.core :as com :refer [h-box v-box box button gap scroller input-text]]))


(defn popover! [content label title]
  (re-posh/dispatch [:codenames.events.popover/show
                     [:swig/ident idents/main-popover]
                     {:popover/content  content
                      :popover/showing? true
                      :popover/label    label
                      :popover/title    title}]))

(defn display-card
  [game-id
   {:keys [:codenames.word-card/word
           :codenames.word-card/position
           :codenames.word-card/character-card]
    :as   card}]
  (let [character-card-id (:db/id character-card)
        player-type       @(re-posh/subscribe [::game-subs/player-type game-id])
        codemaster?       (= player-type :codemaster)
        card              @(re-posh/subscribe [::game-subs/character-card character-card-id])
        played?           (:codenames.character-card/played? card)
        role              (:codenames.character-card/role card)
        word-color        (if (= role :assassin) "white" "black")]
    [box
     :attr  {:on-click #(re-posh/dispatch [::game-events/card-click game-id character-card-id])}
     :style {:text-align       "center"
             :padding          "12px"
             :border-radius    "3px"
             :box-shadow       (if played? "3px 3px 2px grey" "")
             :background-color (if (or codemaster? played?)
                                 (case role
                                   :neutral  (if played? "#ff9933" "#ffb366")
                                   :blue     (if played? "#0040ff" "#00bfff")
                                   :red      (if played? "#990000" "#cc0000")
                                   :assassin "black")
                                 "white")}
     :child [:h4 {:style {:text-align :center
                          :color      (if (or codemaster? played?) "white" "black")}} word]]))

(defn game-score [game-id]
  (let [red-remaining @(re-posh/subscribe [::game-subs/red-cards-remaining game-id])
        blue-remaining  @(re-posh/subscribe [::game-subs/blue-cards-remaining game-id])]
    [h-box
     :max-width "60px"
     :style {:width "50px"}
     :children
     [[:div {:style {:color "red" :width "20px"}} red-remaining]
      [:div {:style {:color "blue" :width "20px"}} blue-remaining]]]))

(defn board-info [tab-id game-id cards]
  (let [current-team                 @(re-posh/subscribe [::game-subs/current-team game-id])
        team-color                   (:codenames.team/color current-team)
        [winning-team winning-color] @(re-posh/subscribe [::game-subs/game-over game-id])]
    #_(when winning-team
      (re-posh/dispatch [::game-events/set-winning-team game-id winning-team]))
    [h-box
     :class "center"
     :gap "20px"
     :children
     [[button
       :on-click #(re-posh/dispatch [::pregame-events/new-round game-id])
       :label "New Round"]
      [button
       :on-click #(re-posh/dispatch [::pregame-events/enter-pregame tab-id])
       :label "Exit Game"]
      [button
       :label "End Turn"
       :on-click #(re-posh/dispatch [::game-events/end-turn game-id])]
      [game-score game-id]
      (case team-color
        :blue [:div {:style {:color "Blue" :width "125px"}} "Blue Team's Turn"]
        :red  [:div {:style {:color "Red" :width "125px"}} "Red Team's Turn"]
        [:div (str "Error.. " current-team)])
      (case winning-color
        :red  [:div {:style {:color "red"}} "Red team wins!"]
        :blue [:div {:style {:color "blue"}} "Blue team wins!"]
        nil)]]))

(defn board-grid [game-id cards]
  (let [cards (->> cards
                   (sort-by :codenames.word-card/position)
                   (partition (first db/board-dimensions)))]
    [:div #_{:class "center"}
     [:table.center {:style {:border-spacing  "10px"
                             :border-collapse "separate"}}
      [:tbody
       (for [row cards]
         [:tr
          (for [card row]
            [:td [display-card game-id card]])])]]]))

(defn turn-handler [game-id]
  (let [turn        @(re-posh/subscribe [::game-subs/current-turn game-id])
        turn-id     (:db/id turn)
        player-type @(re-posh/subscribe [::game-subs/player-type game-id])
        codemaster? (= player-type :codemaster)
        submitted?  (:codenames.turn/submitted? turn)]
    (if (and codemaster? (not submitted?))
      [h-box
       :class "center"
       :children
       [[input-text
         :model (str (:codenames.turn/word turn))
         :placeholder "word"
         :on-change #(re-posh/dispatch [::game-events/set-word turn-id %])]
        [input-text
         :model (str (:codenames.turn/number turn))
         :placeholder "number"
         :on-change #(re-posh/dispatch [::game-events/set-number turn-id %])]
        [button
         :label    "Submit"
         :on-click #(re-posh/dispatch [::game-events/submit-clue turn-id])]]]
      (when submitted?
        [h-box
         :class "center"
         :children
         [[box :child (str (:codenames.turn/word turn))]
          [gap :size "10px"]
          [box :child (str (:codenames.turn/number turn))]]]))))

(defmethod swig-view/dispatch tabs/game-board
  [{tab-id :db/id}]
  (when-let [game-id @(re-posh/subscribe [::session-subs/game])]
    (let [cards @(re-posh/subscribe [::game-subs/word-cards game-id])]
      [v-box
       :width "100%"
       :children
       [[board-info tab-id game-id cards]
        [turn-handler game-id]
        [scroller
         :style {:flex "1 1 0%"}
         :child
         [board-grid game-id cards]]]])))

(defmethod swig-view/dispatch tabs/leader-board
  [tab]
  [:div "Leader Board"]
  #_(let [stats @(re-posh/subscribe [::stat-subs/leader-board])]
    [:table
     [:thead
      [:tr [:td "Name"] [:td "CM Wins"] [:td "CM Losses"] [:td "Wins"] [:td "Losses"]]]
     [:tbody
      (for [stat stats]
        [:tr
         (for [s stat]
           [:td (str s)])])]]))

(defmethod swig-view/dispatch tabs/score-board
  [tab]
  (let [tab-id (:db/id tab)
        src @(re-posh/subscribe [::game-subs/get-browser-src tab-id])]
    [v-box
     :style {:flex "1 1 0%"}
     :children
     [[input-text
       :width "100%"
       :model (or (str src) "https://marginalrevolution.com")
       :on-change #(re-posh/dispatch [::game-events/set-browser-src tab-id %])
       :change-on-blur? true]
      [:iframe {:width "100%"
                :height "100%"
                :target "_parent"
                :allow "accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture"
                :allowfullscreen true
                :on-load #(let [new-src (-> % .-target .-contentWindow)]
                            #_(when (not= new-src src)
                                (re-posh/dispatch [::game-events/set-browser-src tab-id new-src])))
                :src (or src "https://www.youtube.com/embed/S6GVXk6kbcs")}]]]))


