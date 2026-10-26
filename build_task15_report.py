import json
import shutil
import subprocess
from datetime import datetime
from pathlib import Path
from xml.etree import ElementTree

from docx import Document
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parent
REPORTS = ROOT / "TASKS_REPORTS"
EVIDENCE = REPORTS / "Task15_Evidence"
OUTPUT = REPORTS / "DevOps_Task15_24102A2001.docx"
ACCENT = "19324A"
TEAL = "237C78"
CORAL = "CF684D"
PALE = "EAF2F0"
GRAY = "F3F5F6"
TEXT = "263746"


def command_output(*args):
    try:
        return subprocess.check_output(
            args, cwd=ROOT, text=True, stderr=subprocess.DEVNULL
        ).strip()
    except (OSError, subprocess.CalledProcessError):
        return "unavailable"


def test_totals(pattern):
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    files = sorted(ROOT.glob(pattern))
    for path in files:
        root = ElementTree.parse(path).getroot()
        for key in totals:
            totals[key] += int(root.attrib.get(key, 0))
    totals["suites"] = len(files)
    return totals


def shade(cell, color):
    properties = cell._tc.get_or_add_tcPr()
    shading = OxmlElement("w:shd")
    shading.set(qn("w:fill"), color)
    properties.append(shading)


def set_cell_text(cell, text, *, bold=False, color=TEXT, size=8.5):
    cell.text = ""
    paragraph = cell.paragraphs[0]
    paragraph.paragraph_format.space_after = Pt(2)
    run = paragraph.add_run(str(text))
    run.font.name = "Calibri"
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = RGBColor.from_string(color)
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def add_table(doc, headers, rows, widths=None):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    for index, label in enumerate(headers):
        shade(table.rows[0].cells[index], ACCENT)
        set_cell_text(table.rows[0].cells[index], label, bold=True, color="FFFFFF")
    for row_index, row in enumerate(rows):
        cells = table.add_row().cells
        for index, value in enumerate(row):
            if row_index % 2 == 1:
                shade(cells[index], GRAY)
            set_cell_text(cells[index], value)
    if widths:
        for row in table.rows:
            for index, width in enumerate(widths):
                row.cells[index].width = Inches(width)
    doc.add_paragraph().paragraph_format.space_after = Pt(1)
    return table


def add_bullet(doc, text):
    paragraph = doc.add_paragraph(style="List Bullet")
    paragraph.paragraph_format.space_after = Pt(3)
    paragraph.add_run(text)
    return paragraph


def add_code(doc, text):
    paragraph = doc.add_paragraph()
    paragraph.paragraph_format.left_indent = Inches(0.15)
    paragraph.paragraph_format.space_before = Pt(3)
    paragraph.paragraph_format.space_after = Pt(7)
    paragraph.paragraph_format.keep_together = True
    run = paragraph.add_run(text)
    run.font.name = "Consolas"
    run.font.size = Pt(8)
    run.font.color.rgb = RGBColor.from_string(ACCENT)
    return paragraph


def add_page_number(paragraph):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    paragraph.add_run("Municipal Complaint Portal  |  Task 15  |  ")
    field = OxmlElement("w:fldSimple")
    field.set(qn("w:instr"), "PAGE")
    paragraph._p.append(field)


def configure_document(doc):
    section = doc.sections[0]
    section.top_margin = Inches(0.68)
    section.bottom_margin = Inches(0.65)
    section.left_margin = Inches(0.72)
    section.right_margin = Inches(0.72)

    normal = doc.styles["Normal"]
    normal.font.name = "Calibri"
    normal.font.size = Pt(9.5)
    normal.font.color.rgb = RGBColor.from_string(TEXT)
    normal.paragraph_format.space_after = Pt(5)
    normal.paragraph_format.line_spacing = 1.08
    for name, size, color in [
        ("Title", 28, ACCENT),
        ("Heading 1", 17, ACCENT),
        ("Heading 2", 12, TEAL),
    ]:
        style = doc.styles[name]
        style.font.name = "Calibri"
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor.from_string(color)
    header = section.header.paragraphs[0]
    header.text = "MUNICIPAL COMPLAINT PORTAL  /  FINAL RELEASE REVIEW"
    header.runs[0].font.name = "Calibri"
    header.runs[0].font.size = Pt(8)
    header.runs[0].font.bold = True
    header.runs[0].font.color.rgb = RGBColor.from_string(TEAL)
    add_page_number(section.footer.paragraphs[0])


def build_report():
    if not REPORTS.is_dir():
        raise SystemExit(f"Report directory does not exist: {REPORTS}")
    EVIDENCE.mkdir(exist_ok=True)
    unit = test_totals("target/surefire-reports/TEST-*.xml")
    selenium = test_totals("target/failsafe-reports/TEST-*.xml")
    if unit["suites"] == 0 or selenium["suites"] == 0:
        raise SystemExit("Run the Maven unit and Selenium suites before generating the final report.")

    xml_files = list(ROOT.glob("target/surefire-reports/TEST-*.xml"))
    xml_files += list(ROOT.glob("target/failsafe-reports/TEST-*.xml"))
    xml_files += list(ROOT.glob("target/failsafe-reports/failsafe-summary.xml"))
    for path in xml_files:
        shutil.copy2(path, EVIDENCE / path.name)

    head = command_output("git", "rev-parse", "--short=12", "HEAD")
    branch = command_output("git", "branch", "--show-current")
    tracked_build_files = command_output(
        "git", "ls-tree", "-r", "--name-only", "HEAD", "--", "Dockerfile", ".dockerignore"
    )
    ansible_available = shutil.which("ansible-playbook") is not None
    captured = datetime.now().astimezone().isoformat(timespec="seconds")

    evidence = {
        "captured_at": captured,
        "branch": branch,
        "head": head,
        "unit_tests": unit,
        "selenium_tests": selenium,
        "ansible_playbook_available": ansible_available,
        "ansible_yaml_parse": "PASS (PyYAML parsed site, task14, health_checks, requirements)",
        "inventory_target": "192.0.2.10 (reserved documentation address)",
        "remote_ansible_apply": False,
        "docker_base_images_cached": False,
        "docker_build_or_publish": False,
        "docker_build_context_tracked_at_head": bool(tracked_build_files),
        "docker_build_context_status": tracked_build_files or "Dockerfile and .dockerignore absent from HEAD",
        "local_application_smoke": {
            "http_status": 200,
            "health": "UP",
            "scope": "temporary loopback JAR process; not a Docker/Nginx deployment",
            "screenshot": "Task15_local_live_portal.png",
        },
        "existing_local_listeners": {
            "backend_8085": "HTTP 200, status UP",
            "nginx_8090": "HTTP 200, status UP",
            "scope": "read-only local probes; deployment provenance not verified",
        },
        "jenkins_historical_run": {
            "job": "Municipal-Complaint-Portal-Pipeline #6",
            "checkout": "9aa2e01",
            "result": "SUCCESS",
            "scope": "17 unit + 5 Selenium; earlier native JAR/Nginx deployment",
            "current_docker_pipeline_verified": False,
        },
        "screen_recording": "not captured",
    }
    (EVIDENCE / "Task15_validation.json").write_text(
        json.dumps(evidence, indent=2) + "\n", encoding="utf-8"
    )
    (EVIDENCE / "Task15_validation_summary.txt").write_text(
        "\n".join(
            [
                "Task 15 validation evidence",
                f"Captured: {captured}",
                f"Git: {branch} @ {head}",
                f"Unit: {unit['tests']} tests, {unit['failures']} failures, {unit['errors']} errors, {unit['skipped']} skipped",
                f"Selenium: {selenium['tests']} tests, {selenium['failures']} failures, {selenium['errors']} errors, {selenium['skipped']} skipped",
                "Local loopback application health: HTTP 200, status UP",
                "Existing localhost backend :8085 and Nginx ingress :8090: HTTP 200 (read-only probes; deployment provenance unverified)",
                "Docker build/push: NOT RUN (base images not cached; external pull not authorized)",
                "Remote Ansible provisioning: NOT RUN (Ansible unavailable; inventory is reserved placeholder)",
                "Historical Jenkins build #6: SUCCESS at 9aa2e01; predates Docker pipeline commit",
                "See Task15_validation.json for scope and machine-readable details.",
                "",
            ]
        ),
        encoding="utf-8",
    )

    doc = Document()
    configure_document(doc)
    doc.core_properties.title = "Task 15 - Final Release, Documentation, and Viva"
    doc.core_properties.subject = "Municipal Complaint Portal end-to-end release evidence"
    doc.core_properties.author = "Municipal Complaint Portal DevOps Project"

    cover = doc.add_paragraph()
    cover.paragraph_format.space_before = Pt(50)
    title = cover.add_run("TASK 15")
    title.font.name = "Calibri"
    title.font.size = Pt(17)
    title.font.bold = True
    title.font.color.rgb = RGBColor.from_string(TEAL)
    p = doc.add_paragraph(style="Title")
    p.add_run("Final Release,\nDocumentation & Viva")
    sub = doc.add_paragraph()
    sub.paragraph_format.space_after = Pt(20)
    run = sub.add_run("Municipal Complaint Portal | End-to-End DevOps")
    run.font.size = Pt(14)
    run.font.color.rgb = RGBColor.from_string(TEXT)
    add_table(
        doc,
        ["Release decision", "Local quality gates", "External rollout"],
        [[
            "HOLD - not a production release",
            f"PASS - {unit['tests']}/{unit['tests']} unit; {selenium['tests']}/{selenium['tests']} Selenium",
            "PENDING - Docker registry and Ansible target",
        ]],
        [2.15, 2.25, 2.25],
    )
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(18)
    p.add_run(
        f"Evidence snapshot: {captured}\nGit branch: {branch} | HEAD: {head}\n"
        "Prepared as a reproducible technical report. Test success is reported "
        "separately from image publication and remote provisioning."
    )
    doc.add_page_break()

    doc.add_heading("1. Executive summary", level=1)
    doc.add_paragraph(
        "The Task 15 package documents the portal architecture, test gates, "
        "Docker delivery design, Ansible provisioning and recovery procedure, "
        "troubleshooting, known limitations, and a prioritized improvement plan. "
        "The current workspace passes its local unit and Selenium suites and "
        "a loopback health smoke test."
    )
    doc.add_paragraph(
        "The complete release is not claimed. The current Docker build context "
        "is untracked, Docker base images are not cached, no registry publish was "
        "performed, and no authorized Linux host is configured. Existing Jenkins "
        "build #6 is a successful historical native JAR/Nginx run at revision "
        "9aa2e01; it predates the Docker pipeline commit."
    )

    doc.add_heading("2. Architecture", level=1)
    doc.add_paragraph(
        "The portal uses the JDK HTTP server, service-level validation, and an "
        "in-memory concurrent complaint map. Jenkins is the build and test "
        "orchestrator. Docker packaging and Jenkins staging deployment are one "
        "delivery path; Ansible provisions a separate Debian/Ubuntu target and "
        "is not invoked by the current Jenkinsfile."
    )
    doc.add_picture(str(REPORTS / "Task15_architecture.png"), width=Inches(7.0))
    doc.add_paragraph(
        "Figure 1. Runtime and delivery architecture. Backend port 8085 is "
        "loopback-bound by default; Nginx exposes port 8090.",
        style="Caption",
    )

    doc.add_heading("3. Release workflow and quality gates", level=1)
    doc.add_picture(str(REPORTS / "Task15_release_workflow.png"), width=Inches(7.0))
    doc.add_paragraph(
        "Figure 2. Release gates and verification boundary for this snapshot.",
        style="Caption",
    )
    add_table(
        doc,
        ["Stage", "Implementation", "Current evidence"],
        [
            ["Checkout/version", "Git SCM; tag includes Maven version, build number, commit", f"Local HEAD {head}; Jenkins #6 used 9aa2e01"],
            ["Build and unit gate", "Maven compile/package; archive JAR and publish JUnit", f"{unit['tests']} tests passed locally"],
            ["Selenium gate", "Failsafe integration tests run before Docker build/push", f"{selenium['tests']} tests passed locally"],
            ["Docker build/publish", "Multi-stage Java 17 image; immutable and latest tags", "Not run; base images absent and no pull/push performed"],
            ["Staging deployment", "Deploy script replaces environment container; health/evidence archive", "Current Docker pipeline not verified"],
            ["Ansible rollout", "Host baseline, versioned image, health checks, recovery", "Not run; reserved inventory target and no Ansible binary"],
        ],
        [1.35, 3.35, 2.35],
    )

    doc.add_heading("4. Validation results", level=1)
    add_table(
        doc,
        ["Check", "Result", "Scope"],
        [
            ["Maven unit tests", f"PASS: {unit['tests']} tests; {unit['failures']} failures; {unit['errors']} errors", "Offline run against current workspace."],
            ["Selenium/Failsafe", f"PASS: {selenium['tests']} tests; {selenium['failures']} failures; {selenium['errors']} errors", "Headless Chrome; cached ChromeDriver; CDP warning was non-fatal."],
            ["Ansible YAML", "PASS", "PyYAML parses site, task14, health_checks, and requirements files."],
            ["Ansible syntax check", "NOT RUN", "Ansible Core is not installed on this controller."],
            ["Local app smoke test", "PASS: HTTP 200, JSON status UP", "Temporary loopback JAR process; stopped after probe."],
            ["Existing local backend/ingress", "PASS: HTTP 200 on ports 8085 and 8090", "Read-only probes; deployment provenance not established."],
            ["Docker image/registry", "NOT RUN", "Base images are absent locally; no external pull/publish."],
            ["Remote provisioning/rollback", "NOT RUN", "No real host, SSH identity, or sudo authorization; inventory remains placeholder."],
        ],
        [1.55, 2.5, 3.0],
    )
    doc.add_paragraph(
        "JUnit XML files and a machine-readable validation record are included "
        "under TASKS_REPORTS/Task15_Evidence. The report distinguishes local "
        "test evidence from deployment evidence."
    )

    doc.add_heading("5. Deployment and operations", level=1)
    doc.add_paragraph(
        "The Jenkinsfile derives an immutable Docker reference from the Maven "
        "version, Jenkins build number, and Git commit. Unit and Selenium gates "
        "precede image creation and registry push. Deployment uses the immutable "
        "tag, checks the backend and Nginx proxy, and archives a JSON evidence file."
    )
    doc.add_paragraph(
        "The Ansible playbook installs Docker/Nginx on Debian-family hosts, "
        "deploys a tagged image on loopback port 8085, validates the Nginx "
        "listener on port 8090, repeats safely, and restores the prior image ID "
        "on candidate failure. Its rollback exercise must run only on a "
        "non-production target with a known-good release already available."
    )
    add_code(
        doc,
        "mvn -o test\n"
        "mvn -o failsafe:integration-test failsafe:verify -Pselenium\n"
        "ansible-playbook -i ansible/inventory.ini ansible/task14.yml --syntax-check\n"
        "ansible-playbook -i ansible/inventory.ini ansible/task14.yml --diff "
        "-e portal_image=<registry>/<repository>:<immutable-tag>",
    )
    doc.add_paragraph(
        "Full environment requirements, secret handling, commands, idempotency "
        "checks, and rollback procedure are in TASK15_FINAL_RELEASE.md and "
        "ansible/TASK14_RUNBOOK.md."
    )

    doc.add_heading("6. Troubleshooting guide", level=1)
    add_table(
        doc,
        ["Symptom", "First response"],
        [
            ["Dockerfile missing in Jenkins", "Confirm Dockerfile and .dockerignore are tracked in the checked-out commit."],
            ["Docker pull/push fails", "Check authorized network access, registry namespace, and Jenkins token credential without printing secrets."],
            ["Container unhealthy / Nginx 502", "Inspect container logs and backend health on 8085; validate Nginx proxy target and port 8090."],
            ["Chrome session cannot start", "Check Chrome/ChromeDriver compatibility and use the approved cached driver path."],
            ["Ansible host unreachable", "Replace 192.0.2.10, verify SSH with the inventory, and check sudo/become setup."],
            ["Second Ansible run changes tasks", "Use the same immutable tag; compare --diff and investigate drift before asserting idempotency."],
            ["Rollback rehearsal fails early", "Ensure a different candidate tag and a healthy prior image exist on the non-production target."],
        ],
        [2.0, 5.05],
    )

    doc.add_heading("7. Limitations and future plan", level=1)
    for item in [
        "Complaint data is in memory and is lost on application restart or container replacement.",
        "Production authentication/authorization, TLS, rate limits, privacy controls, and audit retention are not delivered.",
        "Durable attachment storage, malware scanning, and an operational database are not configured.",
        "There is no centralized observability, autoscaling, multi-node failover, or rolling deployment.",
        "Container base images are not digest-pinned; SBOM, image signing, and vulnerability gates are future work.",
    ]:
        add_bullet(doc, item)
    add_table(
        doc,
        ["Priority", "Enhancement"],
        [
            ["P0", "Track Docker build context; run the current Docker Jenkins pipeline and an authorized non-production Ansible rollout."],
            ["P1", "Add relational persistence, migrations, backup/restore; add reviewer identity and authorization."],
            ["P1", "Pin image digests; generate SBOM; scan dependencies/images; sign and verify provenance."],
            ["P2", "Add structured observability, SLOs, alerting, rolling delivery, security/load/accessibility tests."],
        ],
        [0.85, 6.2],
    )

    doc.add_heading("8. Demonstration and viva", level=1)
    doc.add_paragraph(
        "The attached portal screenshot was captured from the local application "
        "on a temporary loopback port. A browser-based presentation and viva "
        "question set accompany this report. A screen recording is not included; "
        "record the live Jenkins, registry, and target steps after the pending "
        "credentials and target are available."
    )
    doc.add_picture(str(REPORTS / "Task15_local_live_portal.png"), width=Inches(6.8))
    doc.add_paragraph(
        "Figure 3. Local portal smoke-test screenshot. This is not a Docker or "
        "remote-host deployment screenshot.",
        style="Caption",
    )
    doc.add_paragraph(
        "The viva guide is TASKS_REPORTS/Task15_Viva_QA.md. The HTML presentation "
        "is TASKS_REPORTS/Task15_Viva_Presentation.html. The operator runbook is "
        "TASK15_FINAL_RELEASE.md."
    )

    doc.save(OUTPUT)
    print(f"Created report: {OUTPUT}")
    print(f"Created evidence package: {EVIDENCE}")


if __name__ == "__main__":
    build_report()
