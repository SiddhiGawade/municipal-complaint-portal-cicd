# Task 14 - Provision, Deploy, Verify, and Recover

This runbook applies the Debian/Ubuntu prerequisite configuration, deploys an
immutable portal image tag, verifies both service entry points, and rehearses
automatic restoration of the exact prior image. The Ansible inventory is still
an example: replace `192.0.2.10` with an authorized, reachable Linux host before
running anything against a real node.

## Prerequisites

- An Ansible Core 2.16+ controller running on Linux, WSL, or macOS. A Windows
  control host should use WSL or a separate Linux controller.
- Install the collection and verify the inventory host:

  ```sh
  ansible-galaxy collection install -r ansible/requirements.yml
  ansible -i ansible/inventory.ini municipal_portal -m ping
  ```

- The remote automation account must already have SSH access and passwordless
  sudo, or supply `--ask-become-pass`. The target needs `/usr/bin/python3` and
  `python3-apt` before fact gathering.
- Provide a registry image reference with a unique, non-`latest` tag. Authenticate
  the target Docker client out-of-band for a private registry; never put registry
  passwords in inventory, command-line variables, or this repository.
- Ensure TCP 8090 is permitted from intended clients. The application is bound
  only to host loopback at TCP 8085; the playbook does not change firewall rules.

## Provision and deploy

First check syntax and review the proposed host changes. The example below uses
a placeholder registry name; replace it with an image tag that has already been
published by the CI pipeline.

```sh
ansible-playbook -i ansible/inventory.ini ansible/task14.yml --syntax-check
ansible-playbook -i ansible/inventory.ini ansible/task14.yml --check --diff \
  -e portal_image=registry.example/municipal-complaint-portal:1.0.0-42-a1b2c3d4e5f6
ansible-playbook -i ansible/inventory.ini ansible/task14.yml --diff \
  -e portal_image=registry.example/municipal-complaint-portal:1.0.0-42-a1b2c3d4e5f6
```

`task14.yml` imports `site.yml` first. That play installs Docker, Nginx, and
their Python integration, creates the restricted deployment paths, renders the
proxy configuration, and enables the services. The deployment play then pulls
the image before changing the named portal container, maps
`127.0.0.1:8085:8085`, sets `PORT=8085`, and checks the backend and Nginx
health endpoints.

## Idempotency evidence

Immediately rerun the same apply command with the same image tag and options:

```sh
ansible-playbook -i ansible/inventory.ini ansible/task14.yml --diff \
  -e portal_image=registry.example/municipal-complaint-portal:1.0.0-42-a1b2c3d4e5f6
```

Keep the first and second full console outputs. With no intervening drift, the
second `PLAY RECAP` should show `changed=0`; health probes are explicitly
read-only. Package indexes and a newly published or retagged mutable image can
cause legitimate changes, which is why deployments require immutable version
tags.

## Health check

The automation checks for HTTP 200 and JSON `status: UP` on both paths. Confirm
from the target after deployment:

```sh
curl --fail --silent --show-error http://127.0.0.1:8085/api/health
curl --fail --silent --show-error http://127.0.0.1:8090/api/health
docker inspect --format '{{.Config.Image}} {{.State.Status}}' municipal-complaint-portal
```

Expected response shape:

```json
{"status":"UP","service":"Municipal Complaint Portal","version":"1.0.0-MVP"}
```

## Rollback/recovery rehearsal

Keep the stable release running, then choose a different candidate tag. The
explicit test flag deliberately rejects the candidate after both of its health
checks pass. Ansible must restore the prior container by its captured image ID,
pass both health checks again, and finish with a non-zero play result so a
rejected release cannot be mistaken for a successful deployment.

```sh
ansible-playbook -i ansible/inventory.ini ansible/task14.yml --diff \
  -e portal_image=registry.example/municipal-complaint-portal:1.0.0-43-f6e5d4c3b2a1 \
  -e portal_test_rollback=true
```

The non-zero exit is expected for this rehearsal. Retain the output showing the
intentional rejection, rollback task, successful backend/proxy probes, and final
failure message. Verify that `docker inspect` reports the original stable image
and that both `curl` commands still return `status: UP`. The same rescue path
also runs if the candidate cannot pass health checks. A first deployment with
no prior container removes the rejected container rather than leaving an
unhealthy release running.

## Evidence record

For a completed target run, record the target hostname and OS, image tags, both
PLAY RECAPs, direct and proxied health responses, the expected rollback-test
exit code, and the post-recovery container image ID. Do not include SSH keys,
registry tokens, or other secrets. Until those target-side outputs are captured,
local YAML validation alone is not evidence that a remote node was provisioned.

The portal currently stores complaints in memory. Container replacement loses
that in-memory state; persistent storage and backups are outside this exercise.
