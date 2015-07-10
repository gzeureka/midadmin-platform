(ns midadmin-platform.schema
  (:require [schema.core :as s]
            [schema.utils :as su]
            [schema.macros :as sm]))

(defn- explicit-key [k] (if (s/specific-key? k) (s/explicit-schema-key k) k))

(defn- explicit-key-set [ks]
  (reduce (fn [s k] (conj s (explicit-key k))) #{} ks))

(defn- key-in-schema [m k]
  (cond
    (contains? m k) k
    (contains? m (s/optional-key k)) (s/optional-key k)
    (contains? m (s/required-key k)) (s/required-key k)
    (and (s/specific-key? k) (contains? m (s/explicit-schema-key k))) (s/explicit-schema-key k)
    :else k))

(defn- get-in-schema [m k & [default]]
  (get m (key-in-schema m k) default))

(s/defschema s-not-empty (s/both s/Str (s/pred not-empty 'content-needed)))
(s/defschema i-pos (s/both (s/pred #(instance? java.lang.Integer %) 'Integer) (s/pred pos? 'positive-number)))

(defn at-least-one-key? [ks s]
  (some s (explicit-key-set (keys ks))))

(defn depent-key-exist? [pk dk s]
  (if (contains? s pk)
    (contains? s dk)
    true))

(defn exclude-key-not-exist? [pk ek s]
  (if (contains? s pk)
    (not (contains? s ek))
    true))

(defn not-less-than? [k1 k2 s]
  (cond
    (and (contains? s k1) (contains? s k2)) (<= (k1 s) (k2 s))
    :else true))

(s/defschema ID {:id s-not-empty
                 :configType s-not-empty})

(s/defschema TARGET {:cell s-not-empty
                     (s/optional-key :node) s-not-empty
                     (s/optional-key :server) s-not-empty
                     (s/optional-key :cluster) s-not-empty})

(s/defschema TARGET-VALID-RULE (s/both (s/pred #(depent-key-exist? :server :node %) 'server-must-with-appropriate-node)
                                       (s/pred #(exclude-key-not-exist? :cluster :node %) 'cluster-cannot-coexist-with-node)))

(s/defschema STREAMREDIRECT (merge ID {:fileName s-not-empty
                                       (s/optional-key :maxNumberOfBackupFiles) i-pos
                                       (s/optional-key :rolloverSize) i-pos
                                       }))

(s/defschema JVM-P  {(s/optional-key :initialHeapSize) i-pos
                     (s/optional-key :maximumHeapSize) i-pos
                     (s/optional-key :verboseModeGarbageCollection) s/Bool
                     (s/optional-key :genericJvmArguments) s/Str
                     (s/optional-key :debugArgs) s/Str
                     (s/optional-key :outputStreamRedirect) STREAMREDIRECT
                     (s/optional-key :errorStreamRedirect) STREAMREDIRECT
                     })

(s/defschema JVM-P-VALID-RULE (s/both (s/pred #(at-least-one-key? JVM-P %) 'at-least-one-attribute-must-present) (s/pred #(not-less-than? :initialHeapSize :maximumHeapSize %) 'maximumHeapSize-must-not-less-than-initialHeapSize)))

(s/defschema JVM (s/both (merge ID TARGET JVM-P) (s/both TARGET-VALID-RULE JVM-P-VALID-RULE)))

(s/defschema ThreadPool-P {(s/optional-key :name) s-not-empty
                           (s/optional-key :minimumSize) i-pos
                           (s/optional-key :maximumSize) i-pos
                           })
(s/defschema ThreadPool-P-VALID-RULE (s/both (s/pred #(at-least-one-key? ThreadPool-P %) 'at-least-one-attribute-must-present) (s/pred #(not-less-than? :minimumSize :maximumSize %) 'maximumSize-must-not-less-than-minimumSize)))

(s/defschema ThreadPool (s/both (merge ID TARGET ThreadPool-P) (s/both TARGET-VALID-RULE ThreadPool-P-VALID-RULE)))

(s/defschema JDBCProvider-P {:name s-not-empty
                             (s/optional-key :classpath) [s/Str]
                             (s/optional-key :nativepath) [s/Str]
                             (s/optional-key :implementationClassName) s-not-empty
                             (s/optional-key :xa) s/Bool
                             })
(s/defschema JDBCProvider (s/both (merge ID TARGET JDBCProvider-P) TARGET-VALID-RULE))
(s/defschema JDBCProvider-NEW {:templateObjectName s-not-empty
                               :newJDBCProvider (s/both (merge TARGET JDBCProvider-P) TARGET-VALID-RULE)})

(s/defschema ConnectionPool-P {(s/optional-key :minimumPoolSize) i-pos
                               (s/optional-key :maximumPoolSize) i-pos
                               })

(s/defschema DataSource-P {:name s-not-empty
                           :jndiName s-not-empty
                           (s/optional-key :connectionPool) ConnectionPool-P
                           })
(s/defschema DataSource (merge ID TARGET DataSource-P))
(s/defschema DataSource-NEW {:templateId ID
                             :providerId ID
                             :newDataSource (merge TARGET DataSource-P)})

(s/defschema JMSConnectionFactory {:id s/Str
                         :cell s/Str
                         :node s/Str
                         :server s/Str
                         :name s/Str
                         :minimumSize s/Int
                         :maximumSize s/Int
                         })

(s/defschema ATTRINFO {:cell s-not-empty
                       :attrtype [s/Str]
                       })

(s/defschema DataSource-Template {:cell s-not-empty
                                  :id s-not-empty
                                  })
