(defproject clj-webtoolbox "0.0.1"
  :description "Miscellaneous helper functions and macros for Ring and Compojure."
  :url         "https://github.com/gered/clj-webtoolbox"
  :license     {:name "MIT License"
                :url  "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.4.0" :scope "provided"]
                 [ring/ring-core "1.4.0" :scope "provided"]
                 [cheshire "5.5.0"]
                 [prismatic/schema "1.0.4"]])
