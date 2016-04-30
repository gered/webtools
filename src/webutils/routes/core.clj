(ns webtools.routes.core
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

(defmacro wrap-middleware
  "Applies middleware to a handler. Intended to be used to wrap a subset of
   Compojure routes with middleware. The middleware is only run if one of the
   routes being wrapped is matched against the current request."
  [handler & middlewares]
  (let [middleware-forms (map #(concat '(compojure.core/wrap-routes) %) middlewares)]
    `(-> ~handler
         ~@middleware-forms)))
