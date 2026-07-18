# kotoba-captcha

Self-hosted CAPTCHA task orchestration for **synthetic tests, first-party
properties, accessibility, and human-assisted verification**. It provides the
portable task contract beneath HTTP and worker adapters; it is not an anti-bot
evasion product.

## CapSolver-compatible API core

- `kotoba.captcha.api/create-task!` accepts `clientKey`, `task`, and an explicit
  `authorization` object, and returns `{errorId, taskId}`.
- `kotoba.captcha.api/get-task-result` returns CapSolver-compatible
  `processing`, `ready`, and error responses.
- `kotoba.captcha.api/transition-task!` is the worker-facing state transition.

Supported safe task types are `SyntheticChallengeTask`,
`HumanVerificationTask`, and `ImageToTextTask`. Common anti-bot service task
types such as `ReCaptchaV2TaskProxyLess` are intentionally rejected. First-party
tasks require both `owner-confirmed?` and an auditable `reason`.

```clojure
(def tasks (kotoba.captcha.store/memory-store))
(kotoba.captcha.api/create-task!
 tasks
 {:task {:type "SyntheticChallengeTask"
         :prompt "2 + 2"
         :authorization {:scope :synthetic :reason "CI fixture"}}})
```

The portable `.cljc` core retains neither `clientKey` nor worker credentials.
Production adapters must encrypt sensitive payloads, authenticate callers,
apply quotas, and expire task data.

## Verify

```sh
clojure -M:test
```

HTTP routing, durable persistence, and execution workers are separate adapters
so the pure contract remains usable from JVM, ClojureScript, and WASM hosts.
