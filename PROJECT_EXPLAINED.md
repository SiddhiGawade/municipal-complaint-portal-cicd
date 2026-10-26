# CivicFlow: Our Complete Project Explained

**Project:** CI/CD Pipeline for a Municipal Complaint Portal  
**Guide reviewed:** 26 October 2026  
**Purpose:** Explain all 15 tasks, the website, the code, and the DevOps workflow in simple words with technical details.

This guide describes the current files in this workspace, including local changes. Existing task reports contain historical evidence. The website's recorded test numbers belong to 20 October 2026; they are not automatically updated when the code changes. Creating this guide did not run a new build, test suite, or deployment.

## Contents

1. [What our project does](#1-what-our-project-does)
2. [How we show the project on the website](#2-how-we-show-the-project-on-the-website)
3. [All 15 tasks explained](#3-all-15-tasks-explained)
4. [How the application works internally](#4-how-the-application-works-internally)
5. [How the DevOps pipeline works](#5-how-the-devops-pipeline-works)
6. [Main files and their purpose](#6-main-files-and-their-purpose)
7. [How to run and demonstrate the project](#7-how-to-run-and-demonstrate-the-project)
8. [What is working and what still needs evidence](#8-what-is-working-and-what-still-needs-evidence)
9. [Technical words in simple language](#9-technical-words-in-simple-language)
10. [Short explanation for presentation or viva](#10-short-explanation-for-presentation-or-viva)

## 1. What our project does

Our application is called **CivicFlow**. It helps a citizen report a municipal problem, such as a damaged road, water leakage, overflowing garbage, or a broken streetlight.

The citizen enters the complaint and receives a tracking token, for example `CMP-2026-1001`. A local demo reviewer can inspect the complaint and change its status. The citizen can use the same token to see its current status, remarks, and recorded activity.

The project also explains how developers build, test, package, and deploy this application using DevOps tools.

There are two connected parts:

| Part | Simple meaning | Technical implementation |
| --- | --- | --- |
| Complaint portal | The application people use | HTML, CSS, JavaScript, Java HTTP API, service classes, and an in-memory store |
| DevOps workflow | The process used to check and deliver the application | Git/GitHub, Maven, Jenkins, JUnit, Selenium, Docker, Nginx, and Ansible |

**Example:** A citizen reports a pothole. The portal stores the complaint and shows its tracking ID. Separately, when a developer changes the portal code, the Jenkins pipeline is designed to test the change before building and deploying a Docker image.

The current application is a learning MVP: a small version that demonstrates the main workflow. It does not yet provide durable storage or production reviewer authentication.

## 2. How we show the project on the website

The sidebar contains six main pages. The website brings together the working complaint application and explanations of the engineering behind it.

| Website page | Address after the website URL | What the visitor sees | How it is implemented |
| --- | --- | --- | --- |
| Overview | `/#overview` | Complaint totals, category bars, status chart, recent complaints, and links to the DevOps journey | `overview()` in `app.js`, using complaint data from the Java API |
| Complaints | `/#complaints` | Searchable complaint queue, filters, details, event history, and local demo reviewer actions | `complaintsPage()`, `updateTable()`, and `openDetail()` in `app.js` |
| Submit complaint | `/#submit` | Form for citizen details and the civic issue; a receipt and token after success | `submitPage()` and the form handler in `app.js`; `POST /api/complaints` |
| Track status | `/#track` | Token lookup, current status, remarks, and an activity timeline | `trackPage()` and `track()` in `app.js`; tracking API |
| DevOps Lab | `/#devops` | Interactive explanations of the pipeline, architecture, containers, provisioning, and test evidence | `renderLab()` in `devops.js` |
| Tasks & reports | `/#tasks` | All 15 task cards, explanations, source references, and downloadable Word reports | `renderTasks()` in `devops.js`; bundled `.docx` resources |

### 2.1 Overview: explaining the project at a glance

The dashboard shows four cards calculated from the current session's complaint records:

- **Total complaints:** All records currently stored by the running application.
- **Awaiting review:** Complaints with `PENDING` or `UNDER_REVIEW` status.
- **Active field work:** Complaints with `APPROVED` or `IN_PROGRESS` status.
- **Resolved issues:** Complaints with `RESOLVED` status.

Category bars show the number of complaints in each civic category. A donut chart shows the distribution across all six statuses. Recent complaints appear in a table.

**Technical explanation:** The browser calls `GET /api/complaints`, stores the response in JavaScript state, and calculates these counts. The chart graphics use HTML/CSS; no separate chart library or Node build is required. A separate `/api/dashboard` endpoint also exposes summary counts, but the current overview calculates its cards from the complaint list.

The health indicator uses `/api/health`. It tells us whether this portal responds. It does not monitor Jenkins, Docker Hub, or remote Ansible targets.

### 2.2 Complaints: demonstrating reviewer work

We can search by citizen name or token, filter by status/category/ward, sort the results, and move through pages of eight complaints.

Clicking a complaint opens a detail dialog with its description, status, remarks, and recorded activity. When local demo mode is enabled, the dialog offers only the next statuses allowed by the backend.

**Technical explanation:** Search and filtering operate on the already-loaded complaint list in the browser. Status changes call `POST /api/complaints/status`. The backend validates the transition and records an event. The browser reloads the data after a successful change.

Reviewer actions are enabled by `--demo` on a loopback address. This is a local demonstration setting; there is no reviewer login or production authorization system.

### 2.3 Submit complaint: demonstrating citizen registration

The form collects:

- Citizen name.
- Mobile number or email.
- Civic category.
- Ward or sector.
- Description of the issue.

After successful submission, we show a receipt with the tracking token, a copy button, and a link to track the complaint.

**Technical explanation:** JavaScript checks the form, then sends form-encoded fields using `fetch()` and `URLSearchParams`. The Java backend validates the input again, creates a complaint with `PENDING` status, and returns HTTP `201` with a JSON response containing the token and `SUCCESS` result.

The current form does not upload photographs or documents. Some older planning text discusses attachments, but the current website explicitly says attachments and notifications are outside this release.

### 2.4 Track status: demonstrating transparency

The citizen enters a token and sees the complaint details, latest status, reviewer remarks, and events recorded during this application session.

**Technical explanation:** The browser calls `/api/complaints/track?token=...`. The backend trims and normalizes the token, looks it up, and returns the complaint or HTTP `404` if it does not exist. The timeline comes from stored complaint events, rather than an invented sequence of steps.

Tracking is refreshed when a lookup is submitted; this is not a WebSocket or continuously pushed live feed.

### 2.5 DevOps Lab: making technical tasks understandable

The lab contains four tabs:

| Tab | What we show | What it teaches |
| --- | --- | --- |
| Pipeline explorer | Clickable stages, sample commands, inputs, outputs, gates, and Play/Pause/Next/Reset controls | How a source change moves toward deployment |
| Architecture | Browser-to-backend flow, Jenkins delivery path, separate Ansible path, and recorded diagrams | Which component does which job |
| Docker & Ansible | Container concepts, host configuration, and recovery explanation | How software is packaged and its environment is prepared |
| Quality & evidence | Recorded test totals, historical failed/successful Jenkins runs, and pending release gates | How we distinguish a definition from a verified execution |

The pipeline explorer has three scenarios:

1. **Successful release:** Illustrates the stages from source checkout through deployment verification.
2. **Failed quality gate:** Illustrates browser tests failing and the Docker build, publish, and deploy stages being skipped.
3. **Ansible recovery:** Illustrates checking a candidate release, rejecting it, and restoring the prior stable image.

**Technical explanation:** These animations run inside `devops.js` using browser state and timers. Selecting a scenario or pressing Play does not call Jenkins, execute Docker, publish an image, or run Ansible.

### 2.6 Tasks & reports: showing the complete academic journey

Each of the 15 cards contains the task number, title, learning chapter, simple explanation, deliverables, relevant source files, an evidence label, and a report download button.

Visitors can search for concepts such as Git, Docker, testing, or rollback and filter by learning chapter.

**Technical explanation:** Task descriptions are defined in the `taskData` array in `devops.js`. Downloads use `/reports/task-1.docx` through `/reports/task-15.docx`. The Java server serves bundled copies from `src/main/resources/reports/`. Original reports remain in `TASKS_REPORTS/`.

These reports are files packaged with the application. They are not generated on demand from Jenkins or the current test results.

## 3. All 15 tasks explained

### Task 1: Problem definition and scope

**Simple explanation:** We first decided what problem to solve, who would use the system, and which features belonged in the first version.

**Technical explanation:** The scope covers citizen submission, data validation, reviewer decisions, and token-based tracking. Citizens, municipal reviewers, and the development/operations team have different responsibilities. This defines the MVP and prevents uncontrolled feature expansion.

**How we show it:** The Overview explains the civic purpose. Submit complaint, Complaints, and Track status demonstrate the chosen scope. The Task 1 card links to the problem-definition report.

**Files/evidence:** [TASKS.md](TASKS.md), Task 1 report, and the portal's implemented workflows.

### Task 2: Agile planning and DevOps workflow

**Simple explanation:** We divided the project into smaller pieces of work so we could build and check it step by step.

**Technical explanation:** The backlog uses user stories such as complaint registration and approval/rejection. Acceptance criteria describe when a feature works correctly. The task plan connects planning, coding, building, testing, deployment, and operations.

**How we show it:** Tasks & reports displays the 15-task learning journey, grouped into chapters. DevOps Lab shows the development-to-delivery flow. The website is not a live Scrum or Kanban board.

**Files/evidence:** [BACKLOG.md](BACKLOG.md), [TASKS.md](TASKS.md), and Task 2 report. `BACKLOG.md` is a Task 6 snapshot; later website features such as dashboard charts and search now exist even though that older backlog still lists them as pending.

### Task 3: Requirements, architecture, and technology setup

**Simple explanation:** We decided what the application needed to do and how its parts would communicate.

**Technical explanation:** The browser uses HTML/CSS/JavaScript. Java 17's built-in `HttpServer` serves the website and API. Service classes handle complaint rules and validation. A `ConcurrentHashMap` stores complaint records in memory. Maven compiles, tests, and packages the project as an executable JAR.

**How we show it:** DevOps Lab > Architecture shows the application layers. The citizen and reviewer pages demonstrate the requirements.

**Files/evidence:** [pom.xml](pom.xml), `App.java`, the `model/` and `service/` folders, architecture diagrams, and Task 3 report. The current server is not Spring Boot or Tomcat, and there is no operational database.

### Task 4: Git and GitHub repository initialization

**Simple explanation:** We placed the project in version control so changes could be tracked and shared.

**Technical explanation:** The repository includes project documentation, ignore rules, issue templates, and a branch policy. The policy assigns `main` to releases, `dev` to integration, and `feature/...` branches to individual features. Commit messages describe the purpose of each change.

**How we show it:** The Task 4 report card explains repository setup. The Pipeline explorer begins with Git/GitHub checkout. The website does not run Git commands or display a live repository history.

**Files/evidence:** [.gitignore](.gitignore), [BRANCHING_POLICY.md](BRANCHING_POLICY.md), `.github/ISSUE_TEMPLATE/`, Git history, and Task 4 report. A written branch policy describes the intended workflow; it alone does not prove current GitHub protection settings.

### Task 5: Feature development with branching

**Simple explanation:** We developed complaint registration on its own branch and then brought it into the shared development branch.

**Technical explanation:** The registration workflow checks the citizen's details, validates the category/contact/ward/description, creates a tracking token, and stores a pending complaint. Git history records the submission feature and its merge into `dev`.

**How we show it:** Submit complaint demonstrates the feature. A successful submission displays the token receipt. Tasks & reports provides the Task 5 report and source reference.

**Files/evidence:** `ComplaintRegistrationWorkflow.java`, `ComplaintService.java`, `ValidationService.java`, registration tests, and historical merge commit `183f3f3`. The current HTTP submission handler calls `ComplaintService` directly; the workflow class also documents/tests the original feature design.

### Task 6: MVP completion and Git collaboration

**Simple explanation:** We completed the reviewer and tracking functions and practiced handling conflicting code changes.

**Technical explanation:** Reviewer services support triage, approval, rejection, field work, and resolution. The recorded merge conflict combined a minimum description length of 15 characters with a maximum of 1,500 characters. Git history contains the conflict-resolution commit; the backlog records the `v1.0.0` baseline.

**How we show it:** Complaints demonstrates reviewer actions. Track status shows the outcome and history. The validation rules appear in the submission form. Git collaboration itself is explained through the report, rather than performed by the website.

**Files/evidence:** `ReviewerActionService.java`, `ComplaintService.java`, `ValidationService.java`, [BACKLOG.md](BACKLOG.md), Task 6 report, and conflict-resolution commit `a0b90ce`.

### Task 7: Jenkins installation and continuous integration job

**Simple explanation:** We configured Jenkins to fetch the project and build it automatically instead of repeating every step manually.

**Technical explanation:** The documented CI job, `Municipal-Complaint-Portal-CI`, uses source control, Maven, automated tests, and artifact archiving. The setup describes scheduled SCM polling and commit-driven triggers. Actual trigger activity needs Jenkins job/history evidence.

**How we show it:** The Pipeline explorer explains checkout, compilation, tests, and the JAR artifact. Quality & evidence presents recorded results. The Task 7 report contains the historical setup evidence.

**Files/evidence:** [README.md](README.md), `pom.xml`, Jenkins history, and Task 7 report. The current pipeline-as-code definition is in [Jenkinsfile](Jenkinsfile); the website is not a Jenkins connection or job console.

### Task 8: Pipeline as code and server deployment

**Simple explanation:** We wrote the delivery steps in a file so Jenkins could follow a repeatable process.

**Technical explanation:** A declarative `Jenkinsfile` defines stages and parameters such as `DEPLOY_ENV`, `APP_PORT`, and `NGINX_PORT`. The earlier deployment ran a native Java JAR behind Nginx. The current deployment script supports the newer Docker delivery path.

**How we show it:** Pipeline explorer explains the stages. Architecture shows Nginx as the entry point and Java as the backend. The Task 8 report records the earlier deployment.

**Files/evidence:** [Jenkinsfile](Jenkinsfile), [deploy/deploy.ps1](deploy/deploy.ps1), and Task 8 report. Historical Jenkins build #6 used the earlier native JAR/Nginx workflow; it does not verify the later Docker stages.

### Task 9: Selenium test design and local execution

**Simple explanation:** We wrote automated browser tests that use the site in a similar way to a person.

**Technical explanation:** Selenium WebDriver drives Chrome, enters form data, clicks controls, and checks outcomes using assertions. Explicit waits wait for expected page conditions. Failed tests can save screenshots under `target/selenium-failures/`. Maven Failsafe runs integration tests through the `selenium` profile.

**How we show it:** The working pages demonstrate the tested journeys. DevOps Lab > Quality & evidence displays the recorded browser-test baseline and explains its date.

**Files/evidence:** `SeleniumComplaintJourneyIT.java`, [pom.xml](pom.xml), Task 9 report, and recorded Failsafe XML. The current browser suite also covers newer dashboard, filtering, lifecycle, report, accessibility, and response-ordering behavior; the displayed historical 5/5 total is not its current test count.

### Task 10: Continuous testing and quality gates

**Simple explanation:** We made successful tests a requirement before deployment and demonstrated what happens when a defect breaks the tests.

**Technical explanation:** A deliberate API response-contract defect was introduced in commit `33d9f01`. Historical Jenkins build #5 failed the browser-test gate and skipped downstream deployment. Commit `9aa2e01` restored the expected `SUCCESS` response, and historical build #6 passed. The current pipeline places both JUnit and Selenium before Docker delivery.

**How we show it:** Quality & evidence shows the historical failure/correction comparison. The Failed quality gate scenario animates the same rule: failed browser tests stop later delivery stages.

**Files/evidence:** [Jenkinsfile](Jenkinsfile), `SeleniumComplaintJourneyIT.java`, Task 10 report, and the bundled comparison image. The animation teaches the rule; historical Jenkins records provide evidence of the earlier execution.

### Task 11: Docker image and container lifecycle

**Simple explanation:** We defined a package that contains the application and its Java runtime, so it can run as a container.

**Technical explanation:** The multi-stage Dockerfile builds with Maven and Java 17, then copies the executable JAR into a Java 17 runtime image. It runs as UID/GID `10001`, exposes port `8085`, and sets `BIND_ADDRESS=0.0.0.0` for container networking. `.dockerignore` limits the build context.

**How we show it:** DevOps Lab > Docker & Ansible explains the image, runtime, ports, and lifecycle. The Build image stage shows the role of `docker build`. The Task 11 report describes lifecycle commands.

**Files/evidence:** [Dockerfile](Dockerfile), [.dockerignore](.dockerignore), and Task 11 report. Image build, run, logs, stop, restart, and remove are the lifecycle steps to demonstrate; the recorded evidence does not verify a completed current runtime lifecycle.

### Task 12: Jenkins-Docker continuous deployment

**Simple explanation:** We extended Jenkins so passing code can be packaged into a versioned image, published, and deployed as a fresh container.

**Technical explanation:** Jenkins constructs a tag from the Maven version, build number, and Git commit. It builds the image, authenticates using managed credential `dockerhub-credentials`, pushes the versioned tag plus `latest`, and calls `deploy.ps1` with the versioned image reference. The script checks the backend and Nginx and writes deployment evidence.

**How we show it:** Pipeline explorer includes Prepare version, Build image, Publish image, and Deploy & verify. Architecture connects GitHub, Jenkins, the registry, and the runtime.

**Files/evidence:** [Jenkinsfile](Jenkinsfile), [deploy/deploy.ps1](deploy/deploy.ps1), [TASK12_JENKINS_DOCKER_DEPLOYMENT.md](TASK12_JENKINS_DOCKER_DEPLOYMENT.md), and Task 12 report. The implementation exists; a successful current registry push, digest, and rollout are not established by the recorded snapshot.

### Task 13: Configuration management with Ansible

**Simple explanation:** We wrote instructions that prepare the server consistently, instead of installing and configuring everything by hand.

**Technical explanation:** Ansible inventory identifies target machines. The host playbook describes packages, deployment directories, a locked service identity, configuration templates, and Docker/Nginx services on a Debian/Ubuntu host. The container contains Java, so the target host does not need Maven, Tomcat, or a separate Java installation for this deployment model.

**How we show it:** Docker & Ansible explains host preparation. Architecture shows Ansible as a separate operator-run workflow. The Task 13 card links the playbook and configuration specification to the report.

**Files/evidence:** [ansible/site.yml](ansible/site.yml), [ansible/inventory.ini](ansible/inventory.ini), [ansible/CONFIGURATION_SPEC.md](ansible/CONFIGURATION_SPEC.md), templates, and Task 13 report. The inventory still contains example address `192.0.2.10`; it is not a provisioned target.

### Task 14: Provisioning, idempotency, health, and recovery

**Simple explanation:** We defined how to deploy a candidate release, check that it works, and recover the previous release if it fails.

**Technical explanation:** `task14.yml` imports the host baseline, captures the previous container image ID, pulls a versioned candidate, runs it, and checks both direct and Nginx-facing health endpoints. Its rescue path restores the previous image when possible. Repeating the same desired configuration should avoid unnecessary changes: this is idempotency.

**How we show it:** The Ansible recovery scenario illustrates Configure host, Record stable, Run candidate, Check health, and Restore stable. The explanation makes clear that a recovered service does not make the rejected release successful.

**Files/evidence:** [ansible/task14.yml](ansible/task14.yml), [ansible/health_checks.yml](ansible/health_checks.yml), [ansible/TASK14_RUNBOOK.md](ansible/TASK14_RUNBOOK.md), and Task 14 report. Remote provisioning, a second-run `changed=0` result, and actual rollback still need target-side execution evidence. Jenkins does not currently invoke this playbook.

### Task 15: Final release, documentation, and viva

**Simple explanation:** We brought the project explanation, diagrams, reports, recorded checks, limitations, and presentation material together.

**Technical explanation:** The final package includes an architecture view, release workflow, local validation metadata, test XML, screenshot evidence, troubleshooting, release gates, and an enhancement plan. The recorded local baseline passed 17 unit tests and 5 browser journeys. External Docker and Ansible rollout evidence remains pending, so the documented release decision is `HOLD`.

**How we show it:** Overview introduces the complete story. DevOps Lab explains the architecture and evidence. Tasks & reports makes all 15 reports downloadable. A separate HTML presentation and Markdown viva guide support the demonstration.

**Files/evidence:** [TASK15_FINAL_RELEASE.md](TASK15_FINAL_RELEASE.md), [Task15_Viva_QA.md](TASKS_REPORTS/Task15_Viva_QA.md), [Task15_Viva_Presentation.html](TASKS_REPORTS/Task15_Viva_Presentation.html), and [Task15_Evidence](TASKS_REPORTS/Task15_Evidence/).

**Historical-document detail:** The older Task 15 report says Dockerfile and `.dockerignore` were untracked at its capture time. Current Git history includes them in commit `fb59a55`. That particular source-control issue has been addressed, but committing the files does not prove that the Docker delivery pipeline has executed successfully.

## 4. How the application works internally

### 4.1 Application architecture

```text
Browser: HTML + CSS + JavaScript
                  |
                  | HTTP requests and JSON responses
                  v
App.java: JDK HttpServer, API routing, static resources
                  |
                  v
ComplaintService: complaint creation, lookup, status rules
                  |
                  +----> ValidationService: input checks and sanitization
                  |
                  v
ConcurrentHashMap: in-memory complaints and their event history
```

Nginx can sit in front of the Java server as a reverse proxy. The browser then talks to Nginx, which forwards requests to the backend.

### 4.2 What happens when a citizen submits a complaint?

1. The citizen fills in the form.
2. JavaScript checks required fields and input formats.
3. The browser sends a form-encoded `POST /api/complaints` request.
4. `App.java` applies request checks and calls the complaint service.
5. The service sanitizes selected fields, normalizes values, and validates them.
6. It generates a token such as `CMP-2026-1021`, creates a `PENDING` complaint, and stores it in memory.
7. The API returns the token in JSON with HTTP `201`.
8. The page displays the receipt and loads updated complaint data.

**Technical detail:** The current token prefix is hardcoded as `CMP-2026-`. An `AtomicInteger` generates the numeric sequence starting at `1001` for each new application instance. These are local tracking identifiers, not globally permanent IDs across restarts or multiple servers.

### 4.3 Main validation rules

| Field | Current rule |
| --- | --- |
| Citizen name | Required; validated sanitized name is at most 100 characters |
| Contact | Required; at most 150 characters; a supported email format or 10-digit mobile beginning with 6, 7, 8, or 9 |
| Category | Must match one of the six supported civic categories |
| Ward | Required; at most 10 characters after trimming |
| Description | Between 15 and 1,500 characters after sanitization and trimming |
| Rejection remarks | At least 5 characters after trimming |
| Reviewer remarks | At most 1,500 characters |

The six categories are Roads & Potholes, Solid Waste Management, Water Supply & Leakage, Streetlights & Electricity, Drainage & Sewage, and Public Health & Sanitation.

The backend removes HTML-like tags from selected submission fields. The frontend escapes displayed text, and the server sends security headers. These are useful safeguards within the MVP; they do not replace production authentication, durable audit storage, or a full security review.

### 4.4 Complaint status flow

The usual demonstration path is:

```text
PENDING -> UNDER_REVIEW -> APPROVED -> IN_PROGRESS -> RESOLVED
```

The exact implemented transitions are:

| Current status | Allowed next statuses |
| --- | --- |
| `PENDING` | `UNDER_REVIEW`, `APPROVED`, `IN_PROGRESS`, `REJECTED` |
| `UNDER_REVIEW` | `APPROVED`, `IN_PROGRESS`, `REJECTED` |
| `APPROVED` | `IN_PROGRESS`, `REJECTED` |
| `IN_PROGRESS` | `RESOLVED` |
| `RESOLVED` | None |
| `REJECTED` | None |

The backend enforces this table. Each successful change records the status, remarks, actor, and time. Resolved and rejected complaints cannot be reopened in this demo.

### 4.5 API endpoints

An API endpoint is an address the frontend uses to request data or an action.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/health` | Return service health, demo mode, storage type, and check time |
| `GET` | `/api/complaints` | Return the complaint list |
| `POST` | `/api/complaints` | Validate and register a complaint |
| `GET` | `/api/complaints/track?token=CMP-2026-1001` | Look up one complaint and its history |
| `POST` | `/api/complaints/status` | Apply an allowed reviewer action in local demo mode |
| `GET` | `/api/dashboard` | Return total and grouped counts |
| `GET` | `/reports/task-N.docx` | Download a bundled report, where `N` is 1 through 15 |

POST requests use `application/x-www-form-urlencoded`; responses use JSON. Invalid data returns an error response, and an unknown token returns `404`.

### 4.6 Frontend, packaging, and usability

- `index.html` defines the sidebar, header, main view, and dialogs.
- `styles.css` defines layout, colors, charts, responsive behavior, and focus styles.
- `app.js` renders application pages and communicates with the API.
- `devops.js` renders learning content, task cards, and animations.
- Hash routes such as `#submit` switch views without loading separate HTML pages.
- The interface includes labels, keyboard navigation, a skip link, status announcements, and a mobile menu.
- Request sequence checks prevent older tracking/detail responses from replacing a newer selection.
- Maven packages website assets, diagrams, and reports inside the JAR, so no Node server or CDN is required.

## 5. How the DevOps pipeline works

### 5.1 Jenkins delivery path

```text
Git/GitHub source
       |
       v
Checkout -> Prepare image version -> Compile -> Package + JUnit
                                                       |
                                                       v
                                                 Selenium gate
                                                       |
                                                       v
Docker build -> Registry publish -> Fresh container -> Health checks
```

The current `Jenkinsfile` defines these stages:

| Stage | What it does in simple words | Main technical action |
| --- | --- | --- |
| Checkout | Fetch the selected source revision | `checkout scm` |
| Prepare Image Version | Identify exactly which build we are delivering | Validate parameters; read POM version, build number, and Git hash |
| Build | Compile Java code | `mvn clean compile` |
| Package and Unit Tests | Check code behavior and create the JAR | `mvn test package`; archive `target/*.jar` |
| Selenium Integration Tests | Check browser journeys before delivery | `mvn failsafe:integration-test failsafe:verify -Pselenium` |
| Build Docker Image | Package the application and runtime | Multi-stage `docker build` with labels and tags |
| Publish Docker Image | Store the image in the registry | Managed registry login and `docker push` |
| Deploy Fresh Container | Run the selected version and verify it | `deploy/deploy.ps1`; backend/Nginx health checks; evidence JSON |

Jenkins publishes Surefire and Failsafe results in its post-build handling. Failed build/test stages prevent the downstream delivery stages from running.

The Jenkinsfile uses Windows `bat` steps and PowerShell. Its agent therefore needs the Windows tooling described in the deployment setup, even though the application container uses a Linux Java runtime.

### 5.2 Image tags and traceability

The versioned image reference follows this format:

```text
<namespace>/<repository>:<pom-version>-<build-number>-<12-character-commit>
```

This links a deployment to the source commit and Jenkins run. The pipeline also publishes `latest`, but deploys the versioned reference.

The project calls the build-specific reference an immutable tag. Technically, registry tags can still be reassigned unless the registry enforces immutability. An image digest/image ID identifies the image content more precisely; the Ansible recovery path records the previous local image ID.

### 5.3 Nginx and ports

```text
Browser -> Nginx on port 8090 -> Java backend on port 8085
```

Nginx is a reverse proxy: it receives the browser request and forwards it to the application. The Docker deployment model maps the backend to host loopback, keeping the backend behind the proxy. A direct local JAR demonstration can use port `8085` without Nginx.

### 5.4 Separate Ansible provisioning and recovery path

```text
Operator + inventory + SSH access
             |
             v
Prepare Linux host -> Record previous image -> Run candidate
                                                  |
                                                  v
                                        Check backend + proxy
                                           /             \
                                        pass             fail
                                         |                |
                                   Accept release   Restore previous image
                                                          |
                                                    Recheck health
                                                          |
                                              Report candidate failure
```

Ansible and Jenkins have different roles in this project. Jenkins checks and delivers the application. Ansible prepares a Linux target and includes a separate deployment/recovery workflow. The current Jenkinsfile does not automatically run Ansible.

**Idempotency example:** If Docker, Nginx, configuration files, and the selected container already match the desired state, running the same playbook again should avoid unnecessary modifications. A real second-run `PLAY RECAP` is needed to prove this.

**Recovery example:** If a new candidate fails health checks and a previous stable image exists, restore that exact prior image. The candidate deployment still reports failure even if service recovery succeeds. On a first deployment without a prior release, the playbook cannot claim to restore a nonexistent stable version.

## 6. Main files and their purpose

Paths below are relative to the repository root.

| File or folder | Purpose |
| --- | --- |
| `src/main/java/com/municipal/portal/App.java` | Start the HTTP server, route API requests, serve assets/reports, and apply request checks |
| `src/main/java/com/municipal/portal/Json.java` | Encode Java values as JSON responses |
| `src/main/java/com/municipal/portal/model/` | Complaint, status, request, and event data models |
| `src/main/java/com/municipal/portal/service/ComplaintService.java` | Create/store/find complaints and enforce status transitions |
| `src/main/java/com/municipal/portal/service/ValidationService.java` | Validate fields and sanitize selected input |
| `src/main/java/com/municipal/portal/service/ComplaintRegistrationWorkflow.java` | Original registration workflow abstraction |
| `src/main/java/com/municipal/portal/service/ReviewerActionService.java` | Reviewer operation helpers and filtering |
| `src/main/java/com/municipal/portal/service/DemoData.java` | Seed synthetic complaint records |
| `src/main/resources/web/` | Website HTML, CSS, JavaScript, icons, and diagrams |
| `src/main/resources/reports/` | Fifteen Word reports bundled for website downloads |
| `src/test/java/com/municipal/portal/` | Service, lifecycle, normalization, HTTP API, and Selenium tests |
| `pom.xml` | Java version, dependencies, Maven plugins, and Selenium profile |
| `Jenkinsfile` | Build/test/publish/deploy pipeline as code |
| `Dockerfile` and `.dockerignore` | Container build and build-context rules |
| `deploy/deploy.ps1` | Windows deployment automation and health verification |
| `ansible/` | Linux host configuration, templates, inventory, deployment, and recovery |
| `TASKS.md` | Original objectives and deliverables for all 15 tasks |
| `TASKS_REPORTS/` | Original task reports, evidence, diagrams, and viva material |
| `TASK15_FINAL_RELEASE.md` | Recorded release assessment, runbook, limitations, and future work |
| `target/` | Generated classes, JARs, and test/build outputs |

## 7. How to run and demonstrate the project

### 7.1 Build and start a local demo

From the repository root in PowerShell, with JDK 17+ and Maven available:

```powershell
mvn package
java -jar target/municipal-complaint-portal-1.0.0-SNAPSHOT.jar 8085 --demo
```

Open `http://localhost:8085/`.

`--demo` seeds 20 synthetic complaints and enables local reviewer actions. Without it, the application seeds two sample complaints and disables reviewer writes. Data and event history reset when the process restarts. Demo mode requires a loopback bind address; a non-loopback `BIND_ADDRESS` is incompatible with it.

If port `8085` is occupied, use another free port, for example:

```powershell
java -jar target/municipal-complaint-portal-1.0.0-SNAPSHOT.jar 8086 --demo
```

Then open `http://localhost:8086/`. Do not stop an existing service just to follow this example.

For an environment with dependencies already cached, Maven's `-o` option runs offline, for example `mvn -o package`. Offline builds fail if a required dependency/plugin is not cached.

### 7.2 Check the backend and run tests

```powershell
Invoke-RestMethod http://localhost:8085/api/health
mvn test
mvn verify -Pselenium
```

Use the port chosen for your running application in the health URL. The Selenium suite starts its own isolated test server; it needs a compatible Chrome/ChromeDriver setup. Read the actual new Maven results to report current totals.

Unit/service/API reports are written under `target/surefire-reports/`. Browser integration reports are written under `target/failsafe-reports/`.

These commands are instructions for a fresh demonstration; they were not executed while writing this guide.

### 7.3 Suggested website demonstration order

1. Open Overview and explain the complaint totals, category bars, and status chart.
2. Open Submit complaint and register a synthetic issue. Save its returned token.
3. Open Track status and show the new pending complaint.
4. Open Complaints, search for the token, and open its details.
5. In local demo mode, move it through `APPROVED`, `IN_PROGRESS`, and `RESOLVED`.
6. Track it again and show the status, remarks, and event timeline.
7. Open DevOps Lab and step through Successful release.
8. Select Failed quality gate and explain why delivery stops.
9. Select Ansible recovery and explain the separate provisioning/recovery path.
10. Open Architecture and Quality & evidence. Explain the dates and pending external gates.
11. Open Tasks & reports, filter by a concept, expand a card, and download a report.

Show actual Jenkins logs, registry records, or Ansible output separately when presenting execution evidence. The lab animation alone cannot establish that those tools ran.

## 8. What is working and what still needs evidence

This table separates current implementation from the recorded execution history.

| Area | What the repository establishes |
| --- | --- |
| Complaint application | Current code implements submission, validation, tracking, filtering, local reviewer actions, and event history |
| Website showcase | Current frontend includes the dashboard, four-tab DevOps Lab, three walkthrough scenarios, and 15 report downloads |
| Local data | Synthetic samples and submitted complaints live in process memory |
| Recorded local tests | 17 unit tests and 5 browser journeys passed in the 20 October 2026 baseline at `abc900735330` |
| Current expanded test suite | Source contains additional tests; this guide does not claim a fresh execution result or reuse the historical count as today's total |
| Historical Jenkins quality gate | Build #5 failed on the deliberate defect; build #6 passed at `9aa2e01` using the earlier JAR/Nginx delivery |
| Docker source files | Dockerfile and `.dockerignore` exist and are included in Git history at `fb59a55` |
| Current Docker delivery | Pipeline/script implementation exists; the recorded snapshot does not establish a successful image build, push, and deployment |
| Ansible | Host setup and recovery code exists; inventory still uses an example address, and remote apply/idempotency/rollback evidence is pending |
| Release decision | The website and recorded Task 15 package show `HOLD` while external rollout gates remain unverified |

### Current limitations and next improvements

- **Persistence:** Add a database, migrations, backups, and restore checks. Current records disappear on process/container replacement.
- **Identity:** Add authenticated users and role-based reviewer authorization. Local demo mode is not a login system.
- **Citizen features:** Add durable attachment storage and email/SMS notifications. The current release provides neither.
- **Production operation:** Add HTTPS, privacy controls, centralized logs, metrics, alerts, and stronger security testing.
- **Delivery evidence:** Run the current Docker pipeline and capture the image digest, exact version, deployment output, and health results.
- **Infrastructure evidence:** Use a real Linux target and capture first apply, repeat-run idempotency, and rollback results.
- **Availability and supply chain:** Improve rollout strategy, pin/verify images, and add dependency/image scanning and release provenance.

## 9. Technical words in simple language

| Word | Simple meaning | Example in this project |
| --- | --- | --- |
| MVP | Small first version with essential features | Submit, review, and track a complaint |
| Frontend | What the visitor sees and interacts with | Dashboard, forms, tables, charts |
| Backend | Server code that handles data and rules | Java API and complaint services |
| API | Agreed way for software parts to communicate | Browser requests `/api/complaints` |
| HTTP | Request/response protocol used by the website | `GET` data or `POST` a complaint |
| JSON | Text format for structured data | API response containing token and status |
| Validation | Checking that input meets rules | Rejecting a short description |
| Sanitization | Cleaning selected input before use | Removing HTML-like tags from a name |
| Git | Tool that records source-code history | Commits, branches, merges, tags |
| GitHub | Remote hosting/collaboration service for Git | Repository and pull requests |
| Branch | Separate line of development | `feature/complaint-submission` |
| Pull request | Proposal to review and merge changes | Feature branch into `dev` |
| Merge conflict | Overlapping changes requiring a decision | Combining minimum and maximum description rules |
| Maven | Tool that builds and tests Java projects | `mvn package` |
| Artifact | File produced by a build | Executable `.jar` |
| Jenkins | Automation server that runs pipeline steps | Checkout, tests, image publish, deploy |
| CI | Automatically integrating and checking changes | Build and test after source changes |
| Continuous delivery | Keeping tested changes ready for release | Creating a versioned image after passing gates |
| Continuous deployment | Automatically deploying passing changes | The defined Jenkins fresh-container stage |
| JUnit | Framework for automated Java tests | Validation and status-transition checks |
| Selenium | Tool for controlling a browser in tests | Submit a complaint and check the receipt |
| Assertion | Check of an expected test outcome | Verify that a response contains a tracking token |
| Quality gate | Required check before proceeding | Selenium must pass before Docker delivery |
| Docker image | Packaged application and runtime | Built from the Dockerfile |
| Container | Running instance of an image | Portal process inside Docker |
| Registry | Place where container images are stored | Docker Hub |
| Tag | Human-readable image label | Version/build/commit reference |
| Digest | Content-based identifier for an image | Registry evidence identifying exact image content |
| Reverse proxy | Server that forwards requests to another server | Nginx forwarding to Java |
| Ansible | Tool for declaring and applying server setup | Install prerequisites and configure services |
| Inventory | List of Ansible target machines | `ansible/inventory.ini` |
| Playbook | YAML instructions for Ansible | `ansible/task14.yml` |
| Idempotency | Repeating setup avoids unnecessary changes | Second unchanged deployment reports `changed=0` |
| Health check | Simple request checking service response | `/api/health` returns `UP` |
| Rollback | Restoring a previous release | Recreate the prior container image |
| In-memory storage | Data held only in the running process | Complaint `ConcurrentHashMap` |
| Evidence | Retained proof of an actual action/result | Test XML, Jenkins log, registry digest, PLAY RECAP |

## 10. Short explanation for presentation or viva

> Our project is CivicFlow, a Municipal Complaint Portal with a DevOps workflow. Citizens can register civic issues and receive a tracking token. A local demo reviewer can update complaints, and citizens can see the status and recorded history.
>
> The website uses HTML, CSS, and JavaScript. A Java 17 HTTP server handles the API, validation, complaint rules, and in-memory storage. Maven builds the executable JAR, including the website and task reports.
>
> Across 15 tasks, we covered problem definition, Agile planning, architecture, Git collaboration, Jenkins, automated tests, Docker delivery, Ansible configuration, recovery, and final documentation. The website presents these through its dashboard, working complaint pages, interactive DevOps Lab, and downloadable reports.
>
> Jenkins is designed to run JUnit and Selenium before Docker build, publication, and deployment. Ansible is a separate workflow for preparing a Linux target and recovering a previous release after failure.
>
> We distinguish working application behavior, recorded test evidence, and illustrative animations. The displayed 17 unit tests and 5 browser journeys are a dated baseline. Current Docker rollout and remote Ansible verification still need execution evidence. Our next priorities are durable storage, authenticated reviewer access, and completing those release checks.

