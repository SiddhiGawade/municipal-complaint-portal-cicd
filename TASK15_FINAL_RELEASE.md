# Task 15 - Final Release, Documentation, and Viva

**Project:** Municipal Complaint Portal  
**Snapshot:** 2026-10-20  
**Release decision:** Local application and quality gates pass. The Docker registry release and remote Ansible rollout remain blocked; this snapshot is not a production release.

## 1. Release status

This document records what was verified in this workspace and separates it
from steps that need a registry credential, network access, or a real Linux
target. A green local test run is not treated as proof of a published image or
provisioned server.

| Gate | Result | Evidence and boundary |
| --- | --- | --- |
| Maven unit tests | PASS | `mvn -o test`: 17 tests, 0 failures, 0 errors, 0 skipped. |
| Selenium browser tests | PASS | Failsafe: 5 journeys, 0 failures, 0 errors, 0 skipped. ChromeDriver was selected from the local Selenium cache. |
| Ansible source validation | PASS, limited | PyYAML parsed the four Ansible YAML files. Ansible Core syntax validation was not run because `ansible-playbook` is unavailable. |
| Local application smoke test | PASS | Temporary loopback process returned HTTP 200 and `status: UP`; the process was stopped after capture. |
| Existing local backend and Nginx listeners | PASS, read-only | GET probes to `127.0.0.1:8085/api/health` and `127.0.0.1:8090/api/health` both returned HTTP 200. Deployment provenance was not verified. |
| Portal screenshot | CAPTURED | `TASKS_REPORTS/Task15_local_live_portal.png`; local JAR on a temporary loopback port, not the Docker or Nginx deployment. |
| Jenkins history | HISTORICAL PASS | Pipeline build #6 checked out `9aa2e01`, passed 17 unit and 5 Selenium tests, and performed the earlier native JAR/Nginx deployment. It predates the Docker-pipeline commit. |
| Docker image build/publish | NOT RUN | Neither Dockerfile base image is cached. Building requires external image pulls; no registry push was attempted. |
| Docker deployment from the current commit | BLOCKED | `Dockerfile` and `.dockerignore` are untracked in the current worktree and are absent from `HEAD`; an SCM checkout of the commit cannot build the image yet. |
| Ansible target provisioning | NOT RUN | Ansible is not installed and `ansible/inventory.ini` still points to reserved documentation address `192.0.2.10`. No target, SSH identity, or sudo authorization was supplied. |
| Target idempotency, health, rollback | NOT RUN | The playbook implements these checks, but target-side execution evidence is absent. |
| Screen recording | NOT CAPTURED | A live-demo script is provided. Record a video during the authorized Jenkins/target run; no video is claimed in this repository. |

The current branch is `dev`, with local `HEAD` at `abc9007` and three commits
ahead of `origin/dev` at the time of inspection. The Jenkins history checked
out `9aa2e01`; it does not validate the newer Docker stages. Existing untracked
work was left intact.

## 2. System architecture

The portal is a small Java 17 application:

1. Citizens and reviewers use the static HTML/CSS/JavaScript interface.
2. The JDK `HttpServer` in `App.java` exposes health, complaint submission,
   tracking, and status-update endpoints.
3. `ComplaintService` validates submissions, generates tracking tokens, and
   manages status; `ValidationService` checks and sanitizes selected fields.
4. A `ConcurrentHashMap` stores complaints in process memory. It is not a
   durable database.
5. The multi-stage Dockerfile builds the JAR with Maven and runs it as the
   unprivileged UID/GID 10001 in a Java 17 runtime.
6. Nginx is the ingress proxy. The default deployment binds the backend to
   loopback port 8085 and exposes Nginx on port 8090.
7. Jenkins builds/tests and contains the Docker publish/deploy stages.
   Ansible provisions a separate Debian/Ubuntu Linux host and deploys a
   versioned image. The current Jenkinsfile does not invoke Ansible.

See `TASKS_REPORTS/Task15_architecture.png` for the implementation-aligned
architecture and `TASKS_REPORTS/Task15_release_workflow.png` for the release
gates and evidence boundary.

## 3. CI/CD and release flow

The current Jenkinsfile defines the following order:

1. Check out SCM and read the commit.
2. Validate Docker repository and port parameters; derive an immutable image
   tag from the Maven version, Jenkins build number, and 12-character commit.
3. Compile and package the application; run the JUnit suite and archive the
   JAR.
4. Run the Selenium/Failsafe suite. A failed quality gate stops later stages.
5. Build and label the Docker image only after tests pass.
6. Log in using Jenkins credential ID `dockerhub-credentials`; push the
   immutable tag and the convenience `latest` tag.
7. Pull the immutable tag, replace the environment-specific container, check
   backend and Nginx health, and archive
   `target/docker-deployment-evidence.json`.
8. Publish Surefire and Failsafe JUnit results in the pipeline post action.

The tag format is:

```text
<namespace>/<repository>:<pom-version>-<jenkins-build>-<12-character-commit>
```

The immutable tag, not `latest`, is used for deployment. The Windows Jenkins
agent needs Git, Java 17, Maven, Docker, PowerShell, Chrome/ChromeDriver, and
the configured Nginx installation. The Docker Hub token belongs in Jenkins
credentials, never in Git or a build parameter.

Task 14's Ansible run is a separate operator-controlled deployment path. It
installs Docker/Nginx prerequisites, deploys a tagged image, checks both
service entry points, and restores the prior image ID on candidate failure.
Combining it with the Jenkins job requires an authorized Linux execution
agent, managed inventory/SSH credentials, and an explicit environment policy;
those are not configured in this snapshot.

## 4. Reproduce local validation

Run from the repository root:

```powershell
mvn -o test
mvn -o failsafe:integration-test failsafe:verify -Pselenium
```

On the captured Windows host, Selenium used the locally cached driver without
network access:

```powershell
mvn -o failsafe:integration-test failsafe:verify -Pselenium `
  "-Dwebdriver.chrome.driver=C:/Users/Student/.cache/selenium/chromedriver/win64/155.0.8059.84/chromedriver.exe"
```

The current browser is Chrome 155 and Selenium 4.19.1 emits a CDP-version
warning because no matching CDP module is present. The 5 UI tests still pass.
Pin compatible browser/driver versions or upgrade Selenium before making this
warning part of a strict production gate.

To build and run the application locally:

```powershell
mvn -o package
java -jar target/municipal-complaint-portal-1.0.0-SNAPSHOT.jar
```

Check `http://localhost:8085/api/health`. The captured screenshot is a local
browser view, not a Docker deployment. Existing live listeners on ports 8080,
8085, and 8090 were not stopped or changed during this work.

The existing local portal is available at `http://localhost:8090/` in this
workspace. Read-only health probes to both the backend and Nginx ingress
returned HTTP 200. The listener's deployment provenance was not established,
so do not treat it as evidence for the current Docker pipeline.

## 5. Complete the gated release

These are the remaining authorized operator actions. Review every target and
parameter before running commands; do not use a production host for a rollback
rehearsal.

### 5.1 Commit the Docker build context

First review the existing untracked files. The pipeline's Docker build requires
the Dockerfile and ignore file to be present in the Git commit:

```powershell
git status --short
git diff -- .dockerignore Dockerfile
git add .dockerignore Dockerfile
git add TASK15_FINAL_RELEASE.md build_task15_assets.py build_task15_report.py
git add TASKS_REPORTS/Task15*
git add README.md TASKS_REPORTS/Task15_Viva_QA.md
git commit -m "docs(release): complete Task 15 release and viva package"
```

Stage only the intended files. Do not include unrelated untracked files such
as `build_task14_report.py` unless its owner explicitly requests it. Before
triggering Jenkins, confirm the release commit is on the SCM branch configured
for the pipeline. Pushing to a remote requires the repository owner's normal
approval and credentials.

### 5.2 Run the Docker pipeline

In Jenkins, set `DEPLOY_ENV=staging`, confirm the application and Nginx ports
are unused, set `DOCKERHUB_REPOSITORY` to an authorized lowercase
`namespace/repository`, and make sure credential
`dockerhub-credentials` contains a Docker Hub access token. Start the
`Municipal-Complaint-Portal-Pipeline` job and retain:

- The checked-out commit and successful stage view.
- JUnit/Failsafe reports showing both test suites green.
- The image tag and Docker registry digest.
- `docker-deployment-evidence.json` and the proxied health response.
- A screenshot of the successful Jenkins run and registry tag page.

Do not paste credentials or tokens into the build log, report, or screenshots.

### 5.3 Provision and verify the Linux target

Use a reachable, authorized Debian/Ubuntu target and an Ansible Core 2.16+
controller. Install `community.docker` from `ansible/requirements.yml`; replace
the reserved inventory address and use an approved SSH agent/key and sudo
boundary. Then run the checks in `ansible/TASK14_RUNBOOK.md`:

```sh
ansible-galaxy collection install -r ansible/requirements.yml
ansible -i ansible/inventory.ini municipal_portal -m ping
ansible-playbook -i ansible/inventory.ini ansible/task14.yml --syntax-check
ansible-playbook -i ansible/inventory.ini ansible/task14.yml --diff \
  -e portal_image=<registry>/<repository>:<immutable-tag>
```

Rerun the same deployment command and retain the second `PLAY RECAP`; expect
`changed=0` in a drift-free environment. Verify the direct backend and Nginx
health responses. Rehearse `portal_test_rollback=true` on a non-production
target with a stable image already deployed. A non-zero result is expected
after the playbook restores the previous image and confirms recovery.

## 6. Troubleshooting

| Symptom | Likely cause | Response |
| --- | --- | --- |
| Jenkins fails at `Prepare Image Version` | Empty/invalid `DOCKERHUB_REPOSITORY` or invalid port | Use a lowercase `namespace/repository`; use distinct valid TCP ports. |
| Docker build cannot find `Dockerfile` | Dockerfile is not in the checked-out commit | Track/commit `Dockerfile` and `.dockerignore`, then confirm the build's checkout hash. |
| Docker build cannot pull a base image | Registry/network unavailable or rate-limited | Verify authorized registry/network access and retry; do not claim a local image build from a failed pull. |
| Docker push returns unauthorized | Missing/expired Jenkins credential or wrong repository namespace | Check `dockerhub-credentials` and repository permissions; never print the token. |
| Docker deploy reports unhealthy container | Port conflict, bad image, slow startup, or runtime failure | Inspect `docker ps`, `docker logs <container>`, image tag and backend health before retrying. |
| Nginx returns 502 | Backend not healthy or proxy target/port mismatch | Check the 8085 loopback health endpoint, Nginx config/test, and service logs. |
| Selenium cannot create a session | Browser/driver mismatch or missing browser | Pin a compatible driver/browser or set the approved cached driver path. CDP warning alone did not fail this run. |
| `ansible-playbook` is not found | Controller is Windows without Ansible/WSL setup | Use Linux, macOS, or WSL with Ansible Core; do not use Docker Desktop's internal distribution as a general-purpose controller. |
| Ansible cannot connect to `portal01` | Placeholder inventory, SSH, or sudo failure | Replace `192.0.2.10`, verify SSH with `ansible -m ping`, and configure the authorized become method. |
| Ansible reports a changed second run | Host drift, changing image tag, package cache, or configuration drift | Compare `--diff`; use the same immutable tag and investigate the changed task before accepting idempotency. |
| Rollback rehearsal fails | No prior stable container, candidate tag equals current tag, or previous image unavailable | Deploy a known-good stable release first; use a different candidate tag and retain the previous image locally. |

## 7. Limitations and risks

- **Data durability:** Complaint records are process-local. Container restart or
  replacement loses all records; no database or backup is configured.
- **Security:** Reviewer status changes are not protected by a production
  identity/authorization layer. The MVP lacks TLS termination, rate limiting,
  audit retention, and a reviewed privacy policy for contact details.
- **Input and attachment scope:** Validation and sanitization are lightweight.
  File upload persistence, malware scanning, and durable evidence storage are
  not implemented as a production workflow.
- **Availability:** The pipeline replaces one container rather than doing
  rolling or blue/green deployment. There is no autoscaling or multi-node
  failover.
- **Observability:** Health is a simple HTTP endpoint. Centralized logs,
  metrics, traces, alerting, and SLOs are not configured.
- **Supply chain:** Base images are mutable tags. There is no SBOM, image
  signature verification, dependency vulnerability gate, or provenance
  attestation.
- **Configuration boundary:** Ansible assumes a supported Debian-family host,
  existing SSH/sudo access, Python support, and out-of-band registry login.
  Firewall and TLS policy remain operator responsibilities.
- **Release evidence:** The successful Jenkins #6 run is an earlier native
  deployment at `9aa2e01`; it is not evidence for the Docker stages added in
  `abc9007`.

## 8. Future enhancement plan

1. **Release reproducibility (P0):** Track the Docker build context, run the
   current Docker Jenkinsfile, publish an immutable tag, capture deployment
   evidence, and run Ansible against a non-production Linux host.
2. **Persistence and recovery (P1):** Move complaints to a managed relational
   database, add schema migrations, backup/restore tests, and data-retention
   controls.
3. **Identity and privacy (P1):** Add role-based reviewer authentication,
   authorization tests, HTTPS, secret rotation, PII minimization, and access
   auditing.
4. **Supply-chain hardening (P1):** Pin base images by digest, generate an
   SBOM, scan dependencies and images, sign the release, and verify provenance
   before deployment.
5. **Operational maturity (P2):** Add structured logs, metrics, traces,
   service-level indicators, alerting, and production runbooks.
6. **Safer delivery (P2):** Add blue/green or rolling rollout, traffic
   verification, policy-based production approval, and automated rollback
   rehearsals.
7. **Quality and accessibility (P2):** Add API contract, accessibility,
   security, load, browser-compatibility, and flaky-test monitoring gates.

## 9. Demonstration and evidence package

- `TASKS_REPORTS/DevOps_Task15_24102A2001.docx` - final technical report.
- `TASKS_REPORTS/Task15_Viva_Presentation.html` - browser-based, keyboard
  navigable viva presentation.
- `TASKS_REPORTS/Task15_Viva_QA.md` - viva questions, model answers, and demo
  flow.
- `TASKS_REPORTS/Task15_local_live_portal.png` - screenshot from the temporary
  local app smoke test.
- `TASKS_REPORTS/Task15_architecture.png` and
  `TASKS_REPORTS/Task15_release_workflow.png` - updated diagrams.
- `TASKS_REPORTS/Task15_Evidence/` - test XML, validation summary, and evidence
  metadata.
- `TASK15_FINAL_RELEASE.md` - operator runbook and release decision.

Use the demo script to show the portal, run the two local test commands, open
the historical Jenkins evidence, and explain why the Docker publish and remote
Ansible run are still gated. Record the Jenkins/Docker/Ansible portion only
after credentials and a non-production target are authorized. Do not present
the historical Jenkins build as a run of the current Docker pipeline.
