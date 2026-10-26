# Task 15 Viva Guide

## Seven-minute demonstration

| Time | Demonstration | Evidence boundary |
| --- | --- | --- |
| 0:00-0:45 | Introduce the citizen complaint flow and release decision. Open `Task15_local_live_portal.png` or the local portal. | Screenshot and smoke test use a temporary local JAR, not Docker or Nginx. |
| 0:45-1:45 | Submit or track a seeded complaint; point out the returned tracking token and health endpoint. | Complaint storage is in memory and resets with the process. |
| 1:45-2:45 | Run `mvn -o test`; summarize 17/17 unit tests. | Local test run against this workspace. |
| 2:45-3:45 | Run `mvn -o failsafe:integration-test failsafe:verify -Pselenium`; summarize 5/5 browser journeys. | Chrome 155 logs a non-fatal CDP warning with Selenium 4.19.1. |
| 3:45-4:45 | Show Jenkins history build #6 and explain the checked-out commit. | It ran revision `9aa2e01` and predates the current Docker pipeline. |
| 4:45-5:45 | Walk through the Jenkins Docker stages and `ansible/task14.yml`. | No Docker publish or remote Ansible apply was performed for this snapshot. |
| 5:45-7:00 | Explain risks, rollback, future work, and the P0 release checklist. | Keep release status as HOLD until all external gates pass. |

For an actual live portal demo in this workspace, open `http://localhost:8090/`
and use a second browser tab for `/api/health`. The live preview is backed by
an existing local listener whose deployment provenance is not verified. Do
not stop services already running on ports 8080, 8085, or 8090. Use the deployment commands in
`TASK15_FINAL_RELEASE.md` only after reviewing the target and its parameters.

## Viva questions and model answers

### 1. What problem does this portal solve?

It provides citizens a simple way to submit a civic complaint and track it
with a token, while reviewers can inspect and change a complaint status. The
project emphasizes a repeatable delivery path around that MVP workflow.

### 2. What are the main application layers?

The browser UI calls the JDK HTTP server endpoints. `ComplaintService`
implements complaint operations and delegates input validation and
sanitization to `ValidationService`. Complaint objects are held in an
in-memory `ConcurrentHashMap`.

### 3. What is a CI quality gate?

A quality gate is a condition that must pass before a later release stage is
allowed. In the Jenkinsfile, the unit and Selenium/Failsafe stages precede
Docker build, publish, and deployment, so a failing test prevents promotion.

### 4. Why run both unit tests and Selenium tests?

Unit tests provide fast, focused checks of service behavior. Selenium drives a
browser through user-facing flows, including submission, validation, tracking,
and reviewer behavior. They cover different failure modes.

### 5. Why use an immutable Docker tag?

The Maven version, Jenkins build number, and Git commit identify one build.
That makes deployment and audit evidence traceable and avoids the ambiguity
of a mutable `latest` tag. The pipeline deploys the immutable tag.

### 6. What does Ansible idempotency mean?

Running the same playbook with the same desired state should not keep changing
an already-correct host. With no drift and the same immutable image, the
second apply should report `changed=0`; health probes themselves are read-only.

### 7. How does the Ansible rollback work?

Before changing the container, the playbook captures its previous image
reference and local image ID. If candidate deployment or health validation
fails, the rescue block restores the prior image ID and reruns both health
checks. The play still exits unsuccessfully so the rejected release cannot
look like a successful rollout.

### 8. Does Jenkins automatically provision the Ansible host?

No. Jenkins contains the Docker build, publish, and staging deployment path.
Task 14's Ansible playbook is currently a separate operator-run Linux workflow.
Integrating it requires an authorized Linux agent, inventory, SSH/sudo
credentials, and an explicit deployment approval policy.

### 9. What was actually validated for Task 15?

The current local source passed 17 unit tests and 5 Selenium tests. A
temporary local application process returned HTTP 200 with `status: UP`, and
PyYAML parsed the Ansible YAML files. Jenkins history includes an earlier
successful build at `9aa2e01`. Docker registry publication, current-pipeline
deployment, and remote Ansible provisioning were not run.

### 10. Why is the release on hold?

The Dockerfile and `.dockerignore` are not present in the inspected Git HEAD,
the local Docker base images are not cached, no registry push was authorized,
Ansible is unavailable, and the inventory contains only reserved address
`192.0.2.10`. A real target and its access credentials are also absent.

### 11. What is the most serious application limitation?

Complaint state is in memory. Restarting or replacing the process loses
records, so durable storage, backups, and recovery validation are required
before handling real citizen complaints.

### 12. What would you improve first?

First complete and evidence the versioned Docker deployment plus an authorized
non-production Ansible apply. Before production, prioritize durable
persistence, reviewer authorization, HTTPS, privacy controls, supply-chain
verification, and operational monitoring.

## Recording checklist

- Record only after the Docker registry and non-production target are approved.
- Show the exact Git commit, Jenkins stage results, and immutable image tag.
- Capture Docker deployment evidence and direct/proxied health checks.
- Show both Ansible PLAY RECAPs, including `changed=0` on the repeat run.
- Show rollback restoration and its expected non-zero rehearsal result.
- Remove tokens, SSH material, and citizen personal data from the screen.
- Do not describe the existing historical Jenkins build as the current Docker
  release.
