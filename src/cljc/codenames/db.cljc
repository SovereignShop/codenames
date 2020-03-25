(ns codenames.db
  (:require
   [codenames.constants.ui-idents :as idents]
   [codenames.constants.ui-views :as views]
   [codenames.constants.ui-splits :as splits]
   [codenames.constants.ui-tabs :as tabs]
   [datascript.core :as d]
   [swig.core :as swig :refer [view tab split window full-schema]]
   [taoensso.timbre :as timbre :refer [debug info warn]]))

#?(:cljs (goog-define HOSTNAME "http://localhost"))
#?(:cljs (goog-define PORT "3001"))

(def board-dimensions [5 5])
(def board-size (apply * board-dimensions))

(def words
  #{"Acne" "Acre" "Addendum" "Advertise" "Aircraft" "Aisle" "Alligator" "Alphabetize"
    "America" "Ankle" "Apathy" "Applause" "Applesauc" "Application" "Archaeologist" "Aristocrat"
    "Arm" "Armada" "Asleep" "Astronaut" "Athlete" "Atlantis" "Aunt" "Avocado"
    "Baby-Sitter" "Backbone" "Bag" "Baguette" "Bald" "Balloon" "Banana" "Banister"
    "Baseball" "Baseboards" "Basketball" "Bat" "Battery" "Beach" "Beanstalk" "Bedbug"
    "Beer" "Beethoven" "Belt" "Bib" "Bicycle" "Big" "Bike" "Billboard"
    "Bird" "Birthday" "Bite" "Blacksmith" "Blanket" "Bleach" "Blimp" "Blossom"
    "Blueprint" "Blunt" "Blur" "Boa" "Boat" "Bob" "Bobsled" "Body"
    "Bomb" "Bonnet" "Book" "Booth" "Bowtie" "Box" "Boy" "Brainstorm"
    "Brand" "Brave" "Bride" "Bridge" "Broccoli" "Broken" "Broom" "Bruise"
    "Brunette" "Bubble" "Buddy" "Buffalo" "Bulb" "Bunny" "Bus" "Buy"
    "Cabin" "Cafeteria" "Cake" "Calculator" "Campsite" "Can" "Canada" "Candle"
    "Candy" "Cape" "Capitalism" "Car" "Cardboard" "Cartography" "Cat" "Cd"
    "Ceiling" "Cell" "Century" "Chair" "Chalk" "Champion" "Charger" "Cheerleader"
    "Chef" "Chess" "Chew" "Chicken" "Chime" "China" "Chocolate" "Church"
    "Circus" "Clay" "Cliff" "Cloak" "Clockwork" "Clown" "Clue" "Coach"
    "Coal" "Coaster" "Cog" "Cold" "College" "Comfort" "Computer" "Cone"
    "Constrictor" "Continuum" "Conversation" "Cook" "Coop" "Cord" "Corduroy" "Cot"
    "Cough" "Cow" "Cowboy" "Crayon" "Cream" "Crisp" "Criticize" "Crow"
    "Cruise" "Crumb" "Crust" "Cuff" "Curtain" "Cuticle" "Czar" "Dad"
    "Dart" "Dawn" "Day" "Deep" "Defect" "Dent" "Dentist" "Desk"
    "Dictionary" "Dimple" "Dirty" "Dismantle" "Ditch" "Diver" "Doctor" "Dog"
    "Doghouse" "Doll" "Dominoes" "Door" "Dot" "Drain" "Draw" "Dream"
    "Dress" "Drink" "Drip" "Drums" "Dryer" "Duck" "Dump" "Dunk"
    "Dust" "Ear" "Eat" "Ebony" "Elbow" "Electricity" "Elephant" "Elevator"
    "Elf" "Elm" "Engine" "England" "Ergonomic" "Escalator" "Eureka" "Europe"
    "Evolution" "Extension" "Eyebrow" "Fan" "Fancy" "Fast" "Feast" "Fence"
    "Feudalism" "Fiddle" "Figment" "Finger" "Fire" "First" "Fishing" "Fix"
    "Fizz" "Flagpole" "Flannel" "Flashlight" "Flock" "Flotsam" "Flower" "Flu"
    "Flush" "Flutter" "Fog" "Foil" "Football" "Forehead" "Forever" "Fortnight"
    "France" "Freckle" "Freight" "Fringe" "Frog" "Frown" "Gallop" "Game"
    "Garbage" "Garden" "Gasoline" "Gem" "Ginger" "Gingerbread" "Girl" "Glasses"
    "Goblin" "Gold" "Goodbye" "Grandpa" "Grape" "Grass" "Gratitude" "Gray"
    "Green" "Guitar" "Gum" "Gumball" "Hair" "Half" "Handle" "Handwriting"
    "Hang" "Happy" "Hat" "Hatch" "Headache" "Heart" "Hedge" "Helicopter"
    "Hem" "Hide" "Hill" "Hockey" "Homework" "Honk" "Hopscotch" "Horse"
    "Hose" "Hot" "House" "Houseboat" "Hug" "Humidifier" "Hungry" "Hurdle"
    "Hurt" "Hut" "Ice" "Implode" "Inn" "Inquisition" "Intern" "Internet"
    "Invitation" "Ironic" "Ivory" "Ivy" "Jade" "Japan" "Jeans" "Jelly"
    "Jet" "Jig" "Jog" "Journal" "Jump" "Key" "Killer" "Kilogram"
    "King" "Kitchen" "Kite" "Knee" "Kneel" "Knife" "Knight" "Koala"
    "Lace" "Ladder" "Ladybug" "Lag" "Landfill" "Lap" "Laugh" "Laundry"
    "Law" "Lawn" "Lawnmower" "Leak" "Leg" "Letter" "Level" "Lifestyle"
    "Ligament" "Light" "Lightsaber" "Lime" "Lion" "Lizard" "Log" "Loiterer"
    "Lollipop" "Loveseat" "Loyalty" "Lunch" "Lunchbox" "Lyrics" "Machine" "Macho"
    "Mailbox" "Mammoth" "Mark" "Mars" "Mascot" "Mast" "Matchstick" "Mate"
    "Mattress" "Mess" "Mexico" "Midsummer" "Mine" "Mistake" "Modern" "Mold"
    "Mom" "Monday" "Money" "Monitor" "Monster" "Mooch" "Moon" "Mop"
    "Moth" "Motorcycle" "Mountain" "Mouse" "Mower" "Mud" "Music" "Mute"
    "Nature" "Negotiate" "Neighbor" "Nest" "Neutron" "Niece" "Night" "Nightmare"
    "Nose" "Oar" "Observatory" "Office" "Oil" "Old" "Olympian" "Opaque"
    "Opener" "Orbit" "Organ" "Organize" "Outer" "Outside" "Ovation" "Overture"
    "Pail" "Paint" "Pajamas" "Palace" "Pants" "Paper" "Park"
    "Parody" "Party" "Password" "Pastry" "Pawn" "Pear" "Pen" "Pencil"
    "Pendulum" "Penny" "Pepper" "Personal" "Philosopher" "Phone" "Photograph"
    "Piano" "Picnic" "Pigpen" "Pillow" "Pilot" "Pinch" "Ping" "Pinwheel"
    "Pirate" "Plaid" "Plan" "Plank" "Plate" "Platypus" "Playground" "Plow"
    "Plumber" "Pocket" "Poem" "Point" "Pole" "Pomp" "Pong" "Pool"
    "Popsicle" "Population" "Portfolio" "Positive" "Post" "Princess" "Procrastinate" "Protestant"
    "Psychologist" "Publisher" "Punk" "Puppet" "Puppy" "Push" "Puzzle" "Quarantine"
    "Queen" "Quicksand" "Quiet" "Race" "Radio" "Raft" "Rag" "Rainbow"
    "Rainwater" "Random" "Ray" "Recycle" "Red" "Regret" "Reimbursement" "Retaliate"
    "Rib" "Riddle" "Rim" "Rink" "Roller" "Room" "Rose" "Round"
    "Roundabout" "Rung" "Runt" "Rut" "Sad" "Safe" "Salmon" "Salt"
    "Sandbox" "Sandcastle" "Sandwich" "Sash" "Satellite" "Scar" "Scared" "School"
    "Scoundrel" "Scramble" "Scuff" "Seashell" "Season" "Sentence" "Sequins" "Set"
    "Shaft" "Shallow" "Shampoo" "Shark" "Sheep" "Sheets" "Sheriff" "Shipwreck"
    "Shirt" "Shoelace" "Short" "Shower" "Shrink" "Sick" "Siesta" "Silhouette"
    "Singer" "Sip" "Skate" "Skating" "Ski" "Slam" "Sleep" "Sling"
    "Slow" "Slump" "Smith" "Sneeze" "Snow" "Snuggle" "Song" "Space"
    "Spare" "Speakers" "Spider" "Spit" "Sponge" "Spool" "Spoon" "Spring"
    "Sprinkler" "Spy" "Square" "Squint" "Stairs" "Standing" "Star" "State"
    "Stick" "Stockholder" "Stoplight" "Stout" "Stove" "Stowaway" "Straw" "Stream"
    "Streamline" "Stripe" "Student" "Sun" "Sunburn" "Sushi" "Swamp" "Swarm"
    "Sweater" "Swimming" "Swing" "Tachometer" "Talk" "Taxi" "Teacher" "Teapot"
    "Teenager" "Telephone" "Ten" "Tennis" "Thief" "Think" "Throne" "Through"
    "Thunder" "Tide" "Tiger" "Time" "Tinting" "Tiptoe" "Tiptop" "Tired"
    "Tissue" "Toast" "Toilet" "Tool" "Toothbrush" "Tornado" "Tournament" "Tractor"
    "Train" "Trash" "Treasure" "Tree" "Triangle" "Trip" "Truck" "Tub"
    "Tuba" "Tutor" "Television" "Twang" "Twig" "Twitterpated" "Type" "Unemployed"
    "Upgrade" "Vest" "Vision" "Wag" "Water" "Watermelon" "Wax" "Wedding"
    "Weed" "Welder" "Whatever" "Wheelchair" "Whiplash" "Whisk" "Whistle" "White"
    "Wig" "Will" "Windmill" "Winter" "Wish" "Wolf" "Wool" "World"
    "worm" "wristwatch" "yardstick" "zamboni" "zen" "zero" "zipper" "zone" "zoo"})

(def game-state
  [{:player/name  "John"
    :player/color "blue"
    :player/type  :player.type/guesser}
   {:player/name  "David"
    :player/color "blue"
    :player/type  :player.type/codemaster}
   {:codenames.character-card/color    "Blue"
    :codenames.character-card/played?  false
    :codenames.character-card/position [0 0]}
   {:codenames.word-card/word "Home"
    :codenames.word-card/position? [0 0]
    :codenames.word-card/covered? false}])

(def extras
  [{:swig/ident              idents/fullscreen-view
    :fullscreen-view/view-id views/main-view}
   {:swig/ident       :user-login
    :app/type         :type/user-login
    :user-login/state :unauthenticated}
   {:swig/ident idents/server-events}
   {:swig/ident  idents/app-state}])

(def login-layout
  (swig/view {:swig/ident :swig/main-view}
             (swig/window {:swig/ident idents/login-window})
             (swig/window {:swig/ident idents/modal-dialog})))

(def board-layout
  (swig/view  {:swig/ident :swig/main-view
               :swig.view/active-tab [:swig/ident tabs/game]}
              (swig/tab {:swig/ident tabs/leader-board
                         :swig.tab/label {:swig/type :swig.type/cell
                                          :swig.cell/element "Leader Board"}})
              (swig/tab {:swig/ident tabs/game
                         :swig.tab/label  {:swig/type :swig.type/cell
                                           :swig.cell/element "Game"}}
                        (swig/split {:swig/ident               splits/game-split
                                     :swig.split/orientation   :vertical
                                     :swig.split/split-percent 30}
                                    (swig/split {:swig/ident               splits/game-info-split
                                                 :swig.split/split-percent 50
                                                 :swig.split/orientation   :horizontal}
                                                (swig/tab {:swig/ident tabs/score-board})
                                                (swig/tab {:swig/ident tabs/player-board}))
                                    (swig/tab {:swig/ident tabs/game-board})))))

(def schema-keys
  (into #{} (map :db/ident) full-schema))

(defonce default-db (into [] cat [game-state extras]))

(def schema
  [{:db/ident :turn/id :db/valueType :db.type/uuid :db/cardinality :db.cardinality/one}
   {:db/ident :turn/game :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :group/id :db/valueType :db.type/uuid :db/cardinality :db.cardinality/one}
   {:db/ident :group/name :db/valueType :db.type/uuid :db/cardinality :db.cardinality/one}
   {:db/ident :user/id :db/valueType :db.type/uuid :db/cardinality :db.cardinality/one}
   {:db/ident :user/name :db/valueType :db.type/uuid :db/cardinality :db.cardinality/one}
   {:db/ident :user/alias :db/valueType :db.type/uuid :db/cardinality :db.cardinality/one}
   {:db/ident :game/id :db/valueType :db.type/uuid :db/cardinality :db.cardinality/one}
   {:db/ident :game/name :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :game/teams :db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   {:db/ident :game/finished? :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.team/id :db/valueType :db.type/uuid :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.team/name :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.team/color :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.team/players :db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   {:db/ident :codenames.player/id :db/valueType :db.type/uuid :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.player/user :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.player/type :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.app-state/current-teams :db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   {:db/ident :codenames.game-state/current-team :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.player-state/team :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.player-state/player :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.piece/game :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.piece/type :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.word-card/character-card :db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.word-card/position :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.word-card/word :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.character-card/color :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :codenames.character-card/role :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :transaction/tx-meta :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :transaction/tx-added :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :transaction/tx-entities :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
   {:db/ident :ui/type :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}
   {:db/ident :popover/showing? :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}
   {:db/ident :app/type :db/valueType :db.type/keyword :db/cardinality :db.cardinality/one}])

(defn to-ds-schema [schema]
  (into {:db/ident {:db/unique :db.unique/identity}}
        (comp cat
              (map (fn [m]
                     [(:db/ident m)(cond-> m
                                     true                                  (dissoc m :db/ident)
                                     (not= (:db/valueType m) :db.type/ref) (dissoc m :db/valueType))])))
        [swig/full-schema schema]))

(def ds-schema (to-ds-schema schema))

(defonce conn (d/create-conn ds-schema))
