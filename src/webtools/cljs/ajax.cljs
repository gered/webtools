(ns webtools.cljs.ajax
  (:require
    [clojure.walk :refer [stringify-keys keywordize-keys]]
    [ajax.core :as ajax]
    [webtools.cljs.utils :refer [->url get-anti-forgery-token]]))

(defn add-headers!
  [headers]
  (let [headers     (stringify-keys headers)
        interceptor (ajax/to-interceptor
                      {:request #(assoc % :headers (merge (:headers %) headers))})]
    (swap! ajax/default-interceptors (partial cons interceptor))
    headers))

(defn add-csrf-header!
  []
  (let [token (get-anti-forgery-token)]
    (if token (add-headers! {"X-CSRF-Token" token}))
    token))

(defn GET
  [url & {:keys [format params on-success on-error on-complete keywords? headers]}]
  (let [url   (->url url)
        json? (or (= :json format)
                  (nil? format))]
    (ajax/GET url
              (merge
                {:format (or format :json)}
                (if json? {:keywords? (or keywords? true)})
                (if params {:params params})
                (if headers {:headers (stringify-keys headers)})
                (if on-success {:handler on-success})
                (if on-error {:error-handler on-error})
                (if on-complete {:finally on-complete})
                ))))

(defn POST
  [url & {:keys [format params body on-success on-error on-complete keywords? headers]}]
  (let [url   (->url url)
        json? (or (= :json format)
                  (nil? format))]
    (ajax/POST url
               (merge
                 {:format (or format :json)}
                 (if json? {:keywords? (or keywords? true)})
                 (if params {:params params})
                 (if body {:body body})
                 (if headers {:headers (stringify-keys headers)})
                 (if on-success {:handler on-success})
                 (if on-error {:error-handler on-error})
                 (if on-complete {:finally on-complete})))))

(defn fetch!
  [destination-atom url & {:keys [format params keywords? on-error headers transform]}]
  (let [transform (or transform identity)]
    (GET url
         :format format
         :params params
         :keywords? keywords?
         :on-error on-error
         :headers headers
         :on-success #(reset! destination-atom (transform %)))))
