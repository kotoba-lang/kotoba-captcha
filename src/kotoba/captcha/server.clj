(ns kotoba.captcha.server
  "Optional local host. `run-server` is injected (for example http-kit), keeping
  the library dependency-light and the lifecycle explicit."
  (:require [clojure.string :as str]
            [kotoba.captcha.http :as http]
            [kotoba.captcha.store :as store]))

(defn start!
  [run-server config {:keys [port host] :or {port 8787 host "127.0.0.1"}}]
  (run-server (http/make-handler config) {:port port :ip host}))

(defn stop! [close-server]
  (when close-server (close-server)))

(defn -main [& _]
  (let [keys (some-> (System/getenv "KOTOBA_CAPTCHA_API_KEYS")
                     (str/split #",") seq)
        port (some-> (System/getenv "PORT") parse-long)
        run-server (requiring-resolve 'org.httpkit.server/run-server)]
    (when-not keys
      (throw (ex-info "KOTOBA_CAPTCHA_API_KEYS is required" {:type :missing-api-keys})))
    (start! run-server {:task-store (store/memory-store) :api-keys keys}
            {:port (or port 8787) :host "127.0.0.1"})
    (println "kotoba-captcha listening on 127.0.0.1:" (or port 8787))))
