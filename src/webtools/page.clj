(ns webtools.page
  (:require
    [clojure.string :as string]
    [hiccup.core :refer [html]]
    [hiccup.page :refer [doctype]]
    [hiccup.element :refer [javascript-tag]]))

(defn html5
  [& contents]
  (html
    {:mode :html}
    (doctype :html5)
    (-> ["<!--[if lt IE 10]>"  "<html class=\"old-ie no-websockets\">" "<![endif]-->"
         "<!--[if gte IE 10]>" "<html>"                                "<![endif]-->"
         "<!--[if !IE]> -->"   "<html>"                                "<!-- <![endif]-->"]
        (concat contents)
        (concat ["</html>"]))))

(defn js-env-settings
  [context-url dev?]
  (javascript-tag
    (string/join "\n"
                 [(str "var __context = '" context-url "';")
                  (str "var __isDev = " (boolean dev?) ";")])))
