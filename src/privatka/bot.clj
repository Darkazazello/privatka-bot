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
(def cbu-commands {:find-message "Ш" :current-square "КК" :current-position "КУ"})
(def cbu-messages {:get-current-position "сообщите точное местоположение" :get-current-square "сообщите квадрат местоположения"})
(def counter-commands {:blpa "БЛПА"})

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
               {:multipart [{:name "title" :content "Point"}
                            {:name "chat_id" :content chat-id}
                            {:name "photo" :content file}]
                }))

(defn- send-text [chat-id text]
  (client/post (str "https://api.telegram.org/bot" token "/sendMessage")
               {:multipart [
                            {:name "chat_id" :content chat-id}
                            {:name "text" :content text}]
                }))

(defn- send-new-task [message]
  (let [point (find-message message)]
    (if-not (nil? point)
      (do (send-text cbu-chat (get point "text"))
          (send-photo cbu-chat (first (get point "files")))))))

(defn- send-square-to-vs [m]
  (do (println m)
      (send-text counter-chat (str "Группа РДГ замечена в квадрате " m))))


(defn- send-point-to-vs [m]
  (send-text counter-chat (str "БЛПА выявил группу РДГ в " m)))

(defn process-message [chat-id message]
  (let [m (str/lower-case message)]
    (if (= cbu-chat chat-id)

      (cond (str/includes? m "help") (send-text chat-id "Формат сообщений в ЦБУ: \n Для сообщения найдена шифровка: Ш <шифровка>. Пример Ш 1234  
                                                 Для сообщения квадрата: КК <квадрат>. Пример КК квадрат 11-55
                                                 Для сообщения точного местоположения: КУ <квадрат и улитка. Пример КУ 11-55 по улитке 3")
            (str/starts-with? m (:find-message cbu-commands))
            (send-new-task
              (retrive-data (:find-message cbu-commands) m))
            (str/starts-with? m (:current-square cbu-commands))
            (send-square-to-vs
              (retrive-data (:current-square cbu-commands) m))
            (str/starts-with? m (:current-position cbu-commands))
            (send-point-to-vs
              (retrive-data (:current-position cbu-commands) m))
            :else "zero")
      (cond (str/includes? m "help") (send-text chat-id "Формат сообщений в Штаб: \n Запуск БЛПА БЛПА")
            (str/starts-with? m (:blpa counter-commands)) (send-text cbu-chat "Срочно сообщите точное местоположение во избежания дружественного удара")
            :else "zero")))
  )

