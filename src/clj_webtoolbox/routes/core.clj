(ns clj-webtoolbox.routes.core
  (:require
    compojure.core))

;; -----------------------------------------------------------------------------
;; these are taken from compojure.core.

(defn- assoc-&-binding [binds req params-parent sym]
  (assoc binds sym `(dissoc (~params-parent ~req)
                            ~@(map keyword (keys binds))
                            ~@(map str (keys binds)))))

(defn- assoc-symbol-binding [binds req params-parent sym]
  (assoc binds sym `(get-in ~req [~params-parent ~(keyword sym)]
                            (get-in ~req [~params-parent ~(str sym)]))))

(defn destructure-route-bindings
  "Given a vector of Compojure route parameter bindings, expands them to a vector
   of symbols that can be used in, e.g. a macro, to provide Compojure-like
   destructuring elsewhere."
  ([args req] (destructure-route-bindings args :params req))
  ([args params-parent req]
    (loop [args args, binds {}]
      (if-let [sym (first args)]
        (cond
          (= '& sym)    (recur (nnext args) (assoc-&-binding binds req params-parent (second args)))
          (= :as sym)   (recur (nnext args) (assoc binds (second args) req))
          (symbol? sym) (recur (next args) (assoc-symbol-binding binds req params-parent sym))
          :else         (throw (Exception. (str "Unexpected binding: " sym))))
        (mapcat identity binds)))))

;; -----------------------------------------------------------------------------

(defmacro with-middleware
  "Applies a sequence of middleware functions to a series of routes wrapped
   by this macro. For best results, use _within_ a compojure.core/context
   call. Remember that middleware will be applied in the reverse order
   that it is specified in."
  [middlewares & routes]
  `(-> (compojure.core/routes ~@routes)
       ~@middlewares))