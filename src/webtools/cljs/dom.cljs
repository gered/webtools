(ns webtools.cljs.dom)

(defn element-by-id
  [id]
  (.getElementById js/document id))

(defn select-all-elements
  [selector]
  (vec (.querySelectorAll js/document selector)))

(defn select-element
  [selector]
  (.querySelector js/document selector))

(defn get-metatag-content
  [metatag-name]
  (if-let [tag (select-element (str "meta[name='" metatag-name "']"))]
    (.-content tag)))

(defn has-class?
  [element class-name]
  (if (.-classList element)
    (.contains (.-classList element) class-name)
    (doto (js/RegExp. (str "(^| )" class-name "( |$)") "gi")
      (.test (.-className element)))))
