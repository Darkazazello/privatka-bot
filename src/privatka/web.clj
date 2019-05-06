(ns privatka.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :as cookie]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic]
            [cemerick.drawbridge :as drawbridge]
            [environ.core :refer [env]]
            [clojure.data.json :as json]
            [clojure.stacktrace]
            [privatka.bot :refer [process-message]]
            [morse.api :as t]))
(def token (env :token))

(defn- authenticated? [user pass]
  ;; TODO: heroku config:add REPL_USER=[...] REPL_PASSWORD=[...]
  (= [user pass] [(env :repl-user false) (env :repl-password false)]))

(def ^:private drawbridge
  (-> (drawbridge/ring-handler)
      (session/wrap-session)
      (basic/wrap-basic-authentication authenticated?)))

(defn core [message]
  (do
    (println "Intercepted message:" message)
    (try
      (process-message "-325190028"  message)
      (catch Exception e (println (.getMessage e) (clojure.stacktrace/print-stack-trace e)))
      )
    {:status 200}
    )
  )
(defroutes app
           (POST "/handler" {body :body}
                 (let [a (-> body slurp json/read-str)
                       command (get (get a "message") "text")]
                   (do (println a) (core command)) ))
           (ANY "/repl" {:as req}
                (drawbridge req))
           (GET "/" []
                {:status  200
                 :headers {"Content-Type" "text/plain"}
                 :body    (pr-str ["Hello" :from 'Heroku (env :token)])})
           (ANY "*" []
                (route/not-found (slurp (io/resource "404.html")))))

(defn wrap-error-page [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           {:status  500
            :headers {"Content-Type" "text/html"}
            :body    (slurp (io/resource "500.html"))}))))

(defn wrap-app [app]
  ;; TODO: heroku config:add SESSION_SECRET=$RANDOM_16_CHARS
  (let [store (cookie/cookie-store {:key (env :session-secret)})]
    (-> app
        ((if (env :production)
           wrap-error-page
           trace/wrap-stacktrace))
        (site {:session {:store store}}))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (t/set-webhook token "https://privatka-bot.herokuapp.com/handler")
    ;(def channel (p/start token "https://privatka-bot.herokuapp.com/handler"))
    (jetty/run-jetty (wrap-app #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
