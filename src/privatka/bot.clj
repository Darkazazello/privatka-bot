(ns privatka.bot
  (ns user
      (:require [morse.handlers :as h]
                [morse.api :as t]
                [environ.core :refer [env]]
                [compojure.route :as route]))

  (def token (env :token))

  ; This will define bot-api function, which later could be
  ; used to start your bot

  (defroutes app-routes
             )
             (route/not-found "Not Found"))
  )
