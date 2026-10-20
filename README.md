# 🏛️ Municipal Complaint Portal (CI/CD Pipeline Project)

[![Java](https://img.shields.io/badge/Java-17%20%7C%2022-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Build-Maven%203.10-C71A36?style=flat&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Jenkins](https://img.shields.io/badge/CI%2FCD-Jenkins%20Pipeline-D24939?style=flat&logo=jenkins&logoColor=white)](https://www.jenkins.io/)
[![Docker](https://img.shields.io/badge/Container-Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![Ansible](https://img.shields.io/badge/IaC-Ansible-EE0000?style=flat&logo=ansible&logoColor=white)](https://www.ansible.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An enterprise-grade, civic grievance redressal web application backed by an automated end-to-end DevOps CI/CD pipeline. The project demonstrates modern software engineering workflows: Agile planning, Git branching, automated compilation, continuous testing quality gates, containerized deployment, and idempotent infrastructure provisioning.

---

## 📋 Table of Contents
- [Project Overview](#-project-overview)
- [Repository URL & SCM](#-repository-url--scm)
- [System Architecture](#-system-architecture)
- [Folder Structure](#-folder-structure)
- [Prerequisites & Stack](#-prerequisites--stack)
- [Build and Run Locally](#-build-and-run-locally)
- [REST API Catalog](#-rest-api-catalog)
- [Branching & Commit Guidelines](#-branching--commit-guidelines)
- [15-Task DevOps Roadmap](#-15-task-devops-roadmap)

---

## 🏛️ Project Overview
The Municipal Complaint Portal enables citizens to register civic grievances (such as potholes, solid waste overflow, water leakages, and malfunctioning streetlights) and receive real-time, deterministic tracking tokens (`CMP-2026-XXXX`). Municipal officials triage submissions through an administrative reviewer dashboard, enforcing transparent SLAs and auditability.

### Core Workflows:
1. **Citizen Grievance Submission:** Multi-category input, ward selection, description, and photo upload.
2. **Automated Input Validation:** Client/server-side regex checking, anti-XSS sanitization, and 5MB attachment enforcement.
3. **Reviewer Decision Triage:** Authenticated workflow to inspect evidence and mark complaints as `APPROVED`, `IN_PROGRESS`, or `REJECTED` with mandatory remarks.
4. **Public Status Tracking:** Fast token lookup returning live status badges and official notes.
5. **DevOps Telemetry:** Automated health probe at `/api/health`.

---

## 🔗 Repository URL & SCM
- **Remote Repository:** [https://github.com/SiddhiGawade/municipal-complaint-portal-cicd.git](https://github.com/SiddhiGawade/municipal-complaint-portal-cicd.git)
- **Primary Branches:** `main` (Production release), `dev` (Integration)
- **Issue Templates:** Configured under `.github/ISSUE_TEMPLATE/`

---

## 🏗️ System Architecture
The application employs a 4-Tier Layered Architecture:
```
[ Web Browser Client (HTML5 / CSS3 / Vanilla JS) ]
                     │  HTTP / REST JSON
                     ▼
[ Controller Layer (App.java: /api/complaints, /api/health) ]
                     │
                     ▼
[ Service & Validation Layer (ComplaintService, ValidationService) ]
                     │
                     ▼
[ Data Persistence Layer (Concurrent Repository / Pluggable DB) ]
```

---

## 📁 Folder Structure
```
municipal-complaint-portal-cicd/
├── .github/
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md             # Issue template for reporting bugs
│       └── feature_request.md        # Issue template for proposed enhancements
├── .gitignore                        # Git exclusion rules for Java, Maven, IDEs
├── BRANCHING_POLICY.md               # Git branching rules, PR workflow, Conventional Commits
├── README.md                         # Primary project documentation
├── TASKS.md                          # 15-Task DevOps Curriculum Specifications
├── pom.xml                           # Apache Maven declarative project object model
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/municipal/portal/
│   │           ├── App.java                  # Embedded server & HTTP endpoint routing
│   │           ├── model/
│   │           │   ├── Complaint.java        # Core complaint entity
│   │           │   └── ComplaintStatus.java  # Lifecycle enum
│   │           └── service/
│   │               ├── ComplaintService.java # Business operations & state persistence
│   │               └── ValidationService.java# Anti-XSS and regex validation
│   └── test/
│       └── java/
│           └── com/municipal/portal/
│               └── ComplaintServiceTest.java # JUnit 5 automated test suite
└── TASKS_REPORTS/                    # Lab assignment docx reports & architecture diagrams
```

---

## ⚙️ Prerequisites & Stack
- **JDK:** Java SE 17 or 22+
- **Build Tool:** Apache Maven 3.10+
- **Version Control:** Git 2.48+
- **Target Deployment:** Apache Tomcat 10 / Nginx / Docker
- **Test Framework:** JUnit 5 & Selenium WebDriver

---

## 🚀 Build and Run Locally

### 1. Build & Run Tests
```powershell
mvn clean test package
```
*Executes all JUnit 5 test suites and packages the executable JAR in `target/`.*

### 2. Run Application Locally
```powershell
java -jar target/municipal-complaint-portal-1.0.0-SNAPSHOT.jar
```
*The web portal will be accessible at [http://localhost:8080](http://localhost:8080).*

### 3. Verify Health Check
```powershell
curl http://localhost:8080/api/health
```

---

## 📡 REST API Catalog
| Verb | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Service uptime and health telemetry | `200 OK` |
| `GET` | `/api/complaints` | Retrieve all grievances for reviewer | `200 OK` |
| `POST` | `/api/complaints` | Register a new citizen complaint | `201 Created` |
| `GET` | `/api/complaints/track?token=...` | Query status by unique token | `200 OK / 404` |
| `POST` | `/api/complaints/status` | Reviewer action (Approve/Reject) | `200 OK / 400` |

---

## ⚙️ Continuous Integration (Jenkins CI)
The portal is continuously integrated using a dedicated Jenkins CI automation job:
- **Job Identifier:** `Municipal-Complaint-Portal-CI`
- **SCM Integration:** Git checkout tracking `refs/remotes/origin/dev`
- **Automated Trigger:** SCM Polling (`H/5 * * * *`) and commit-driven webhooks
- **Build Lifecycle:** Apache Maven 3.10 with OpenJDK (`clean package`)
- **Automated Test Gate:** JUnit 5 test suite executing model and service layer assertions (17 test cases, 0 failures)
- **Artifact Archiving:** Fingerprinted production artifact `target/municipal-complaint-portal-1.0.0-SNAPSHOT.jar`

---

## 🚀 Pipeline as Code & Server Deployment (Jenkinsfile & Nginx)
The project defines a Declarative Pipeline as Code (`Jenkinsfile`) orchestrating automated delivery:
- **Pipeline Identifier:** `Municipal-Complaint-Portal-Pipeline`
- **Delivery Stages:** Checkout, compile/package, unit tests, Selenium integration tests, versioned Docker build, Docker Hub publish, and fresh-container deployment
- **Environment Parameterization:**
  - `DEPLOY_ENV`: Choice parameter (`staging`, `production`, `development`)
  - `APP_PORT`: Backend application port (default `8085`)
  - `NGINX_PORT`: Nginx public ingress port (default `8090`)
- **Image Version:** `<pom-version>-<Jenkins-build-number>-<12-character-commit>` plus a convenience `latest` tag
- **Automated Verification:**
  - Staging Ingress: `http://localhost:8090/`
  - Health Endpoint: `http://localhost:8090/api/health` (HTTP 200 OK)
- **Delivery Setup:** See [TASK12_JENKINS_DOCKER_DEPLOYMENT.md](TASK12_JENKINS_DOCKER_DEPLOYMENT.md) for Jenkins agent requirements, Docker Hub credentials, and evidence collection.

## Ansible Provisioning and Recovery (Task 14)
`ansible/task14.yml` imports the host baseline from `ansible/site.yml`, deploys a
tagged container image, verifies backend and Nginx health, and restores the
previous image if a candidate fails. See
[ansible/TASK14_RUNBOOK.md](ansible/TASK14_RUNBOOK.md) for controller setup,
idempotency verification, and the controlled rollback rehearsal. Replace the
reserved inventory address before targeting a real Linux node.

---

## 🌿 Branching & Commit Guidelines
See [BRANCHING_POLICY.md](BRANCHING_POLICY.md) for full guidelines.
- **Branch Naming:** `feature/<name>`, `bugfix/<name>`, `hotfix/<name>`
- **Commit Messages:** Follow [Conventional Commits](https://www.conventionalcommits.org/):
  - `feat: <description>`
  - `fix: <description>`
  - `docs: <description>`
  - `test: <description>`
  - `ci: <description>`

---

## 🗺️ 15-Task DevOps Roadmap
Detailed task objectives and deliverables can be found in [TASKS.md](TASKS.md).
