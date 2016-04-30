(ns webtools.utils)

(defmacro pred->
  "Threads exp through the forms (via ->) as long as (pred exp)
   returns logical true values. Returns whatever exp was at
   the time threading stopped (either due to a false return
   from pred or because all forms were executed)."
  [expr pred & forms]
  (let [g     (gensym)
        pstep (fn [step]
                `(if (~pred ~g)
                   (-> ~g ~step)
                   ~g))]
    `(let [~g ~expr
           ~@(interleave
               (repeat g)
               (map pstep forms))]
       ~g)))

(defn request?
  "True if the supplied value is a valid request map."
  [req]
  ;; TODO: probably don't need this many tests, just being overly cautious about
  ;;       making sure this won't confuse a response map with a request map
  (and (map? req)
       (keyword? (:scheme req))
       (keyword? (:request-method req))
       (contains? req :server-port)
       (contains? req :server-name)
       (contains? req :remote-addr)
       (contains? req :uri)))