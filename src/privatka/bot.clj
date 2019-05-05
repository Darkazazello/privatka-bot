(ns privatka.bot

  (:require [clojure.string :as str]
            [clojure.core.match :refer [match]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [environ.core :refer [env]]))

(def token (env :token))
(def tasks (-> "tasks.json" io/resource slurp json/read-str))
(def cbu-chat "-325190028")
(def counter-chat "-353786595")
(def cbu-commands {:find-message "Найдена шифровка" :current-square "Находимся в квадрате" :current-position "Точное местоположение"})
(def cbu-messages {:get-current-position "Сообщите точное местоположение" :get-current-square "Сообщите квадрат местоположения"})
(def counter-commands {:blpa "Запуск БЛПА"})

(defn- find-message [code]
  (let [t (get tasks "scenario")
        task (filter #(= (get % "code") code) t)]
    (if (nil? task)
      (do (print "Wrong" code) nil)
      (let [points (get (first task) "points")
            size (count points)
            r (rand-int size)]
        (nth points r)))))


(defn- retrive-data [prefix message]
  (str/trim (str/replace message prefix "")))

(defn- send-photo [chat-id file]
  (client/post (str "https://api.telegram.org/bot" token "/sendPhoto")
               {:multipart    [{:name "title" :content "Point"}
                               {:name "chat_id" :content chat-id}
                               {:name "photo" :content file}]
                }))

(defn- send-text [chat-id text]
  (client/post (str "https://api.telegram.org/bot" token "/sendMessage")
               {:multipart    [
                               {:name "chat_id" :content chat-id}
                               {:name "text" :content text}]
                }))

(defn- send-new-task [message]
  (let [point (find-message message)]
    (if-not (nil? point)
      (do (send-text cbu-chat message)
          (send-photo cbu-chat (first (get point "files")))))))

(defn- send-square-to-vs [message]
  (send-text counter-chat (str "Группа РДГ замечена в квадрате " message)))

(defn- send-point-to-vs [message]
  (send-text counter-chat (str "БЛПА выявил группу РДГ в " message)))

(defn process-message [chat-id message]
  (if (= cbu-chat chat-id)
    (cond (str/starts-with? message "/help") "Формат сообщений в ЦБУ: \n Найдена шифровка {шифрованый код} \n
                                                 Находимся в квадрате {квадрат на карте} \n
                                                 Точное местоположение {квадрат+улитка}"
          (str/starts-with? message (:find-message cbu-commands))
                            (-> message
                                (retrive-data (:find-message cbu-commands))
                                send-new-task)
          (str/starts-with? message (:current-square cbu-commands))
                            (-> message
                                (retrive-data (:current-square cbu-commands))
                                send-square-to-vs)
          (str/starts-with? message (:current-position cbu-commands))
                            (-> message
                                (retrive-data (:current-position cbu-commands))
                                send-point-to-vs)
          :else "zero")
    (cond (str/starts-with? message "/help") "Формат сообщений в Штаб: \n Запуск БЛПА"
          (str/starts-with? message (:blpa counter-commands)) (send-text cbu-chat "Срочно сообщите точное местоположение во избежания дружественного удара")
          :else "zero")))
