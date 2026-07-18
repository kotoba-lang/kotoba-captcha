(ns kotoba.captcha.server
  "Optional local host. `run-server` is injected (for example http-kit), keeping
  the library dependency-light and the lifecycle explicit."
  (:require [kotoba.captcha.http :as http]))

(defn start!
  [run-server config {:keys [port host] :or {port 8787 host "127.0.0.1"}}]
  (run-server (http/make-handler config) {:port port :ip host}))

(defn stop! [close-server]
  (when close-server (close-server)))
