(ns midadmin-platform.jmxoperator
  (:require [clj-http.client :as client]
            [clj-http.util :as util]
            [taoensso.timbre :as timbre]
            [clojure.string :as cstr]
            [cheshire.core :refer :all]
            [environ.core :refer [env]]))

(defn get-url-from-asset [cell]
  (let [[_ host port _] (re-matches #"http://(.+):(.+)/(.*)" (first (cstr/split (:CONSOLE_URL (:body (client/get (str "http://99.12.139.73:8080/itm-dashboard/rest/public/getAssetDetailByCellId/" cell)))) #" ")))]
    (str "http://" host ":" port "/jolokia")))

(defn get-url-by-cell [cell]
  (cond
    (env :dev) "http://localhost:9060/jolokia"
    :else (get-url-from-asset cell)))

(defn get-user-info-by-cell [cell]
  ["midadmin" "midadminPassw0rd"] )

(defn api-action [method path cell & [opts]]
  (client/with-connection-pool {:timeout 5 :threads 4 :insecure? true :default-per-route 2}
    (let [req-without-url (merge {:method method :basic-auth (get-user-info-by-cell cell) :content-type :json :accept :json :as :json} opts)
          resp1 (client/request
                 (merge {:url (str (get-url-by-cell cell) path)} req-without-url))]
      (if (contains? #{301 302 303 304} (:status resp1)) (client/request (merge {:url (get-in resp1 [:headers "Location"])} req-without-url)) resp1))))

(defn agent-info [cell]
  (:value (:body (api-action :get "/version" cell))))

(defn get-cell-cfg-mbean [cell]
  (first (:value (:body (api-action :post "" cell {:body (encode {:type "search" :mbean (str "WebSphere:name=ConfigService,process=dmgr,cell=" cell ",*")})})))))

(defn objname-to-string [{obj :objectName}]
  (let [[_ _ cfg-id _] (re-matches #"Websphere:(.*)(_Websphere_Config_Data_Id=.+),(.*)" obj)] (str "Websphere:" cfg-id)))

(defn escape-cfg-string [cfg-string]
  (cstr/escape cfg-string {\, "!!!,",\= "!!!=",\: "!!!!:"}))

(defn get-attr-info [cell attr-type]
  (let [cell-cfg-mbean (get-cell-cfg-mbean cell)]
    (:value (:body (api-action :post "" cell {:body (encode {:type "exec" :mbean cell-cfg-mbean :operation "getAttributesMetaInfo" :arguments attr-type})})))))

(defn contexturi-to-target [contexturi]
  (let [split-uri (map (fn [s] (if (contains? #{"cells" "nodes" "servers" "clusters"} s) (cstr/replace s #"s$" "") s)) (cstr/split contexturi #"/"))]
    (apply hash-map split-uri)))

(defn target-to-contexturi [m type]
  (let [target (select-keys m [:cell :node :cluster :server])
        contexturi (cond
          (contains? target :cluster) (str "cells/" (:cell target) "/clusters/" (:cluster target) "|cluster.xml")
          (contains? target :server) (str "cells/" (:cell target) "/nodes/" (:node target) "/servers/" (:server target) "|server.xml")
          (contains? target :node) (str "cells/" (:cell target) "/nodes/" (:node target) "|node.xml")
          :else (str "cells/" (:cell target) "|cell.xml"))]
    (cond
      (= type "resource") (cstr/replace contexturi #"cell.xml|node.xml|server.xml|cluster.xml" "resources.xml")
      :else contexturi)))

(defn jolokia-to-output [cfg-obj-prop-list]
  (reduce merge (map (fn [{k :name v :value}]
                       (cond
                         (= k "_Websphere_Config_Data_Id") (merge (contexturi-to-target (:contextUri v)) {:id (str (:contextUri v) "|" (:href v))})
                         (= k "_Websphere_Config_Data_Type") (merge {k v} {:configType v})
                         (and (vector? v) (map? (first v))) {k (jolokia-to-output v)}
                         (and (vector? v) (vector? (first v))) {k (map jolokia-to-output v)}
                         :else {k v}))
                     cfg-obj-prop-list)))

(def schema-to-backend-map {(type "String") "S",
                            (type (int 1)) "I",
                            (type 1) "L",
                            (type (float 1.1)) "F",
                            (type 1.1) "D",
                            (type true) "B",
                            (type ["String"]) "AoS"
                            })

(def objref-keys #{:provider :pretest})
(def non-attributes #{:id :configType :parentId :cell :node :server :cluster})

(defn convert-str-to-jolokia [t k v]
  (if (cstr/blank? v) (str (name k) "=") (str (name k) "=" t ":" (escape-cfg-string v))))

(defn input-to-jolokia [m]
  (cstr/join "," (map (fn [[k v]]
                        (cond
                          (contains? objref-keys k) (str (name k) "=ObjRef:" (escape-cfg-string v))
                          :else (let [t (get schema-to-backend-map (type v))]
                                  (cond
                                    (= t "S") (convert-str-to-jolokia t k v)
                                    (= t "AoS") (convert-str-to-jolokia t k (cstr/join " " v))
                                    :else  (str (name k) "=" t ":" v)))))
                      m)))

(defn jmx-resolve [cell-cfg-mbean cfg-type m]
  "Generate jmx resolve operation arguments based on request map"
  {:type "exec",
   :mbean cell-cfg-mbean,
   :operation "resolve(com.ibm.websphere.management.Session,java.lang.String)",
   :arguments ["[null]",cfg-type]})

(defn jmx-resolve-with-scope [cell-cfg-mbean cfg-type m target-type]
  {:type "exec",
   :mbean cell-cfg-mbean,
   :operation "resolve(com.ibm.websphere.management.Session,javax.management.ObjectName,java.lang.String)",
   :arguments ["[null]",(str "Websphere:_Websphere_Config_Data_Id=" (target-to-contexturi m target-type)),cfg-type]})

(defn jmx-query-cfg-with-scope [cell-cfg-mbean cfg-type m target-type]
  {:type "exec",
   :mbean cell-cfg-mbean,
   :operation "queryConfigObjectsAtCurrentScope",
   :arguments ["[null]",(str "Websphere:_Websphere_Config_Data_Id=" (target-to-contexturi m target-type)),cfg-type]})

(defn jmx-get-attributes-all [cell-cfg-mbean m]
  {:type "exec",
   :mbean cell-cfg-mbean,
   :operation "getAttributes",
   :arguments ["[null]",(if (contains? m :id) (str "Websphere:_Websphere_Config_Data_Id=" (:id m)) (objname-to-string m)),"[null]",false]})

(defn jmx-get-attributes-all-recursive [cell-cfg-mbean m]
  {:type "exec",
   :mbean cell-cfg-mbean,
   :operation "getAttributes",
   :arguments ["[null]",(if (contains? m :id) (str "Websphere:_Websphere_Config_Data_Id=" (:id m)) (objname-to-string m)),"[null]",true]})

(defn jmx-set-attributes [cell-cfg-mbean m]
  {:type "exec",
   :mbean cell-cfg-mbean,
   :operation "setAttributes",
   :arguments ["[null]",(str "Websphere:_Websphere_Config_Data_Id=" (:id m)), (input-to-jolokia (remove #(contains? non-attributes (key %)) m))]})

(defn jmx-delete-cfg-data [cell-cfg-mbean m]
  {:type "exec",
   :mbean cell-cfg-mbean,
   :operation "deleteConfigData",
   :arguments ["[null]",(str "Websphere:_Websphere_Config_Data_Id=" (:id m))]})

(defn jmx-create-cfg-data [cell-cfg-mbean type m]
  {:type "exec",
   :mbean cell-cfg-mbean,
   :operation "createConfigData",
   :arguments ["[null]",(str "Websphere:_Websphere_Config_Data_Id=" (if (contains? m :parentId) (:parentId m) (target-to-contexturi m "self"))),(:name m),type,(input-to-jolokia (remove #(contains? non-attributes (key %)) m))]})

(defn jmx-create-cfg-data-by-template [cell-cfg-mbean m template]
  {:type "exec",
   :mbean cell-cfg-mbean,
   :operation "createConfigDataByTemplate",
   :arguments ["[null]",(str "Websphere:_Websphere_Config_Data_Id=" (if (contains? m :parentId) (:parentId m) (target-to-contexturi m "self"))) ,(last (cstr/split (cstr/join (filter #(re-find #"_Websphere_Config_Data_Type=" %) (cstr/split template #","))) #"=")),(input-to-jolokia (remove #(contains? non-attributes (key %)) m)),template]})

(defn jmx-exec [cell req]
  (let [rs (:body (api-action :post "" cell {:body (encode req)}))
        error (:error rs)]
    (if (nil? error)
      (if (seq? rs)
        (map (fn [{value :value}] value) rs)
        (:value rs))
      (do (timbre/error error) rs))))

(defn jmx-list [m cfg-type]
  (let [cell-cfg-mbean (get-cell-cfg-mbean (:cell m))]
    (map jolokia-to-output (jmx-exec (:cell m) (map #(jmx-get-attributes-all-recursive cell-cfg-mbean %) (jmx-exec (:cell m) (jmx-query-cfg-with-scope cell-cfg-mbean cfg-type m "self")))))))

(defn jmx-list-recursive [m cfg-type]
  (let [cell-cfg-mbean (get-cell-cfg-mbean (:cell m))]
    (map jolokia-to-output (jmx-exec (:cell m) (map #(jmx-get-attributes-all-recursive cell-cfg-mbean %) (jmx-exec (:cell m) (jmx-resolve-with-scope cell-cfg-mbean cfg-type m "self")))))))

(defn jmx-update [m]
  (let [cell-cfg-mbean (get-cell-cfg-mbean (:cell m))
        reqs (if (vector? m) (map #(jmx-set-attributes cell-cfg-mbean %) m)
                 (jmx-set-attributes cell-cfg-mbean m))]
    (jmx-exec (:cell m) reqs)))

(defn jmx-delete [m]
  (let [cell-cfg-mbean (get-cell-cfg-mbean (:cell m))
        reqs (if (vector? m) (map #(jmx-delete-cfg-data cell-cfg-mbean %) m)
                 (jmx-delete-cfg-data cell-cfg-mbean m))]
    (jmx-exec (:cell m) reqs)))

(defn jmx-new [m template]
  (timbre/info "get template:" template)
  (timbre/info "get data map:" m)
  (let [cell-cfg-mbean (get-cell-cfg-mbean (:cell m))
        reqs (if (vector? m) (map #(jmx-create-cfg-data-by-template cell-cfg-mbean % template) m)
                 (jmx-create-cfg-data-by-template cell-cfg-mbean m template))]
    (jmx-exec (:cell m) reqs)))


(defn system-template? [{objname :objectName}]
  (re-find #"_Websphere_Config_Data_Id=templates/system" objname))

(defn jdbc-provider-only? [{objname :objectName}]
  (re-find #"jdbc-resource-provider-only-templates.xml" objname))

(defn remove-session-in-objectname [{objname :objectName}]
  {:objectName (cstr/replace-first objname #"_WEBSPHERE_CONFIG_SESSION=anonymous[0-9]+," "")})

(defn jmx-query [m cfg-type]
  (let [cell-cfg-mbean (get-cell-cfg-mbean (:cell m))]
    (map remove-session-in-objectname (jmx-exec (:cell m) (jmx-query-cfg-with-scope cell-cfg-mbean cfg-type m "resource")))))

(defn jmx-query-recursive [m cfg-type]
  (let [cell-cfg-mbean (get-cell-cfg-mbean (:cell m))]
    (map remove-session-in-objectname (jmx-exec (:cell m) (jmx-resolve-with-scope cell-cfg-mbean cfg-type m "self")))))

(defn query-template [cell type]
  (let [cell-cfg-mbean (get-cell-cfg-mbean cell)
        templates (map remove-session-in-objectname (filter system-template? (:value (:body (api-action :post "" cell {:body (encode {:type "exec" :mbean cell-cfg-mbean :operation "queryTemplates" :arguments ["[null]",type]})})))))]
    (if (= "JDBCProvider" type)
      (filter jdbc-provider-only? templates)
      templates)))

(defn same-provider-type? [type p]
  (= type (get p "providerType")))

(defn get-datasource-template-by-provider [m]
  "m is a map with cell name and the jdbc provider id,e.g. {:cell cell01, :id jp1}"
  (let [cell-cfg-mbean (get-cell-cfg-mbean (:cell m))
        ds-templates (map jolokia-to-output (jmx-exec (:cell m) (map #(jmx-get-attributes-all-recursive cell-cfg-mbean %) (query-template (:cell m) "DataSource"))))
        p (get (jolokia-to-output (jmx-exec (:cell m) (jmx-get-attributes-all cell-cfg-mbean m))) "providerType")]
    (filter #(same-provider-type? p %) ds-templates)))

(defn get-cfg-types [cell]
  "list all supported config types for a specific cell"
  (let [cell-cfg-mbean (get-cell-cfg-mbean cell)]
    (:value (:body (api-action :post "" cell {:body (encode {:type "exec" :mbean cell-cfg-mbean :operation "getSupportedConfigObjectTypes" :arguments []})})))))
