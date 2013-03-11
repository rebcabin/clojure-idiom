(ns chapter-protocols.worker-server
  (:use chapter-protocols.rabbitmq chapter-protocols.worker))

(defn response-for [worker-handler worker-args]
  (try
    (let [value (apply worker-handler worker-args)]
      {:value value :status :success})
   (catch Exception e 
     {:status :error})))

(defn process-request [worker-handler worker-args return-q]
  (future 
    (with-rabbit ["localhost" "guest" "guest"]
      (let [response-envelope (response-for worker-handler worker-args)]
        (when return-q
          (send-message return-q response-envelope))))))

(defn handle-request-message [req-str]
  (try
   (let [req (read-string req-str)
         worker-name (req :worker-name) worker-args (req :worker-args) return-q (req :return-q)
         worker-handler (@workers worker-name)]
     (if (not (nil? worker-handler))
       (do
         (println "Processing:" worker-name "with args:" worker-args "for return-q:" return-q)
         (process-request worker-handler worker-args return-q))))
   (catch Exception e
     (println "Found exception:" (class e))
     (.printStackTrace e))))

(defn start-handler-process []
  (println "Serving up" (count @workers) "workers.")
  (doseq [request-message (message-seq WORKER-QUEUE)]
    (handle-request-message request-message)))

(defn start-broadcast-listener []
  (println "Listening to broadcasts.")
  (doseq [request-message (message-seq BROADCAST-EXCHANGE FANOUT-EXCHANGE-TYPE BROADCAST-QUEUE)]
    (handle-request-message request-message)))