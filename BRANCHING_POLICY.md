# Branching Policy & Git Collaboration Guidelines

## 1. Branch Strategy (GitFlow Model)

The Municipal Complaint Portal follows a structured GitFlow branching strategy:

- **`main` (Production Branch):**
  - Contains strictly production-ready, release-tagged code (e.g., `v1.0`).
  - Protected branch; direct commits are prohibited.
  - Merges into `main` require a reviewed Pull Request (PR) from `dev` and a 100% passing CI build.

- **`dev` (Development Integration Branch):**
  - Serves as the primary integration branch for ongoing sprint features.
  - Aggregates validated feature branches before release tagging.

- **`feature/<name>` (Feature Branches):**
  - Branched off `dev`.
  - Used for developing specific user stories (e.g., `feature/complaint-submission`, `feature/reviewer-triage`).
  - Merged back into `dev` via approved Pull Request.

- **`bugfix/<name>` (Bugfix Branches):**
  - Branched off `dev` to resolve defects discovered during sprint testing.

- **`hotfix/<name>` (Emergency Patch Branches):**
  - Branched off `main` to address critical production regressions, merged into both `main` and `dev`.

---

## 2. Branch Naming Rules

All branches must adhere to lowercase hyphenated nomenclature:
- `feature/<short-description>`: e.g., `feature/complaint-submission`
- `bugfix/<ticket-or-name>`: e.g., `bugfix/validation-error-handling`
- `hotfix/<patch-version>`: e.g., `hotfix/security-header-fix`
- `release/<version>`: e.g., `release/v1.0.0`

---

## 3. Semantic Commit Guidelines (Conventional Commits)

Commit messages must follow the standard format:
`<type>(<scope>): <short description>`

### Types:
- `feat`: A new feature (e.g., `feat(intake): add complaint submission form with validation`)
- `fix`: A bug fix (e.g., `fix(validation): handle null contact numbers gracefully`)
- `docs`: Documentation updates (e.g., `docs: update README with API specifications`)
- `test`: Adding or updating test cases (e.g., `test(service): add unit tests for token generation`)
- `ci`: CI/CD pipeline modifications (e.g., `ci(jenkins): configure declarative pipeline stage`)
- `chore`: Maintenance tasks (e.g., `chore: update maven compiler plugin configuration`)

---

## 4. Pull Request (PR) Lifecycle & Quality Gates

1. Developer branches off `dev`, implements code, and verifies `mvn test` locally.
2. Push branch to GitHub: `git push -u origin feature/<name>`
3. Open a Pull Request targeting `dev` with a clear description and checklist.
4. Peer code review required (minimum 1 approval).
5. Automated Jenkins CI pipeline executes build and test quality gates.
6. Merge using "Squash and Merge" or "Merge Commit" once all checks pass.
