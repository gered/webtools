(ns webtools.cljs.utils
  (:import
    goog.History)
  (:require
    [clojure.string :as string]
    [clojure.walk :refer [keywordize-keys]]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [secretary.core :as secretary]
    [webtools.cljs.dom :as dom]))

(defn- get-hidden-field-value
  [hidden-field-id]
  (if-let [hidden-field (dom/element-by-id hidden-field-id)]
    (.-value hidden-field)))

(defn get-anti-forgery-token
  []
  ; bunch of common names for this csrf token that i've seen used
  ; ring's own anti-forgery middleware has a helper which outputs
  ; an <input type="hidden"> with the id "__anti-forgery-token"
  (->> [(dom/get-metatag-content "anti-forgery-token")
        (dom/get-metatag-content "__anti-forgery-token")
        (dom/get-metatag-content "csrf-token")
        (get-hidden-field-value "anti-forgery-token")
        (get-hidden-field-value "__anti-forgery-token")
        (get-hidden-field-value "csrf-token")]
       (remove nil?)
       (first)))

(defn dev?
  []
  (if (undefined? js/__isDev)
    false
    (boolean js/__isDev)))

(defn context-url
  []
  (if (undefined? js/__context)
    ""
    (str js/__context)))

(defn supports-websockets?
  []
  (not (-> (dom/select-element "html")
           (dom/has-class? "no-websockets"))))

(defn old-ie?
  []
  (-> (dom/select-element "html")
      (dom/has-class? "old-ie")))

(defn ->url
  [url]
  (-> (str (context-url) url)
      (string/replace #"(/+)" "/")))

(defn hook-browser-navigation!
  []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn redirect!
  [secretary-url]
  (-> (.-location js/window)
      (set! secretary-url)))

(defn pprint-json
  [x]
  (.stringify js/JSON (clj->js x) nil "  "))
