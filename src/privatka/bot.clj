(ns privatka.bot

  (:require [clojure.string :as str]
            [clojure.core.match :refer [match]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [environ.core :refer [env]]
            [clojure.core.async :as async :refer [<! timeout chan go]]))

(def token (env :token))
(def tasks (-> "tasks.json" io/resource slurp json/read-str))
;(def points (-> "points.json" io/resource slurp json/read-str))
(def cbu-chat "-1001360449621")
(def counter-chat "-1001409162263")
(def cbu-commands {:find-message "ш" :current-square "кк" :current-position "кку" :evacuation-point "пе"})
(def cbu-messages {:get-current-position "сообщите точное местоположение" :get-current-square "сообщите квадрат местоположения"})
(def counter-commands {:blpa "бпла" :start-game "красный одуванчик" :end-game "стоп"})

(def help-message "Формат сообщений ДРГ в ЦБУ:
Передача кода точки: Ш <код> К <текущие географические координаты группы>. \\n Пример Ш 1234 К N 53° 52.617', E 027° 47.750'
Передача квадрата группы: КК <квадрат>. \\nПример КК 11-55 \\n
Передача уточненных координат: ККУ <квадрат и улитка. \\n Пример ККУ 11-55 у 3
Запрос эвакуации: ПЕ <код пункта эвакуации>. Пример ПЕ 093")

(def contr-cbu-message "Формат сообщений в Штаб:
Запрос вылета БПЛА: БПЛА")
;Активация сети информаторов Красный одуванчик
;Отбой для сети информаторов Стоп")

(def meet-message "По агентурным данным состоялась встреча ДРГ и агента, координаты - ")

(def fire-warning "Срочно сообщите точное местоположение во избежания дружественного удара")

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
          (doseq [photo (get point "files")]
            (send-photo cbu-chat photo)
            )
          ))))


(defn- send-square-to-vs [m]
  (do (println m)
      (send-text counter-chat (str "Группа ДРГ замечена в квадрате " m))))


(defn- send-point-to-vs [m]
  (send-text counter-chat (str "БПЛА выявил группу ДРГ в " m)))

(def milisecs-to-wait 360)
(def is-game-started (atom true))

(defn ping-to-drg []
  (go
    (while (@is-game-started)
      (<! (timeout milisecs-to-wait))
      (send-text cbu-chat (:get-current-square cbu-messages)))))

(defn- is-encoded-message[m]
  (if (re-find #"^ш.*к.*$" m)
    :true
    :false))

(defn- send-ep [point-code]
(let [t (get tasks "points")
      coordinate (get (first (filter #(= (get % "code") point-code) t)) "coordinate") ]
  (send-text counter-chat (str/join ["ДРГ начала процедуру эвакуации в точке " coordinate]) )))

(defn process-message [chat-id message]
  (let [m (str/lower-case message)]
    (if (= cbu-chat chat-id)

      (cond 
        (str/includes? m "help") (send-text chat-id help-message)
        (= (is-encoded-message m) :true) (let [p1 (first (str/split m #"к"))
                                     p2 (last (str/split m #"к"))]
                                 (do
                                   (send-new-task (retrive-data (:find-message cbu-commands) p1))
                                  (send-text counter-chat (str/join [meet-message p2]))
                                  ))
        (str/includes? m (:current-square cbu-commands)) (send-square-to-vs (retrive-data (:current-square cbu-commands) m))
        (str/includes? m (:current-position cbu-commands)) (send-point-to-vs (retrive-data (:current-position cbu-commands) m))
        (str/includes? m (:evacuation-point cbu-commands)) (send-ep (retrive-data (:evacuation-point cbu-commands) m))
        :else (send-text chat-id "Сообщение не принято, повторите передачу."))
      (cond (str/includes? m "help") (send-text chat-id contr-cbu-message)
            (str/includes? m (:blpa counter-commands)) (send-text cbu-chat fire-warning)
            (str/includes? m (:start-game counter-commands)) (do (reset! is-game-started true) ping-to-drg)
            (str/includes? m (:end-game counter-commands)) (do (reset! is-game-started false))
            :else (send-text chat-id "Сообщение не принято, повторите передачу."))))
  )
