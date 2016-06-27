(defproject gered/webtools "0.2-SNAPSHOT"
  :description "Miscellaneous helper functions and macros for Ring and Compojure."
  :url         "https://github.com/gered/webtools"
  :license     {:name "MIT License"
                :url  "http://opensource.org/licenses/MIT"}

  :dependencies [[cheshire "5.5.0"]
                 [prismatic/schema "1.0.4"]
                 [hiccup "1.0.5"]
                 [cljs-ajax "0.5.5"]
                 [secretary "1.2.3"]]

  :plugins      [[lein-cljsbuild "1.1.3"]]

  :profiles     {:provided
                 {:dependencies [[org.clojure/clojure "1.8.0"]
                                 [org.clojure/clojurescript "1.8.51"]
                                 [compojure "1.4.0"]
                                 [ring/ring-core "1.4.0"]]}}

  :cljsbuild    {:builds
                 {:main
                  {:source-paths ["src"]}}})
