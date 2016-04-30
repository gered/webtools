(ns webtools.session
  "Convenience helper functions for applying session data to Ring response maps."
  (:refer-clojure :exclude [set dissoc assoc assoc-in update-in]))

(defn set
  "Returns an updated Ring response with session information added. This will
   overwrite existing session data."
  [resp session]
  (clojure.core/assoc resp :session session))

(defn set-from-request
  "Returns an updated Ring response with session information added from the
   Ring request. You can use this to set the initial session to be modified
   by subsequent functions to modify the response's session map (so as to
   modify the existing session rather then overwrite it completely)."
  [resp request]
  (clojure.core/assoc resp :session (:session request)))

(defn assoc
  "Returns an updated Ring response with the new key/value set in the
   response's session map."
  [resp k v]
  (clojure.core/assoc-in resp [:session k] v))

(defn dissoc
  "Returns an updated Ring response with the key removed from the response's
   session map."
  [resp k]
  (clojure.core/update-in resp [:session] clojure.core/dissoc k))

(defn assoc-in
  "Returns an updated Ring response with the new key/value set in the
   response's session map. ks should be a vector of keywords referring
   to a nested value to set in the session. Any levels that do not exist
   will be created."
  [resp ks v]
  (clojure.core/assoc-in resp (concat [:session] ks) v))

(defn update-in
  "Returns an updated Ring response where a specific value within the
   existing response's session map is 'updated' using function f which
   takes the existing value along with any supplied args and should
   return the new value. ks should be a vector of keywords referring
   to a nested value to update. Any levels that do not exist will be
   created."
  [resp ks f & args]
  (apply clojure.core/update-in resp (concat [:session] ks) f args))

(defn clear
  [resp]
  "Returns an updated Ring response which will cause the session to be
   completely cleared."
  (clojure.core/assoc resp :session nil))
