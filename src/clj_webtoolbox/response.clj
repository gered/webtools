(ns clj-webtoolbox.response
  "Response helpers to compliment those provided by ring.util.response. Some
   of these are mostly just copies with maybe slight tweaks here and there
   just so that application code need not include both namespaces unless
   absolutely needed for the not-so-commonly-used functions."
  (:require
    [cheshire.core :as json]))

(def ^:private base-response
  {:status  200
   :headers {}
   :body    nil})

(defn content
  "Returns a Ring response where the body is set to the value given."
  ([body] (content base-response body))
  ([resp body]
    (assoc resp :body body)))

(defn status
  "Returns a Ring response where the HTTP status is set to the status code given."
  ([http-status] (status base-response http-status))
  ([resp http-status]
    (assoc resp :status http-status)))

(defn header
  "Returns a Ring response where the HTTP header/value is added to the existing
   set of headers in the response. The header name given should be a string."
  ([name value] (header base-response name value))
  ([resp name value]
    (update-in resp [:headers] assoc name value)))

(defn headers
  "Returns a Ring response where the map of HTTP headers is added to the existing
   set of headers in the response. The map should contain string keys."
  ([m] (headers base-response m))
  ([resp m]
    (update-in resp [:headers] merge m)))

(defn content-type
  "Returns a Ring response where the content type is set to the value given."
  ([type] (content-type base-response type))
  ([resp type]
    (header resp "Content-Type" type)))

(defn plain-text
  "Returns a Ring response containing plain text content."
  [body]
  (-> (content-type "text/plain; charset=utf-8")
      (content body)))

(defn html
  "Returns a Ring response containing HTML content."
  [body]
  (-> (content-type "text/html; charset=utf-8")
      (content body)))

(defn xml
  "Returns a Ring response containing XML content."
  [body]
  (-> (content-type "text/xml; charset=utf-8")
      (content body)))

(defn json
  "Returns a Ring response containing JSON content. The body passed in will
   be automatically converted to a JSON format equivalent."
  [body]
  (-> (content-type "application/json; charset=utf-8")
      (content (json/generate-string body))))

(defn edn
  "Returns a Ring response containing EDN content. The body passed in should
   be a Clojure data structure and will be serialized using pr-str."
  [body]
  (-> (content-type "application/edn; charset=utf-8")
      (content (pr-str body))))

(defn cookie
  "Returns a Ring response where a cookie is appended to any existing
   cookies set in the existing response."
  [resp name value & [opts]]
  (assoc-in resp [:cookies name] (merge (:value value opts))))
