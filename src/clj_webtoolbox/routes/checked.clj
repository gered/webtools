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
  "Wraps any number of routes with one or more filters which are run before any of the
   wrapped routes are executed. If any of the filters return nil, fail-response is
   returned by the server instead of the running a route handler. Filter functions
   should accept a Ring request as the first argument and return a Ring request map
   if the filter logic passed. The same Ring request map is threaded through all of
   the filters before eventually being passed on to the route handler."
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
  "Used in place of a normal Compojure route handler's body. Applies a series of
   filters to the route/request parameters before finally running the handler
   body itself. All of the filters used should accept a Ring request as the first
   argument and return a filtered Ring request, or nil if the filter failed. Use
   routefn at the end to wrap your actual route handler logic. The arguments it
   receives are destructured the same as with a normal Compojure route, but the
   request's :safe-params map is used instead of :params to find the values to
   bind to the arguments listed. If any of the filters fail, the :on-fail response
   is returned (or a default HTTP 500 response if not specified)."
  {:arglists '([& body]
               [:on-fail fail-response & body])}
  [& body]
  (let [has-fail-response? (= :on-fail (first body))
        fail-response      (if has-fail-response? (second body) default-check-error-response)
        body               (if has-fail-response? (drop 2 body) body)]
    `(fn [request#]
       (threaded-checks request# ~body ~fail-response))))

(defmacro checked
  "Convenience method of defining a 'checked' Compojure route. Place this at the
   start of the Compojure route definition (before GET, PUT, etc) and omit the
   params vector after the route path. The route handler body is automatically
   wrapped in a call to checked-route, so you just define the contents of
   it as you would if you were using checked-route manually."
  {:arglists '([method-fn path & body]
                [method-fn path :on-fail fail-response & body])}
  [method-fn path & body]
  `(~method-fn ~path []
     (checked-route ~@body)))

(defmacro routefn
  "The final form present in a checked-route call. The forms in body are executed
   only if none of the previous filters failed. The arguments vector is destructured
   the same was as normal Compojure route parameter destructuring is done, except
   that parameters come from the request's :safe-params map instead."
  [request args & body]
  (if (vector? args)
    `(let [~@(destructure-route-bindings args :safe-params request)] ~@body)
    `(let [~args ~request] ~@body)))

(defn safe
  "Marks one or more parameters as safe (copying them from the request's :params
   map to :safe-params)."
  [request & params]
  (let [safe-params (select-keys (:params request) params)]
    (update-in request [:safe-params] merge safe-params)))

(defn validate
  "Validates the specified parameter using function f which gets passed the value
   of the parameter and any additional arguments given. If f returns a 'truthy' value
   the parameter is marked safe."
  [request param f & args]
  (if (apply f (get-in request [:params param]) args)
    (safe request param)))

(defn transform
  "Transforms the specified parameter using function f which gets passed the value
   of the parameter and any additional arguments given. Note that this does not
   mark a parameter safe after transformation. This is intended to be used to
   perform any pre-validation transformations if necessary."
  [request param f & args]
  (apply update-in request [:params param] f args))
