(ns privatka.bot

  (:require [clojure.string :as str]
            [clojure.core.match :refer [match]]))

(def token (env :token))
(def tasks (-> "tasks.json" io/resource slurp json/read-str))
(def cbu-chat "-325190028")
(def counter-chat "-353786595")
(def cbu-commands {:find-message "Найдена шифровка:" :current-square "Находимся в квадрате" :current-position "Точка стояния:"})
(def cbu-messages {:get-current-position "Сообщите точное местоположение" :get-current-square "Сообщите квадрат местоположения"})
(def counter-commands {:blpa "Запуск БЛПА"})

(defn process-find-message [code]
  (let [t (get tasks "scenario")
        task (filter #(= (get % "code") code) t)]
    (if (nil? task)
      (do (print "Wrong" code) nil)
      (let [points (get (first task) "points")
            size (count points)
            r (rand-int size)]
        (nth points r)))))


(defn process-message [chat-id message]
  (if (= cbu-chat chat-id)
    (cond (str/starts-with? message "/help") "Формат сообщений в ЦБУ: \n Найдена шифровка:{шифрованый код} \n
                                                 Находимся в квадрате:{квадрат на карте} \n "
          (str/starts-with? message (:find-message cbu-commands) (-> message (str/replace )))
          :else "zero")
    (cond (str/starts-with? message "/help") "Формат сообщений в Штаб: Запуск БЛПА"
          :else "zero")
    )

  )
