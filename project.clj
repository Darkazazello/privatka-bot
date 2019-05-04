(defproject helloworld "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://helloworld.herokuapp.com"
  :license {:name "FIXME: choose"
            :url "http://example.com/FIXME"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [ring/ring-devel "1.2.2"]
                 [ring/ring-json "0.4.0"]
                 [ring-basic-authentication "1.0.5"]
                 [environ "0.5.0"]
                 [com.cemerick/drawbridge "0.0.6"]
                 [morse "0.4.2"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.9.1"]
                 [org.clojure/core.match "0.3.0"]]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.2.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "privatka-bot.jar"
  :main privatka.web
  :profiles {:production {:env {:production true}}})
