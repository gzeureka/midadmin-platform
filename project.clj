(defproject midadmin-platform "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [selmer "0.8.2"]
                 [com.taoensso/timbre "4.0.2"]
                 [com.taoensso/tower "3.0.2"]
                 [markdown-clj "0.9.67"]
                 [environ "1.0.0"]
                 [compojure "1.3.4"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-session-timeout "0.1.0"]
                 [ring "1.4.0-RC2"
                  :exclusions [ring/ring-jetty-adapter]]
                 [ring-server "0.4.0"]
                 [cc.qbits/jet "0.6.5"]
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.3"]
                 [bouncer "0.3.3"]
                 [prone "0.8.2"]
                 [org.clojure/tools.nrepl "0.2.10"]

                 [buddy "0.6.0"]
                 [metosin/compojure-api "0.22.0"]
                 [metosin/ring-swagger-ui "2.1.1-M2"]
                 [clj-http "1.1.2"]]

  :min-lein-version "2.0.0"
  :uberjar-name "midadmin-platform.jar"
  :jvm-opts ["-server"]

;;enable to start the nREPL server when the application launches
;:env {:repl-port 7001}

  :main midadmin-platform.core

  :plugins [[lein-ring "0.9.6"]
            [lein-environ "1.0.0"]
            [lein-ancient "0.6.5"]
            ]
  

  
  :ring {:handler midadmin-platform.handler/app
         :init    midadmin-platform.handler/init
         :destroy midadmin-platform.handler/destroy
         :uberwar-name "midadmin-platform.war"}

  
  
  :profiles
  {:uberjar {:omit-source true
             :env {:production true}
             
             :aot :all}
   :dev {:dependencies [[ring-mock "0.1.5"]
                        [ring/ring-devel "1.3.2"]
                        [pjstadig/humane-test-output "0.7.0"]
                        ]
         
         
         
         :repl-options {:init-ns midadmin-platform.core}
         :injections [(require 'pjstadig.humane-test-output)
                      (pjstadig.humane-test-output/activate!)]
         :env {:dev true}}})
