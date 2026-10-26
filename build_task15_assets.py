from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch


ROOT = Path(__file__).resolve().parent
REPORTS = ROOT / "TASKS_REPORTS"
NAVY = "#19324A"
TEAL = "#237C78"
CORAL = "#CF684D"
GOLD = "#C18B34"
MINT = "#E8F2EF"
PAPER = "#F6F4EF"
INK = "#203342"
MUTED = "#62727E"
WHITE = "#FFFFFF"


def rounded_box(ax, x, y, width, height, title, body, accent, title_size=12):
    patch = FancyBboxPatch(
        (x, y),
        width,
        height,
        boxstyle="round,pad=0.45,rounding_size=1.4",
        linewidth=1.5,
        edgecolor=accent,
        facecolor=WHITE,
    )
    ax.add_patch(patch)
    ax.text(
        x + width / 2,
        y + height - 1.6,
        title,
        ha="center",
        va="top",
        fontsize=title_size,
        fontweight="bold",
        color=accent,
    )
    ax.text(
        x + width / 2,
        y + height / 2 - 1,
        body,
        ha="center",
        va="center",
        fontsize=9.2,
        color=INK,
        linespacing=1.35,
    )


def arrow(ax, start, end, color=TEAL, style="-"):
    ax.annotate(
        "",
        xy=end,
        xytext=start,
        arrowprops={
            "arrowstyle": "-|>",
            "color": color,
            "lw": 1.8,
            "linestyle": style,
            "shrinkA": 4,
            "shrinkB": 4,
        },
    )


def build_architecture():
    fig, ax = plt.subplots(figsize=(15, 9), facecolor=PAPER)
    ax.set_facecolor(PAPER)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis("off")
    ax.text(
        3,
        96,
        "MUNICIPAL COMPLAINT PORTAL",
        fontsize=22,
        fontweight="bold",
        color=NAVY,
        va="top",
    )
    ax.text(
        3,
        91,
        "Application runtime and release architecture",
        fontsize=12,
        color=MUTED,
        va="top",
    )

    ax.text(3, 83, "APPLICATION", fontsize=10, fontweight="bold", color=TEAL)
    app_boxes = [
        (3, "Citizen / reviewer", "HTML, CSS and\nvanilla JavaScript", TEAL),
        (27, "Java HTTP server", "JDK HttpServer\nREST endpoints", TEAL),
        (51, "Service layer", "ComplaintService\nValidationService", TEAL),
        (75, "Complaint store", "ConcurrentHashMap\nin-memory only", CORAL),
    ]
    for x, title, body, color in app_boxes:
        rounded_box(ax, x, 66, 20, 13, title, body, color)
    for x in (23, 47, 71):
        arrow(ax, (x, 72.5), (x + 4, 72.5))

    ax.text(3, 57, "BUILD AND QUALITY GATES", fontsize=10, fontweight="bold", color=TEAL)
    build_boxes = [
        (3, "Git", "dev branch\ncommit", NAVY),
        (27, "Jenkins", "Pipeline as code\nWindows agent", NAVY),
        (51, "Maven + Selenium", "17 unit tests\n5 browser journeys", GOLD),
        (75, "Docker image", "Java 17 runtime\nUID/GID 10001", TEAL),
    ]
    for x, title, body, color in build_boxes:
        rounded_box(ax, x, 40, 20, 13, title, body, color)
    for x in (23, 47, 71):
        arrow(ax, (x, 46.5), (x + 4, 46.5))

    ax.text(3, 33, "DELIVERY TARGETS", fontsize=10, fontweight="bold", color=TEAL)
    rounded_box(
        ax,
        3,
        12,
        42,
        17,
        "Jenkins staging deployment",
        "Docker Hub immutable tag -> Docker container\nNginx ingress :8090 -> app loopback :8085",
        TEAL,
        title_size=11,
    )
    rounded_box(
        ax,
        55,
        12,
        42,
        17,
        "Ansible Linux deployment",
        "Debian / Ubuntu host baseline -> Docker + Nginx\nhealth checks, repeatable apply and rollback",
        CORAL,
        title_size=11,
    )
    arrow(ax, (80, 40), (36, 29), color=TEAL)
    arrow(ax, (85, 40), (76, 29), color=CORAL, style="--")
    ax.text(
        50,
        5,
        "The Ansible rollout is a separate operator-run workflow; Jenkins does not invoke it.",
        ha="center",
        fontsize=9,
        color=MUTED,
    )
    fig.tight_layout(pad=1.2)
    fig.savefig(REPORTS / "Task15_architecture.png", dpi=180, bbox_inches="tight")
    plt.close(fig)


def build_workflow():
    fig, ax = plt.subplots(figsize=(16, 6.8), facecolor=PAPER)
    ax.set_facecolor(PAPER)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 70)
    ax.axis("off")
    ax.text(
        3,
        67,
        "RELEASE PATH | IMPLEMENTED PIPELINE, HONEST EVIDENCE",
        fontsize=18,
        fontweight="bold",
        color=NAVY,
        va="top",
    )

    steps = [
        ("01  SOURCE", "Git commit\ncurrent HEAD", NAVY, "Repository state"),
        ("02  BUILD", "Jenkins + Maven\ncompile / package", NAVY, "Pipeline defined"),
        ("03  TEST", "JUnit 17/17\nSelenium 5/5", TEAL, "PASS locally"),
        ("04  IMAGE", "Docker build\nregistry push", GOLD, "Not run here"),
        ("05  STAGING", "Versioned image\nDocker + Nginx", GOLD, "Not run here"),
        ("06  PROVISION", "Ansible host\nand rollout", CORAL, "Target missing"),
        ("07  VERIFY", "Health / rerun /\nrollback evidence", CORAL, "Pending target"),
    ]
    left, width, gap = 2, 12.4, 1.8
    for index, (title, body, color, status) in enumerate(steps):
        x = left + index * (width + gap)
        rounded_box(ax, x, 33, width, 20, title, body, color, title_size=9.5)
        ax.text(
            x + width / 2,
            27,
            status,
            ha="center",
            va="center",
            fontsize=8.2,
            fontweight="bold",
            color=color,
        )
        if index < len(steps) - 1:
            arrow(ax, (x + width, 43), (x + width + gap, 43), color=MUTED)

    ax.text(
        3,
        17,
        "Evidence boundary",
        fontsize=11,
        fontweight="bold",
        color=NAVY,
    )
    ax.text(
        3,
        11,
        "A prior Jenkins #6 passed unit + Selenium and native Nginx deployment at revision 9aa2e01.",
        fontsize=9.5,
        color=INK,
    )
    ax.text(
        3,
        6,
        "It predates the Docker pipeline commit; no Docker push, current-pipeline deployment, or remote Ansible apply is claimed.",
        fontsize=9.5,
        color=INK,
    )
    fig.tight_layout(pad=1.1)
    fig.savefig(REPORTS / "Task15_release_workflow.png", dpi=180, bbox_inches="tight")
    plt.close(fig)


if __name__ == "__main__":
    if not REPORTS.is_dir():
        raise SystemExit(f"Report folder does not exist: {REPORTS}")
    build_architecture()
    build_workflow()
    print("Created Task 15 architecture and workflow diagrams.")
