(ns clj-webtoolbox.response-helpers
  (:require
    [clj-webtoolbox.response :as response]))

(defn- ->response [body status]
  (-> (response/content body)
      (response/status status)))

(defn ok [& [body]] (->response body 200))
(defn created [& [body]] (->response body 201))
(defn accepted [& [body]] (->response body 202))
(defn no-content [& [body]] (->response body 204))

(defn moved [& [body]] (->response body 301))
(defn found [& [body]] (->response body 302))

(defn bad-request [& [body]] (->response body 400))
(defn unauthorized [& [body]] (->response body 401))
(defn forbidden [& [body]] (->response body 403))
(defn not-found [& [body]] (->response body 404))

(defn error [& [body]] (->response body 500))
