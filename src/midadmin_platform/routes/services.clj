(ns midadmin-platform.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [taoensso.timbre :as timbre]
            [schema.core :as s]
            [midadmin-platform.jmxoperator :as jmx]
            [midadmin-platform.schema :as schema]))

(defapi service-routes
  (ring.swagger.ui/swagger-ui
   "/swagger-ui")
  ;JSON docs available at the /swagger.json route
  (swagger-docs
   {:info {:title "招行中间件管理平台API"
           :version "1.0.0"
           :description "本平台提供中间件管理的API，包括配置和常见操作，支持的中间件包括websphere、jboss和MQ"
           :contact {:name "IBM招行中间件管理平台项目组"
                     :email "chenjf@cn.ibm.com"
                     :url "http://midadmin.cmbchina.com"}}})

  (context* "/api" []
            :tags ["API"]
            :summary "Top service endpoint"
            (context* "/jvm" []
                      :tags ["JVM"]
                      :summary "配置jvm参数的服务"

                      (GET* "/list/:cell" []
                            :summary "获取指定范围的JVM属性值"
                            :path-params [cell :- s/Str]
                            (ok (jmx/jmx-list-recursive {:cell cell} "JavaVirtualMachine")))

                      (GET* "/list/:cell/:node" []
                            :summary "获取指定范围的JVM属性值"
                            :path-params [cell :- s/Str node :- s/Str]
                            (ok (jmx/jmx-list-recursive {:cell cell :node node} "JavaVirtualMachine")))

                      (GET* "/list/:cell/:node/:server" []
                            :summary "获取指定范围的JVM属性值"
                            :path-params [cell :- s/Str node :- s/Str server :- s/Str]
                            (ok (jmx/jmx-list-recursive {:cell cell :node node :server server} "JavaVirtualMachine")))

                      (POST* "/list" []
                             :return [schema/JVM]
                             :summary "获取指定范围的jvm属性值"
                             :body [target schema/TARGET]
                             (ok (jmx/jmx-list-recursive target "JavaVirtualMachine")))

                      (POST* "/update" []
                             :summary "修改指定JVM的属性"
                             :body [jvm schema/JVM]
                             (timbre/info jvm)
                             (ok (jmx/jmx-update jvm)))

                      (POST* "/validate" []
                             :summary "检查提交的修改JVM属性的请求数据是否合法"
                             :body [jvm schema/JVM]
                             (timbre/info jvm)
                             (ok "Looks all data are good.")))

            (context* "/threadpool" []
                      :tags ["ThreadPool"]
                      :summary "配置线程池的服务"

                      (GET* "/list/:cell" []
                            :summary "获取指定范围的threadpool属性值"
                            :path-params [cell :- s/Str]
                            (ok (jmx/jmx-list-recursive {:cell cell} "ThreadPool")))

                      (GET* "/list/:cell/:node" []
                            :summary "获取指定范围的threadpool属性值"
                            :path-params [cell :- s/Str node :- s/Str]
                            (ok (jmx/jmx-list-recursive {:cell cell :node node} "ThreadPool")))

                      (GET* "/list/:cell/:node/:server" []
                            :summary "获取指定范围的threadpool属性值"
                            :path-params [cell :- s/Str node :- s/Str server :- s/Str]
                            (ok (jmx/jmx-list-recursive {:cell cell :node node :server server} "ThreadPool")))

                      (POST* "/update" []
                             :summary "修改指定threadpool属性值"
                             :body [tp schema/ThreadPool]
                             (timbre/info tp)
                             (ok (jmx/jmx-update tp)))

                      (POST* "/validate" []
                             :summary "检查提交的修改ThreadPool属性的请求数据是否合法"
                             :body [tp schema/ThreadPool]
                             (timbre/info tp)
                             (ok "Looks all data are good.")))

            (context* "/jdbcprovider" []
                      :tags ["JDBCProvider"]
                      :summary "配置jdbc提供者的服务"

                      (GET* "/list/:cell" []
                            :summary "获取指定范围的jdbc提供者属性"
                            :path-params [cell :- s/Str]
                            (ok (jmx/jmx-list {:cell cell} "JDBCProvider")))

                      (GET* "/list/:cell/:node" []
                            :summary "获取指定范围的jdbc提供者属性"
                            :path-params [cell :- s/Str node :- s/Str]
                            (ok (jmx/jmx-list {:cell cell :node node} "JDBCProvider")))

                      (GET* "/list/:cell/:node/:server" []
                            :summary "获取指定范围的jdbc提供者属性"
                            :path-params [cell :- s/Str node :- s/Str server :- s/Str]
                            (ok (jmx/jmx-list {:cell cell :node node :server server} "JDBCProvider")))

                      (POST* "/new" []
                             :summary "创建新的jdbc提供者"
                             :body [jp schema/JDBCProvider-NEW]
                             (timbre/info jp)
                             (ok (jmx/jmx-new (:newJDBCProvider jp) (:templateObjectName jp))))

                      (POST* "/update" []
                             :summary "修改指定的jdbc提供者属性"
                             :body [jp schema/JDBCProvider]
                             (timbre/info jp)
                             (ok (jmx/jmx-update jp)))

                      (POST* "/remove" []
                             :summary "删除指定的jdbc提供者"
                             :body [jp schema/JDBCProvider]
                             (timbre/info jp)
                             (ok (jmx/jmx-delete jp)))

                      (POST* "/validate" []
                             :summary "验证提交的数据是否合法"
                             :body [jp schema/JDBCProvider]
                             (timbre/info jp)
                             (ok "Looks all data are good.")))

            (context* "/datasource" []
                      :tags ["DataSource"]
                      :summary "配置数据源"

                      (GET* "/list/:cell" []
                            :summary "获取指定作用域的数据源属性"
                            :path-params [cell :- s/Str]
                            (ok (jmx/jmx-list {:cell cell} "DataSource")))

                      (GET* "/list/:cell/:node" []
                            :summary "获取指定作用域的数据源属性"
                            :path-params [cell :- s/Str node :- s/Str]
                            (ok (jmx/jmx-list {:cell cell :node node} "DataSource")))

                      (GET* "/list/:cell/:node/:server" []
                            :summary "获取指定作用域的数据源属性"
                            :path-params [cell :- s/Str node :- s/Str server :- s/Str]
                            (ok (jmx/jmx-list {:cell cell :node node :server server} "DataSource")))

                      (POST* "/new" []
                             :summary "创建新的数据源"
                             :body [ds schema/DataSource-NEW]
                             (ok (jmx/jmx-new (merge {:parentId (:id (:providerId ds))} (:newDataSource ds)) (str "Websphere:_Websphere_Config_Data_Id=" (:id (:templateId ds)) ",_Websphere_Config_Data_Type=" (:configType (:templateId ds))))))

                      (POST* "/update" []
                             :summary "修改指定的数据源属性"
                             :body [ds schema/DataSource]
                             (timbre/info ds)
                             (ok (jmx/jmx-update ds)))

                      (POST* "/remove" []
                             :summary "删除指定的数据源"
                             :body [ds schema/DataSource]
                             (ok (jmx/jmx-delete ds)))

                      (POST* "/validate" []
                             :summary "验证提交的数据是否合法"
                             :body [ds schema/DataSource]
                             (ok "Looks all data are good.")))

            (context* "/jmsconnectionfactory" []
                      :tags ["JMSConnectionFactory"]
                      :summary "配置JMS连接工厂"

                      (GET* "/list/:cell" []
                            :summary "获取指定作用域的jms连接工厂属性"
                            :path-params [cell :- s/Str]
                            (ok (jmx/jmx-list {:cell cell} "JMSConnectionFactory")))

                      (GET* "/list/:cell/:node" []
                            :summary "获取指定作用域的jms连接工厂属性"
                            :path-params [cell :- s/Str node :- s/Str]
                            (ok (jmx/jmx-list {:cell cell :node node} "JMSConnectionFactory")))

                      (GET* "/list/:cell/:node/:server" []
                            :summary "获取指定作用域的jms连接工厂属性"
                            :path-params [cell :- s/Str node :- s/Str server :- s/Str]
                            (ok (jmx/jmx-list {:cell cell :node node :server server} "JMSConnectionFactory")))

                      (POST* "/new" []
                             :summary "创建新的jms连接工厂"
                             :body [jcf schema/JMSConnectionFactory]
                             (ok (jmx/jmx-new jcf)))

                      (POST* "/remove" []
                             :summary "删除指定的jms连接工厂"
                             :body [jcf schema/JMSConnectionFactory]
                             (ok (jmx/jmx-delete jcf)))

                      (POST* "/update" []
                             :summary "修改指定的jms连接工厂属性"
                             :body [jcf schema/JMSConnectionFactory]
                             (timbre/info jcf)
                             (ok (jmx/jmx-update jcf)))

                      (POST* "/validate" []
                             :summary "验证提交的数据是否合法"
                             :body [jcf schema/JMSConnectionFactory]
                             (ok "Looks all data are good.")))

            (context* "/template" []
                      :tags ["Template"]
                      :summary "模板相关配置"

                      (GET* "/list/:cell/:type" []
                            :summary "获取指定配置类型的模板"
                            :path-params [cell :- s/Str type :- s/Str]
                            (ok (jmx/query-template cell type)))

                      (POST* "/get-datasource-template-by-provider-id" []
                             :summary "根据指定的jdbc provider列出对应的数据源模板"
                             :body [ds-t schema/DataSource-Template]
                             (ok (jmx/get-datasource-template-by-provider ds-t))))

            (context* "/misc" []
                      :tags ["Misc"]
                      :summary "杂项"

                      (GET* "/agentinfo/:cell" []
                            :summary "获取部署在中间件管理节点上的管理代理的信息"
                            :path-params [cell :- s/Str]
                            (ok (jmx/agent-info cell)))
                      (POST* "/attrinfo/list" []
                            :summary "获取指定属性类型的meta info"
                            :body [attrinfo schema/ATTRINFO]
                            (ok (jmx/get-attr-info (:cell attrinfo) (:attrtype attrinfo))))

                      (GET* "/cfgtypes/:cell" []
                            :summary "获取支持的配置类型"
                            :path-params [cell :- s/Str]
                            (ok (jmx/get-cfg-types cell))))))
