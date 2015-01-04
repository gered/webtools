(ns clj-webtoolbox.routes.checked
  (:require
    [compojure.core :refer [routing]]
    [ring.util.response :refer [response?]]
    [clj-webtoolbox.response :as response]
    [clj-webtoolbox.routes.core :refer [destructure-route-bindings]]))

(defmacro threaded-checks [request checks fail-response]
  `(or (some-> ~request ~@checks)
       (if (response? ~fail-response)
         ~fail-response
         (~fail-response ~request))))

(defmacro checked-routes
  [checks fail-response & routes]
  `(fn [request#]
     (let [result# (threaded-checks request# ~checks ~fail-response)]
       (if (response? result#)
         result#
         (routing result# ~@routes)))))

(def default-check-error-response
  (-> (response/content "Route checks did not all pass.")
      (response/status 500)))

(defmacro checked-route
  {:arglists '([& body]
                [:on-fail fail-response & body])}
  [& body]
  (let [has-fail-response? (= :on-fail (first body))
        fail-response      (if has-fail-response? (second body) default-check-error-response)
        body               (if has-fail-response? (drop 2 body) body)]
    `(fn [request#]
       (threaded-checks request# ~body ~fail-response))))

(defmacro checked
  {:arglists '([method-fn path & body]
                [method-fn path :on-fail fail-response & body])}
  [method-fn path & body]
  `(~method-fn ~path []
     (checked-route ~@body)))

(defmacro routefn [request args & body]
  (if (vector? args)
    `(let [~@(destructure-route-bindings args :safe-params request)] ~@body)
    `(let [~args ~request] ~@body)))

(defn safe
  [request & params]
  (let [safe-params (select-keys (:params request) params)]
    (update-in request [:safe-params] merge safe-params)))

(defn validate
  [request param f & args]
  (if (apply f (get-in request [:params param]) args)
    (safe request param)))

(defn transform
  [request param-k f & args]
  (apply update-in request [:params param-k] f args))