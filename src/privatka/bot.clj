(ns privatka.bot

  (:require [clojure.string :as str] ))

  (def token (env :token))
  (def tasks (-> "tasks.json" io/resource slurp json/read-str))
  (def cbu-chat "-325190028")
  (def counter-chat "-353786595")
  (def cbu-commands {:find-message "Найдена шифровка:" :current-square "Находимся в квадрате"  :current-position "Точка стояния:"})
  (def cbu-messages {:get-current-position "Сообщите точное местоположение" :get-current-square "Сообщите квадрат местоположения"})
  (def counter-commands {:blpa "Запуск БЛПА"})
  (defn process-message [chat-id message]
    (cond (and (str/starts-with? message "/help") (= cbu-chat chat-id))  "Формат сообщений в ЦБУ: \n Найдена шифровка:{шифрованый код} n
                                             Находимся в квадрате:{квадрат на карте} \n
                                             Точка стояния:{квадрат+улитка}"
          (and (str/starts-with? message "/help") (= counter-chat chat-id)) "Формат сообщений в Штаб: Запуск БЛПА"
          :else "zero")

)
