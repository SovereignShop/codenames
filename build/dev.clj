(ns dev
  (:require [nrepl.cmdline :as nrepl]))

(defn -main [& args]
  #_(clojure.lang.RT/loadLibrary "opencv_java410")
  (nrepl/-main "-m" "nrepl.cmdline" "--middleware"
               "[cider.nrepl/cider-middleware,cider.piggieback/wrap-cljs-repl]"))
