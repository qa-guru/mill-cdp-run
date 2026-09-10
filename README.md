# mill-cdp-run

Jenkins Pipeline step **`millCdpRun`**: `POST` a URL **on the allocated agent** via remoting (`HttpURLConnection`). No `sh`, no `curl`.

Built for [greedy.guru](https://greedy.guru) mill: `hot-cdp-daemon` on loopback (`127.0.0.1:17892/run`). Any HTTP endpoint that returns JSON with `"ok":true` works.

**Not** on the Jenkins Update Center. Install the `.hpi`. Pin mill time with JSON `elapsed_ms`, not the Stage View cell.

```groovy
millCdpRun url: 'http://127.0.0.1:17892/run?select=login'
millCdpRun url: 'http://127.0.0.1:17892/run?select=logout'
millCdpRun url: 'http://127.0.0.1:17892/run?select=register'
millCdpRun url: 'http://127.0.0.1:17892/run?select=home'
```

The step fails unless the body contains `"ok":true`.

## Why

On a warm mill daemon (2026-08-29, sequential):

| Step | n | Test median | mill median | tax (Test−mill) |
|--|--|--|--|--|
| `millCdpRun` | 12 | **355 ms** | 218 ms | **~125 ms** |
| `exec curl` | 4 | 600 ms | 220 ms | ~380 ms |
| `sh curl` | 4 | 617 ms | 249 ms | ~355 ms |

Rollback in a Pipeline is still `exec curl`.

## Build

Jenkins **2.479.3+**, Java **21**.

```bash
mvn -B verify
```

Artifact: `target/mill-cdp-run.hpi`.

## Install

Upload the HPI in **Manage Jenkins → Plugins**, or `dynamicLoad` the `.jpi` on the controller. Requires [Pipeline: Step API](https://plugins.jenkins.io/workflow-step-api).

Deploy to [jenkins.qa.guru](https://jenkins.qa.guru) stays in the workspace hub (`apply-mill-cdp-run-plugin.sh`), not this repo.

## License

[MIT](LICENSE)
