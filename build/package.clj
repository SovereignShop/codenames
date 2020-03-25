(ns package
  (:require [badigeon.classpath :as classpath]
            [badigeon.javac :as javac]
            [badigeon.compile :as compile]
            [badigeon.bundle :as bundle]
            [badigeon.jar :as jar]
            [badigeon.bundle :refer [bundle make-out-path]]
            [clojure.tools.deps.alpha :as d]
            [clojure.tools.deps.alpha.reader :as deps-reader]))

(defn -main []
  (let [deps-map (deps-reader/slurp-deps "deps.edn")
        out-path (make-out-path 'lib nil)
        extras   (d/combine-aliases deps-map [:clj-prod])]
    (compile/compile '[codenames.core]
                     {:compile-path "target/classes"
                      :classpath    (classpath/make-classpath {:deps-map deps-map
                                                               :aliases  [:clj-prod :aot]})})

    (jar/jar 'badigeon/badigeon {:mvn/version "0.0.1-SNAPSHOT"}
             {;; The jar file produced.
              :out-path                "target/codenames-0.0.1-SNAPSHOT.jar"
              ;; Adds a \"Main\" entry to the jar manifest with the value \"badigeon.main\"
              :main                    'codenames.core
              ;; By default Badigeon add entries for all files in the directory listed in the :paths section of the deps.edn file. This can be overridden here.
              :paths                   ["src" "target/classes" "resources"]
              ;; The dependencies to be added to the \"dependencies\" section of the pom.xml file. When not specified, defaults to the :deps entry of the deps.edn file, without merging the user-level and system-level deps.edn files
              ;; :deps '{org.clojure/clojure {:mvn/version "1.9.0"}}
              ;; The repositories to be added to the \"repositories\" section of the pom.xml file. When not specified, default to nil - even if the deps.edn files contains a :mvn/repos entry.
              :mvn/repos               '{"clojars" {:url "https://repo.clojars.org/"}}
              ;; A predicate used to excludes files from beeing added to the jar. The predicate is a function of two parameters: The path of the directory beeing visited (among the :paths of the project) and the path of the file beeing visited under this directory.
              :exclusion-predicate     jar/default-exclusion-predicate
              ;; A function to add files to the jar that would otherwise not have been added to it. The function must take two parameters: the path of the root directory of the project and the file being visited under this directory. When the function returns a falsy value, the file is not added to the jar. Otherwise the function must return a string which represents the path within the jar where the file is copied. 
              ;;:inclusion-path (partial badigeon.jar/default-inclusion-path "badigeon" "badigeon")
              ;; By default git and local dependencies are not allowed. Set allow-all-dependencies? to true to allow them 
              :allow-all-dependencies? true})

    (bundle out-path
            {:deps-map             (-> deps-map  ;; TODO: raise issue with badigeon
                                       (update :deps into (:extra-deps extras))
                                       (update :paths into (:extra-paths extras)))
             :allow-unstable-deps? true})))
