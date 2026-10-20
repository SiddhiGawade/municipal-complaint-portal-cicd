# Municipal Complaint Portal - Configuration Specification

## Scope and implementation choice

This configuration uses Ansible to prepare an Ubuntu/Debian Linux host for the
portal's Docker-and-Nginx deployment model. `site.yml` prepares the host and
reverse proxy. `task14.yml` then pulls and deploys a tagged application image,
checks the backend and Nginx health endpoints, and restores the previously
running image if the candidate release fails validation.

The portal is an executable Java 17 application packaged as a JAR and run in a
Docker container. The image already contains the Java 17 runtime and runs as
UID/GID 10001. Therefore, Java, Maven, and Tomcat are not required on the host.
The application currently keeps complaint records in memory; there is no
database service to provision.

## Target and access assumptions

| Item | Requirement |
| --- | --- |
| Operating system | Ubuntu or Debian with APT; a current supported LTS/release is recommended |
| Ansible controller | Ansible Core 2.16+ on Linux/WSL/macOS; Windows users should use WSL or a Linux control host |
| Remote access | SSH to the target as an existing automation account (`ansible`) with passwordless sudo or a become password |
| Remote Python bootstrap | `/usr/bin/python3` and `python3-apt` must be available for fact gathering and APT modules; minimal images need a one-time bootstrap before this play |
| Inventory address | Replace `192.0.2.10` in `inventory.ini`; it is a documentation-only TEST-NET address |
| Suggested capacity | At least 2 vCPU, 2 GiB RAM, and 10 GiB free disk for the OS, Docker layers, and logs; tune for expected traffic |
| Build/deploy | Jenkins publishes an immutable tagged image; Task 14 provisions the node and deploys the selected tag |

The SSH automation account is an access/bootstrap prerequisite and is not
created by this playbook. Use SSH keys or an agent; do not place private keys,
registry passwords, or other secrets in inventory or source control.

## Managed prerequisites

| Category | Managed value | Purpose |
| --- | --- | --- |
| Packages | `docker.io` | Docker Engine and container lifecycle on the host |
| Packages | `python3-docker` | Docker SDK required by the Ansible Docker modules |
| Packages | `nginx` | HTTP reverse proxy to the loopback-only application port |
| Packages | `ca-certificates`, `curl` | Trusted TLS certificates and basic diagnostics/image-registry access |
| Packages | `python3`, `python3-apt` | Python runtime and APT support for Ansible modules |
| Users/groups | System group `municipal-portal` | Shared ownership for deployment configuration and directories |
| Users/groups | System user `portal-deploy` | Non-interactive, locked account for prepared deployment paths; not added to the Docker group |
| Users/groups | `ansible` remote account | Existing SSH/sudo account used by Ansible; not managed here |
| Directories | `/opt/municipal-portal` and `/opt/municipal-portal/releases` | Deployment root and release staging |
| Directories | `/var/lib/municipal-portal` | Reserved host state directory; the current MVP does not persist complaints here |
| Directories | `/var/log/municipal-portal` | Reserved for future mounted application logs; current container output remains in Docker logs |
| Directories | `/etc/municipal-portal` | Managed runtime configuration |
| File | `/etc/municipal-portal/portal.env` (root:`municipal-portal`, `0640`) | Non-secret `PORT=8085` setting for a future container run |
| File | `/etc/nginx/conf.d/municipal-complaint-portal.conf` (root, `0644`) | Nginx reverse proxy on the configurable public port |
| Services | `docker.service` | Enabled and started to support Task 14/image deployment |
| Services | `nginx.service` | Enabled and started; reloaded only after a changed proxy config validates |

The application container runs as its own unprivileged UID/GID 10001, as set in
the repository Dockerfile. `portal-deploy` is not granted Docker-socket access:
membership in the `docker` group is effectively root access. An authorized
automation operator should use the normal sudo/become boundary for Docker
operations.

## Network and ports

| Protocol/port | Binding / direction | Use |
| --- | --- | --- |
| TCP 22 | Inbound from the management network | Ansible SSH; restrict to administrator/control-host source addresses |
| TCP 8090 | Inbound from approved clients | Nginx HTTP ingress by default; change `portal_public_port` for another listener |
| TCP 8085 | Host loopback only (`127.0.0.1`) | Portal container backend; do not expose directly to the network |
| TCP 443 | Outbound | Docker Hub/HTTPS and other TLS endpoints |
| TCP 80/443 | Outbound as required by configured APT mirrors | OS package installation |
| UDP/TCP 53 | Outbound to the configured resolver | DNS lookups |

The playbook does not change UFW, nftables, cloud security groups, or other
firewall policy. Permit TCP 8090 only from the intended clients and keep 8085
private. The default Nginx site, if present on a target, is not removed by this
playbook; review existing listeners before production rollout. TLS termination
and certificates are outside this prerequisite baseline.

## Files and execution

| Path | Description |
| --- | --- |
| `ansible/inventory.ini` | Example host/group inventory; replace the reserved address |
| `ansible/site.yml` | Idempotent host-prerequisite playbook |
| `ansible/task14.yml` | Prerequisite provisioning, versioned image deployment, health checks, and rollback |
| `ansible/health_checks.yml` | Direct backend and Nginx ingress probes reused after deploy and recovery |
| `ansible/requirements.yml` | Pinned Ansible collection dependency (`community.docker`) |
| `ansible/TASK14_RUNBOOK.md` | Setup, idempotency, health-check, and rollback procedure |
| `ansible/task14_validation.log` | Local validation transcript and explicit remote-execution status |
| `ansible/templates/municipal-complaint-portal.conf.j2` | Nginx proxy template |
| `ansible/templates/portal.env.j2` | Non-secret application port environment template |
| `ansible/first_execution.log` | First controller-side execution/validation transcript and limitations |

From an Ansible-capable Linux controller, install the collection and first
verify the target, syntax, and proposed changes:

```sh
ansible-galaxy collection install -r ansible/requirements.yml
ansible -i ansible/inventory.ini municipal_portal -m ping
ansible-playbook -i ansible/inventory.ini ansible/task14.yml --syntax-check
ansible-playbook -i ansible/inventory.ini ansible/task14.yml --check --diff \
  -e portal_image=registry.example/municipal-complaint-portal:1.0.0-42-a1b2c3d4e5f6
```

For initial provisioning, deploy a known-good immutable image tag. Rerun the
same command and retain both PLAY RECAPs; the second run should report
`changed=0` when the host, image, and container have not drifted. Then deploy a
different candidate tag with `portal_test_rollback=true` to deliberately reject
the candidate after health checks and prove that the prior image is restored.
That rollback-test command is expected to exit non-zero after the recovery
health checks pass. Details and evidence requirements are in the Task 14
runbook. Use `--ask-become-pass` if the existing SSH account requires a sudo
password.

## Deployment and recovery boundaries

The playbook binds the application port to `127.0.0.1:8085`, passes
`PORT=8085`, and checks both `http://127.0.0.1:8085/api/health` and the
Nginx-facing URL on port 8090 by default. For a private registry, authenticate
the target's Docker client through an approved secret mechanism before running
Ansible; do not put a password in inventory or extra variables. The recovery
handler uses the prior container's image ID so it restores the exact local
image even if a registry tag is later moved.

The playbook requires an Ansible controller with `community.docker` installed,
an existing SSH/sudo account, and a reachable Debian-family target. The
repository inventory still contains the reserved `192.0.2.10` example address;
replace it before execution. No remote node is considered provisioned until a
real successful apply and its captured output are available.

The MVP's complaint store is in memory and is lost on container replacement.
Persistent storage, backups, production TLS, firewall policy, and registry
credential provisioning remain separate operational requirements.
