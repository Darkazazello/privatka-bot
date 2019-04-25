(ns privatka.bot
  (ns user
      (:require [morse.handlers :as h]
                [morse.api :as t]
                [environ.core :refer [env]]))

  (def token (env :token))

  ; This will define bot-api function, which later could be
  ; used to start your bot
  (h/defhandler bot-api
    ; Each bot has to handle /start and /help commands.
    ; This could be done in form of a function:
                (h/command-fn "start" (fn [{{id :id :as chat} :chat}]
                                        (println "Bot joined new chat: " chat)
                                        (t/send-text token id "Welcome!")))

    ; You can use short syntax for same purposes
    ; Destructuring works same way as in function above
                (h/command "help" {{id :id :as chat} :chat}
                           (println "Help was requested in " chat)
                           (t/send-text token id "Help is on the way"))

    ; Handlers will be applied until there are any of those
    ; returns non-nil result processing update.

    ; Note that sending stuff to the user returns non-nil
    ; response from Telegram API.

    ; So match-all catch-through case would look something like this:
                (h/message message (println "Intercepted message:" message)))
  (t/set-webhook token "https://privatka-bot.herokuapp.com/")
  )
