(ns ring.swagger.common
  (:use [clojure.walk :as walk]))

(defn path-vals
  "Returns vector of tuples containing path vector to the value and the value."
  [m]
  (letfn
    [(pvals [l p m]
       (reduce
         (fn [l [k v]]
           (if (map? v)
             (pvals l (conj p k) v)
             (cons [(conj p k) v] l)))
         l m))]
    (pvals [] [] m)))

(defn assoc-in-path-vals
  "Re-created a map from it's path-vals extracted with (path-vals)."
  [c] (reduce (partial apply assoc-in) {} c))

(defmacro fn->
  "Creates a function that threads on input with some->"
  [& body] `(fn [x#] (some-> x# ~@body)))

(defmacro fn->>
  "Creates a function that threads on input with some->>"
  [& body] `(fn [x#] (some->> x# ~@body)))

(defn ->map
  "Converts a map-like form (list of tuples, record a map) into a map."
  [m] (into {} m))

(defn remove-empty-keys
  "removes empty keys from a map"
  [m] (into {} (filter (fn-> second nil? not) m)))

(defn name-of
  "Returns name of a Var, String, Named object or nil"
  [x]
  (cond
    (var? x) (-> x meta :name name)
    (string? x) x
    (instance? clojure.lang.Named x) (name x)
    :else nil))

(defn value-of
  "Extracts value of for var, symbol or returns itself"
  [x]
  (cond
    (var? x) (var-get x)
    (symbol? x) (eval x)
    :else x))

(defmacro re-resolve
  "Extracts original var from a (potemkined) var or a symbol or returns nil"
  [x]
  (let [evaluated (if (symbol? x) x (eval x))
        resolved  (cond
                    (var? evaluated)    evaluated
                    (symbol? evaluated) (resolve evaluated)
                    :else       nil)
        metadata  (meta resolved)]
    (if metadata
      (let [s (symbol (str (:ns metadata) "/" (:name metadata)))]
        `(var ~s)))))

(defn eval-re-resolve [x] (eval `(re-resolve ~x)))

(defn extract-parameters
  "Extract parameters from head of the list. Parameters can be:
     1) a map (if followed by any form) [{:a 1 :b 2} :body] => {:a 1 :b 2}
     2) list of keywords & values   [:a 1 :b 2 :body] => {:a 1 :b 2}
     3) else => {}
   Returns a tuple with parameters and body without the parameters"
  [c]
  {:pre [(sequential? c)]}
  (if (and (map? (first c)) (> (count c) 1))
    [(first c) (rest c)]
    (if (keyword? (first c))
      (let [parameters (->> c
                         (partition 2)
                         (take-while (comp keyword? first))
                         (mapcat identity)
                         (apply hash-map))
            form       (drop (* 2 (count parameters)) c)]
        [parameters form])
      [{} c])))
