(ns clj-webtoolbox.routes.checked
  (:require
    [clojure.edn :as edn]
    [compojure.core :refer [routing]]
    [ring.util.response :refer [response?]]
    [ring.util.request :refer [body-string]]
    [schema.core :as s]
    [cheshire.core :as json]
    [clj-webtoolbox.response :as response]
    [clj-webtoolbox.routes.core :refer [destructure-route-bindings]]))

(defmacro threaded-checks [request checks fail-response]
  `(or (some-> ~request ~@checks)
       (if (response? ~fail-response)
         ~fail-response
         (~fail-response ~request))))

(def default-wrap-checks-error-response
  (-> (response/content "Handler checks did not all pass.")
      (response/status 500)))

(def default-check-error-response
  (-> (response/content "Route checks did not all pass.")
      (response/status 500)))

(defmacro wrap-checks
  "Wraps a handler (e.g. subset of routes) with one or more filters ('checks') which
   are run before to handler is run. If any of the filters return nil, fail-response
   is returned by the server instead of running the wrapped handler. Filter functions
   should accept a Ring request as the first argument and return a Ring request map
   if the filter logic passed. The same Ring request map is threaded through all of
   the filters before eventually being passed on to the handler. If a failure
   response is not specified via :on-fail, then a default HTTP 500 response is used."
  {:arglists '([checks & body]
               [checks :on-fail fail-response & body])}
  [checks & body]
  (let [has-fail-response? (= :on-fail (first body))
        fail-response      (if has-fail-response? (second body) default-wrap-checks-error-response)
        body               (if has-fail-response? (drop 2 body) body)]
    `(fn [request#]
       (let [result# (threaded-checks request# ~checks ~fail-response)]
         (if (response? result#)
           result#
           (routing result# ~@body))))))

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
   map to :safe-params). The params argument is a vector of keywords referring
   to parameters to be marked as safe. Each parameter itself can refer to a nested
   value by specifying the parameter as another vector of keywords. All params are
   assumed to be located under :params in the request unless otherwise specified
   via parent."
  ([request params] (safe request :params params))
  ([request parent params]
    (reduce
      (fn [request param]
        (let [src-k (if (sequential? param) (concat [parent] param) [parent param])
              dst-k (if (sequential? param) (concat [:safe-params] param) [:safe-params param])]
          (assoc-in request dst-k (get-in request src-k))))
      request
      params)))

(defn safe-body
  "Marks the body as safe. The request's :body value is either copied into
   :safe-params under the :body key, or is merged into the existing :safe-params
   map (controlled via the copy-into-params? argument)."
  [request & [copy-into-params?]]
  (if copy-into-params?
    (assoc-in request [:safe-params :body] (:body request))
    (update-in request [:safe-params] merge (:body request))))

(defn validate
  "Validates the specified parameter using function f which gets passed the value
   of the parameter. If f returns a 'truthy' value the parameter is marked safe.
   A nested parameter can be checked by specifying it as a vector of keywords.
   Parameters are assumed to be located under :params in the request unless
   otherwise specified via parent."
  ([request param f] (validate request :params param f))
  ([request parent param f]
    (let [k (if (sequential? param) (concat [parent] param) [parent param])]
      (if (f (get-in request k))
        (safe request parent [param])))))

(defn validate-schema
  "Validates the specified parameter by checking it against the given schema.
   If it validates, the parameter is marked safe. Follows the same rules for
   param/parent handling as validate."
  ([request param schema] (validate-schema request :params param schema))
  ([request parent param schema]
    (let [k (if (sequential? param) (concat [parent] param) [parent param])]
      (if (nil? (s/check schema (get-in request k)))
        (safe request parent [param])))))

(defn validate-body
  "Validates the request body using function f which gets passed the body of
   the request. If f returns a 'truthy' value, the body is marked safe. You
   likely will want to transform the body first before validation."
  [request f & [copy-into-params?]]
  (if (f (:body request))
    (safe-body request copy-into-params?)))

(defn validate-body-schema
  "Validates the request body by checking it against the given schema. If it
   validates, the body is marked safe. You likely will want to transform the body
   first before validation."
  [request schema & [copy-into-params?]]
  (if (nil? (s/check schema (:body request)))
    (safe-body request copy-into-params?)))

(defn transform
  "Transforms the specified parameter using function f which gets passed the value
   of the parameter. A nested parameter can be transformed by specifying it as a
   vector of keywords. Parameters are assumed to be located under :params in the
   request unless otherwise specified via parent. Note that this does not mark a
   parameter safe after transformation. This is intended to be used to perform any
   pre-validation transformations if necessary."
  ([request param f] (transform request :params param f))
  ([request parent param f]
    (let [k (if (sequential? param) (concat [parent] param) [parent param])]
      (update-in request k f))))

(defn transform-string-body
  "Transforms the body of the request, converting it to a string. The returned
   request will contain a string :body value."
  [request]
  (assoc request :body (body-string request)))

(defn transform-json-body
  "Transforms the body of the request as JSON. The returned request will contain
   a :body value with the parsed JSON result."
  [request]
  (let [body (body-string request)]
    (assoc request :body (json/parse-string body true))))

(defn transform-edn-body
  "Transforms the body of the request as EDN. The returned request will contain
   a :body value with the parsed EDN result."
  [request]
  (let [body (body-string request)]
    (assoc request :body (edn/read-string body))))

(defn auto-transform-body
  "Automatically transforms the body of the request, parsing it as JSON or EDN
   based on the request's Content-Type header. The returned request will contain
   a :body value with the parsed result, or the original value if the
   Content-Type header was not recognized."
  [request]
  (let [body (body-string request)]
    (condp = (:content-type request)
      "application/json" (assoc request :body (json/parse-string body true))
      "application/edn"  (assoc request :body (edn/read-string body))
      request)))