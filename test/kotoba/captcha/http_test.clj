(ns kotoba.captcha.http-test
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [kotoba.captcha.http :as http]
            [kotoba.captcha.store :as store]))

(defn request
  ([uri body] (request uri body {}))
  ([uri body headers]
   {:request-method :post :uri uri :headers headers
    :body (java.io.ByteArrayInputStream.
           (.getBytes (json/generate-string body) "UTF-8"))}))

(defn parsed [response] (json/parse-string (:body response) true))

(def authorized-task
  {:type "SyntheticChallengeTask"
   :authorization {:scope "synthetic" :reason "contract test fixture"}
   :prompt "2 + 2"})

(deftest capsolver-compatible-contract
  (let [handler (http/make-handler {:task-store (store/memory-store)
                                    :api-keys ["test-secret"]})
        created (handler (request "/createTask"
                                  {:clientKey "test-secret" :task authorized-task}))
        task-id (:taskId (parsed created))
        result (handler (request "/getTaskResult"
                                 {:clientKey "test-secret" :taskId task-id}))]
    (is (= 200 (:status created)))
    (is (string? task-id))
    (is (= {:errorId 0 :status "processing"} (parsed result)))))

(deftest authentication-is-fail-closed-and-supports-bearer
  (let [seen (atom nil)
        handler (http/make-handler
                 {:api-keys ["correct"]
                  :service {:create-task! #(do (reset! seen %) {:errorId 0 :taskId "x"})}})]
    (is (= 401 (:status (handler (request "/createTask"
                                          {:clientKey "wrong" :task authorized-task})))))
    (is (= 200 (:status (handler (request "/createTask" {:task authorized-task}
                                          {"authorization" "Bearer correct"})))))
    (is (nil? (:clientKey @seen)))
    (is (= 503 (:status ((http/make-handler {})
                         (request "/createTask" {:clientKey "anything"})))))))

(deftest limits-errors-and-operational-endpoints
  (let [limiter (http/fixed-window-limiter {:limit 1 :clock (constantly 1000)})
        metrics (atom {})
        handler (http/make-handler {:api-keys ["key"] :max-body-bytes 64
                                    :rate-limiter limiter :metrics metrics
                                    :service {:get-task-result
                                              (constantly {:errorId 0 :status "processing"})}})]
    (is (= 200 (:status (handler (request "/getTaskResult"
                                         {:clientKey "key" :taskId "1"})))))
    (is (= 429 (:status (handler (request "/getTaskResult"
                                         {:clientKey "key" :taskId "1"})))))
    (is (= 413 (:status (handler (assoc (request "/createTask" {})
                                        :headers {"content-length" "100"})))))
    (is (= 200 (:status (handler {:request-method :get :uri "/health"}))))
    (is (str/includes? (:body (handler {:request-method :get :uri "/metrics"}))
                       "kotoba_captcha_http_requests_total"))))

(deftest redaction-is-recursive
  (is (= {:clientKey "[REDACTED]"
          :nested [{:authorization "[REDACTED]" :safe "yes"}]}
         (http/redact {:clientKey "secret"
                       :nested [{:authorization "Bearer secret" :safe "yes"}]}))))
