const stages = [
  { title: 'Checkout', tool: 'Git / GitHub', icon: 'git', input: 'Version-controlled source', output: 'Checked-out commit', command: 'checkout scm\ngit log -n 1 --oneline', description: 'Jenkins checks out the configured source revision. The commit is the starting point for tracing code, tests, an image and its deployment.', gate: 'An unavailable repository or failed checkout stops the run.', evidence: 'Source definition in Jenkinsfile. Historical build #6 checked out 9aa2e01.', tasks: 'Tasks 4–8' },
  { title: 'Prepare version', tool: 'Jenkins / Maven metadata', icon: 'layers', input: 'POM version + commit + build', output: 'Unique image reference', command: '<pom-version>-<build-number>-<12-character-commit>', description: 'The pipeline validates its repository and port parameters, then creates a build-specific image tag. The versioned tag is deployed; latest is only a convenience tag.', gate: 'Invalid parameters or missing version metadata stop delivery.', evidence: 'Implemented in the current Jenkinsfile. Current Docker delivery execution is pending.', tasks: 'Task 12' },
  { title: 'Compile', tool: 'Java 17 / Maven', icon: 'code', input: 'Java source and resources', output: 'Compiled classes', command: 'mvn clean compile', description: 'Maven compiles the Java application. The same build now packages the dashboard, offline diagrams and curated reports as classpath resources.', gate: 'Compilation must succeed before the test and delivery stages.', evidence: 'Build stage defined in Jenkinsfile. The website is not a live Jenkins build monitor.', tasks: 'Tasks 7–8' },
  { title: 'Package & test', tool: 'JUnit 5 / Surefire', icon: 'shield', input: 'Compiled application', output: 'Test XML + executable JAR', command: 'mvn test package', description: 'Fast automated tests check service behavior, validation and reviewer rules. Maven creates the executable JAR and Jenkins archives the artifact.', gate: 'A failing unit test prevents subsequent delivery stages.', evidence: 'Recorded baseline: 17/17 unit tests on 20 Oct 2026 at abc900735330. This is not the current build result.', tasks: 'Tasks 7–10' },
  { title: 'Browser tests', tool: 'Selenium / Failsafe', icon: 'checkCircle', input: 'Running isolated test portal', output: 'Journey results + failure captures', command: 'mvn failsafe:integration-test failsafe:verify -Pselenium', description: 'Headless Chrome drives critical user journeys. Explicit waits reduce timing issues; failed journeys can capture screenshots for diagnosis.', gate: 'Browser test failure blocks Docker build, publish and deploy.', evidence: 'Recorded baseline: 5/5 journeys on 20 Oct 2026. Jenkins #5 demonstrated a historical failing quality gate.', tasks: 'Tasks 9–10' },
  { title: 'Build image', tool: 'Docker', icon: 'box', input: 'Dockerfile + source context', output: 'Versioned container image', command: 'docker build --pull --tag "%IMAGE_REF%" --tag "%LATEST_REF%" .', description: 'A multi-stage build compiles with Maven and copies the JAR into a smaller Java runtime image. The runtime uses an unprivileged application user.', gate: 'The image is built only after both test gates pass.', evidence: 'Dockerfile exists. Task 11 runtime lifecycle and the current image build were not verified in the recorded snapshot.', tasks: 'Tasks 11–12' },
  { title: 'Publish image', tool: 'Docker registry', icon: 'cloud', input: 'Tested, tagged image', output: 'Published image and digest', command: 'docker push "%IMAGE_REF%"\ndocker push "%LATEST_REF%"', description: 'Jenkins authenticates with its managed credential and publishes the image. Credentials are not embedded in this website or report viewer.', gate: 'Registry authentication or push failure stops deployment.', evidence: 'Implemented pipeline stage. Registry publication and its digest remain unverified in the recorded evidence.', tasks: 'Task 12' },
  { title: 'Deploy & verify', tool: 'Docker / Nginx', icon: 'server', input: 'Exact published image tag', output: 'Container + health evidence', command: 'deploy/deploy.ps1 -ContainerImage <versioned-image>\nGET /api/health', description: 'The deployment script pulls the versioned tag, replaces the environment container and checks the backend and reverse proxy. It records deployment evidence.', gate: 'Deployment and health verification must succeed. A current healthy portal does not prove that this pipeline ran.', evidence: 'Historical #6 used native JAR/Nginx deployment, not the current Docker pipeline. Current Docker deployment is pending.', tasks: 'Tasks 8, 12, 15' },
];

const recoveryStages = [
  { title: 'Configure host', tool: 'Ansible / APT', icon: 'server', input: 'Approved inventory + Linux host', output: 'Desired prerequisites', command: 'ansible-playbook -i ansible/inventory.ini ansible/task14.yml', description: 'The operator runs a separate Ansible workflow. It installs Docker/Nginx prerequisites and configures a locked deployment identity and required directories.', gate: 'A real authorized host, Ansible controller and SSH/sudo access are required.', evidence: 'Playbooks exist; remote provisioning is not verified. Jenkins does not invoke this playbook.', tasks: 'Tasks 13–14' },
  { title: 'Record stable', tool: 'Ansible / Docker', icon: 'layers', input: 'Existing running container', output: 'Previous immutable image ID', command: 'community.docker.docker_container_info\nrecord previous image ID', description: 'Before changing a running release, the playbook captures the previous image reference and immutable image ID. A first release may have no stable predecessor.', gate: 'Rollback rehearsal requires a different existing stable release.', evidence: 'Implemented in ansible/task14.yml; remote execution evidence is pending.', tasks: 'Task 14' },
  { title: 'Run candidate', tool: 'Ansible / Docker', icon: 'box', input: 'Versioned candidate image', output: 'Candidate container', command: 'community.docker.docker_image\ncommunity.docker.docker_container', description: 'The candidate image is pulled before modifying the current release. The container receives its configured ports, restart policy and environment.', gate: 'latest is rejected; a versioned tag is required.', evidence: 'Implemented. No candidate was rolled out to a remote target in the recorded snapshot.', tasks: 'Task 14' },
  { title: 'Check health', tool: 'HTTP / Nginx', icon: 'health', input: 'Candidate endpoints', output: 'Accepted or rejected candidate', command: 'GET backend /api/health\nGET Nginx /api/health', description: 'Direct and proxied health checks validate the candidate. This illustrative scenario deliberately rejects the candidate to explain recovery.', gate: 'A rejected candidate enters the rescue path.', evidence: 'Remote health and recovery checks remain pending. This animation makes no network deployment calls.', tasks: 'Task 14' },
  { title: 'Restore stable', tool: 'Ansible rescue', icon: 'refresh', input: 'Recorded previous image ID', output: 'Restored stable service', command: 'rescue:\n  restore previous immutable image ID\n  verify health\n  fail rejected deployment', description: 'The previous immutable image is restored and checked. The deployment still exits unsuccessfully so recovery cannot make a rejected release look successful.', gate: 'Without a prior release, remove the rejected candidate instead of claiming recovery.', evidence: 'Implemented rescue logic. Target-side rollback has not been demonstrated in the recorded evidence.', tasks: 'Tasks 14–15' },
];

const taskData = [
  ['Problem definition & scope', 'Foundation', 'Identify civic pain points, stakeholders, constraints and measurable MVP objectives.', 'TASKS.md · Task 1', 'Problem statement, stakeholder map and approved scope.'],
  ['Agile planning & DevOps workflow', 'Foundation', 'Connect user stories, acceptance criteria and the backlog to a continuous delivery lifecycle.', 'BACKLOG.md · TASKS.md', 'User stories, Kanban/Scrum plan, Definition of Done and workflow diagram.'],
  ['Requirements & architecture', 'Foundation', 'Define the application layers, data model, API boundaries and local Java/Maven setup.', 'src/main/java/com/municipal/portal/App.java', 'SRS, use cases, architecture, API list and working local setup.'],
  ['Git & GitHub initialization', 'Collaboration', 'Establish version control, issue templates, repository structure and a branch policy.', 'BRANCHING_POLICY.md · .github/ISSUE_TEMPLATE', 'Repository baseline, semantic commits and collaboration conventions.'],
  ['Feature branching', 'Collaboration', 'Implement complaint registration on an isolated branch and explain review and merge traceability.', 'ComplaintRegistrationWorkflow.java', 'First workflow, feature branch, review records and merge evidence.'],
  ['MVP & Git collaboration', 'Collaboration', 'Complete reviewer behavior, resolve a merge conflict and establish a tagged source baseline.', 'ReviewerActionService.java · ValidationService.java', 'Functional MVP, conflict resolution, release tag and updated backlog.'],
  ['Jenkins continuous integration', 'CI & quality', 'Automate source checkout, Maven builds, unit-test gates and artifact archiving.', 'Jenkinsfile · README.md', 'Configured CI job, trigger evidence, historical build logs and JAR artifact.'],
  ['Pipeline as code & deployment', 'CI & quality', 'Define repeatable stages and parameterized Java/Nginx deployment in version control.', 'Jenkinsfile · deploy/deploy.ps1', 'Declarative pipeline, environment parameters and historical deployment evidence.'],
  ['Selenium journey testing', 'CI & quality', 'Drive critical browser journeys with assertions, explicit waits and failure screenshots.', 'SeleniumComplaintJourneyIT.java', 'Journey suite, local browser reports and diagnostic screenshots.'],
  ['Continuous testing & quality gates', 'CI & quality', 'Show how a deliberate defect fails tests and prevents deployment, then verify the correction.', 'Jenkinsfile · SeleniumComplaintJourneyIT.java', 'Historical failed Jenkins #5 and corrected successful #6; revision context matters.'],
  ['Docker image & container lifecycle', 'Container delivery', 'Explain a multi-stage image, non-root runtime, port mapping and the container lifecycle.', 'Dockerfile · .dockerignore', 'Image definition and lifecycle procedure exist; build/run lifecycle evidence is pending.'],
  ['Jenkins–Docker delivery', 'Container delivery', 'Version images, publish through managed credentials and deploy the exact tested build.', 'Jenkinsfile · deploy/deploy.ps1', 'Implementation exists; registry push, digest and current pipeline rollout remain pending.'],
  ['Ansible configuration management', 'Infrastructure', 'Describe server prerequisites, restricted identities, directories, packages and services as code.', 'ansible/site.yml · ansible/CONFIGURATION_SPEC.md', 'Playbook and local checks exist; a successful remote apply is not recorded.'],
  ['Provisioning & reliability', 'Infrastructure', 'Explain idempotency, candidate health validation and previous-image recovery.', 'ansible/task14.yml · ansible/health_checks.yml', 'Remote apply, changed=0 rerun and rollback evidence remain pending.'],
  ['Final release, evidence & viva', 'Release', 'Connect the architecture, quality gates, evidence boundaries, limitations and demonstration.', 'TASK15_FINAL_RELEASE.md · Task15_Evidence', 'Recorded local gates pass; the documented release decision is HOLD, not production release.'],
];

export function renderLab(container, { icon, escapeHtml: e, heading }) {
  let tab = 'pipeline';
  let selected = 0;
  let progress = -1;
  let scenario = 'success';
  let timer = null;
  let running = false;
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const list = () => scenario === 'recovery' ? recoveryStages : stages;
  const stop = () => { clearInterval(timer); timer = null; running = false; };

  function stageClass(index) {
    if (scenario === 'failure' && progress >= 4) return index === 4 ? 'failed' : index > 4 ? 'skipped' : 'complete';
    if (scenario === 'recovery' && progress >= 3 && index === 3) return 'failed';
    return index <= progress ? 'complete' : '';
  }

  function simulationMessage() {
    if (progress < 0) return 'Ready to explore. Select a stage for details, or step through an illustrative scenario.';
    if (scenario === 'failure' && progress >= 4) return 'Illustration: browser tests failed. Docker build, publish and deployment are skipped.';
    if (scenario === 'recovery' && progress === 4) return 'Illustration: stable image restored; the rejected deployment still reports failure.';
    if (progress === list().length - 1) return 'Illustration complete. No build, registry push or deployment was executed.';
    return `Illustrative step ${progress + 1} of ${list().length}: ${list()[progress].title}. No commands are being executed.`;
  }

  function stageDetail() {
    const stage = list()[selected];
    return `<div class="lab-detail-grid"><section class="card stage-detail"><div class="eyebrow">STAGE ${String(selected + 1).padStart(2, '0')} · ${stage.tasks}</div><h3>${stage.title}</h3><p>${stage.description}</p><dl class="stage-facts"><div><dt>TOOL</dt><dd>${stage.tool}</dd></div><div><dt>INPUT</dt><dd>${stage.input}</dd></div><div><dt>OUTPUT</dt><dd>${stage.output}</dd></div><div><dt>EVIDENCE TYPE</dt><dd>Source + recorded snapshot</dd></div></dl><pre>${e(stage.command)}</pre><div class="callout"><strong>Gate:</strong> ${stage.gate}</div><div class="notice amber">${icon('info')}<span>${stage.evidence}</span></div></section><section class="card"><div class="card-header"><div><h2>Understand the why</h2><p>Concepts, connected to this project</p></div>${icon('book')}</div><div class="card-body concept-list">${[
      ['CI versus CD', 'CI checks changes through automated builds and tests. Delivery/deployment continues with versioned images and environments. A defined stage is not proof that it has run.'],
      ['What is a quality gate?', 'A condition that must pass before promotion. Here, JUnit and Selenium failures prevent downstream Docker delivery. The historical defect exercise is documented in Task 10.'],
      ['Why an immutable tag?', 'The Maven version, Jenkins build number and commit identify a specific artifact. Deployment uses that versioned tag, not the mutable latest reference.'],
      ['What does idempotency mean?', 'Reapplying the same desired configuration should not keep changing a correct host. Ansible needs a real second-run PLAY RECAP to substantiate changed=0.'],
      ['Health is not observability', 'The local health endpoint checks that this portal responds. It does not measure historical uptime, monitor every dependency, or validate remote infrastructure.'],
      ['Recovery is not release success', 'A rollback can restore service while the candidate release still fails. The Ansible rescue path intentionally preserves that failure signal.'],
    ].map(([title, description], i) => `<details${i === 0 ? ' open' : ''}><summary>${title}</summary><p>${description}</p></details>`).join('')}</div></section></div>`;
  }

  function pipelineView() {
    const activeStages = list();
    return `<div class="notice">${icon('info')}<span><strong>Illustrative simulation, not live execution.</strong> The walkthrough runs entirely in your browser. No Jenkins, Docker, registry or Ansible commands are triggered.</span></div><section class="card"><div class="simulation-header"><div><h2>${scenario === 'recovery' ? 'Separate Ansible recovery path' : 'The commit-to-container journey'}</h2><p>${scenario === 'recovery' ? 'Operator-controlled Linux workflow · not invoked by Jenkins' : 'Aligned with the current Jenkinsfile · select any stage'}</p></div><div class="simulation-controls"><label class="sr-only" for="scenario">Illustrative scenario</label><select id="scenario" class="control-select"><option value="success"${scenario === 'success' ? ' selected' : ''}>Successful release</option><option value="failure"${scenario === 'failure' ? ' selected' : ''}>Failed quality gate</option><option value="recovery"${scenario === 'recovery' ? ' selected' : ''}>Ansible candidate recovery</option></select><button class="button small primary" data-lab="play">${icon(running ? 'pause' : 'play')}${running ? 'Pause' : 'Play'}</button><button class="button small" data-lab="next">${icon('next')}Step</button><button class="button small" data-lab="reset" aria-label="Restart walkthrough">${icon('refresh')}</button></div></div><div class="pipeline-scroll"><div class="pipeline${scenario === 'recovery' ? ' recovery' : ''}">${activeStages.map((stage, i) => `<button class="pipeline-node ${selected === i ? 'selected' : ''} ${stageClass(i)}" data-stage="${i}" aria-pressed="${selected === i}" aria-label="Stage ${i + 1}: ${stage.title}"><span class="node-top"><span>${String(i + 1).padStart(2, '0')}</span>${icon(stageClass(i) === 'complete' ? 'checkCircle' : stage.icon)}</span><strong>${stage.title}</strong><small>${stageClass(i) === 'skipped' ? 'Skipped in illustration' : stageClass(i) === 'failed' ? 'Rejected in illustration' : stage.tool}</small></button>`).join('')}</div></div><div class="simulation-status" role="status"><span class="dot"></span>${simulationMessage()}</div></section>${stageDetail()}`;
  }

  const flow = (title, subtitle, nodes, note, tag = 'IMPLEMENTATION VIEW') => `<section class="card flow-card"><div class="flow-title"><div><h3>${title}</h3><p>${subtitle}</p></div><span class="pill outline">${tag}</span></div><div class="flow-row">${nodes.map(([symbol, name, detail], index) => `${index ? '<span class="flow-arrow" aria-hidden="true">→</span>' : ''}<div class="flow-box">${icon(symbol)}<strong>${name}</strong><small>${detail}</small></div>`).join('')}</div><p class="flow-note">${note}</p></section>`;

  function architectureView() {
    return flow('Application runtime', 'A deliberately small application, with clear boundaries.',
      [['grid', 'Browser', 'HTML / CSS / JavaScript'], ['server', 'Java HTTP API', 'JDK HttpServer'], ['layers', 'Service layer', 'Validation + reviewer rules'], ['inbox', 'In-memory store', 'ConcurrentHashMap']],
      'There is no operational database in this release. Complaint records and event history reset with the process.')
      + flow('Jenkins delivery path', 'The intended pipeline after the automated test gates.',
        [['git', 'GitHub', 'Source revision'], ['workflow', 'Jenkins', 'Maven + JUnit + Selenium'], ['box', 'Docker registry', 'Versioned image'], ['server', 'Staging runtime', 'Container + Nginx']],
        'The current Docker build, publication and rollout are not verified by the recorded snapshot. Historical build #6 used an earlier native JAR deployment.', 'EXECUTION PENDING')
      + flow('Separate Ansible path', 'Do not confuse this operator workflow with the Jenkins pipeline.',
        [['terminal', 'Operator', 'Approved controller + access'], ['server', 'Linux baseline', 'Packages / users / Nginx'], ['box', 'Candidate image', 'Tagged Docker release'], ['health', 'Verify / recover', 'Health + rescue']],
        'Jenkins does not currently call Ansible. The remote target, second-run idempotency and rollback still require execution evidence.', 'SEPARATE WORKFLOW')
      + `<div class="architecture-grid">${imageCard('Recorded architecture', '/images/Task15_architecture.png', 'Task 15 architecture diagram. Recorded project snapshot, not a live topology.')}${imageCard('Recorded release boundaries', '/images/Task15_release_workflow.png', 'Task 15 release gates and verification boundaries. Captured 20 Oct 2026.')}</div>`;
  }

  function imageCard(title, source, caption) {
    return `<section class="card"><div class="card-header"><h2>${title}</h2><span class="pill blue">RECORDED</span></div><div class="card-body"><button class="image-link" data-image="${source}" data-alt="${e(caption)}" aria-label="Enlarge ${e(title)}"><img class="evidence-image" src="${source}" alt="${e(caption)}" loading="lazy"></button><p class="image-caption">${caption} Select to enlarge.</p></div></section>`;
  }

  function infrastructureView() {
    return `<div class="notice amber">${icon('info')}<span><strong>Implementation is not execution evidence.</strong> These commands explain the intended lifecycle; the recorded reports do not claim a verified Docker lifecycle or remote Ansible run.</span></div><section class="card"><div class="card-header"><div><h2>Docker: image → container → lifecycle</h2><p>An image is the packaged blueprint. A container is a running instance.</p></div><span class="pill amber">RUNTIME PENDING</span></div><div class="card-body lifecycle">${[
      ['01 · Build', 'Compile the source into a reproducible multi-stage image.', 'docker build -t municipal-portal:demo .'],
      ['02 · Run', 'Create a container and publish its port on the local host.', 'docker run -d -p 127.0.0.1:18085:8085 municipal-portal:demo'],
      ['03 · Inspect', 'Read the status, metadata and application output.', 'docker inspect <container>\ndocker logs <container>'],
      ['04 · Stop / start', 'Pause service availability and start the same container again.', 'docker stop <container>\ndocker start <container>'],
      ['05 · Restart', 'Restart the process; in-memory complaint state will reset.', 'docker restart <container>'],
      ['06 · Remove', 'Remove a stopped demo container when its lifecycle is complete.', 'docker rm <stopped-demo-container>'],
    ].map(([title, description, command]) => `<div class="lifecycle-item"><strong>${title}</strong><p>${description}</p><code>${e(command)}</code></div>`).join('')}</div></section><div class="lab-detail-grid"><section class="card stage-detail"><span class="pill blue">ANSIBLE · DESIRED STATE</span><h3>Configure once. Apply consistently.</h3><p>The baseline prepares a Debian/Ubuntu host: Docker and Nginx packages, a locked deployment account, application paths, environment configuration and validated reverse-proxy configuration.</p><pre>ansible/site.yml\nansible/templates/municipal-complaint-portal.conf.j2\nansible/task14.yml\nansible/health_checks.yml</pre><div class="callout">A true idempotency demonstration needs a second successful apply with unchanged desired state and its actual PLAY RECAP.</div></section><section class="card stage-detail"><span class="pill amber">RELIABILITY · EXECUTION PENDING</span><h3>Reject a candidate. Keep the failure visible.</h3><ol class="steps-list"><li><strong>Record the stable image</strong><p>Capture the previous immutable image ID.</p></li><li><strong>Deploy and check the candidate</strong><p>Verify direct backend and Nginx ingress health.</p></li><li><strong>Recover on rejection</strong><p>Restore the stable image, then recheck health.</p></li><li><strong>Preserve the failure signal</strong><p>The candidate deployment remains unsuccessful.</p></li></ol><button class="button" data-lab="recovery">${icon('play')}Explore recovery walkthrough</button></section></div>`;
  }

  function qualityView() {
    return `<div class="notice">${icon('calendar')}<span><strong>Recorded snapshot: 20 Oct 2026.</strong> Local revision abc900735330. These baseline numbers do not describe the current source or a live build.</span></div><div class="quality-grid"><article class="card quality-card"><span class="pill green">RECORDED LOCAL</span><div class="stat-value">17 / 17</div><h3>JUnit unit tests</h3><p>0 failures · 0 errors · 0 skipped</p><small>Maven Surefire · captured 20 Oct 2026</small></article><article class="card quality-card"><span class="pill green">RECORDED LOCAL</span><div class="stat-value">5 / 5</div><h3>Selenium journeys</h3><p>Headless Chrome · Maven Failsafe</p><small>Browser and driver compatibility warning recorded</small></article><article class="card quality-card"><span class="pill amber">RELEASE BOUNDARY</span><div class="stat-value">HOLD</div><h3>External rollout not verified</h3><p>Docker registry + remote Ansible target</p><small>Source exists; external runtime proof is pending</small></article></div><div class="architecture-grid"><section class="card stage-detail"><div class="eyebrow">TASK 10 · HISTORICAL QUALITY GATE</div><h3>A failure that prevented a bad release.</h3><ul class="compact-list"><li><span class="list-index">#5</span><div><strong>Deliberate API contract defect</strong><p>Revision 33d9f01 caused browser-test failures. The downstream deployment stage was skipped.</p></div></li><li><span class="list-index">#6</span><div><strong>Corrected source, successful rerun</strong><p>Revision 9aa2e01 passed the recorded tests and used the earlier native JAR/Nginx deployment.</p></div></li></ul><div class="callout">Neither historical run validates the newer Docker delivery stages.</div><a class="button small" href="/reports/task-10.docx" download>${icon('download')}Task 10 report</a></section>${imageCard('Historical failure / correction', '/images/jenkins_quality_gate_comparison_task10.png', 'Historical Task 10 comparison. Not a current Jenkins stage view.')}</div><section class="card stage-detail"><h3>Evidence you can explain honestly</h3><dl class="stage-facts"><div><dt>LIVE LOCAL HEALTH</dt><dd>The header checks only this portal.</dd></div><div><dt>RECORDED TESTS</dt><dd>Dated baseline, not auto-updated to current results.</dd></div><div><dt>ILLUSTRATIVE SIMULATION</dt><dd>Browser animation; no tools are executed.</dd></div><div><dt>UNVERIFIED DELIVERY</dt><dd>Docker publication and remote Ansible evidence pending.</dd></div></dl></section>`;
  }

  function draw() {
    container.innerHTML = heading('The engineering behind the experience.', 'An interactive guide to this project’s build, test and delivery workflow.')
      + `<section class="lab-intro"><div><div class="eyebrow">DEVOPS LAB / CONNECT THE DOTS</div><h2>From a single commit to a confident release.</h2><p>Explore the pipeline, understand its quality gates and follow the evidence. A practical learning experience—not a simulated production control panel.</p></div><div class="lab-intro-meta"><strong>15 tasks</strong>One complete learning journey<br>Java · Jenkins · Docker · Ansible</div></section><div class="tabs" role="tablist" aria-label="DevOps topics">${[
        ['pipeline', 'workflow', 'Pipeline explorer'], ['architecture', 'layers', 'Architecture'], ['infrastructure', 'box', 'Docker & Ansible'], ['quality', 'shield', 'Quality & evidence'],
      ].map(([key, symbol, title]) => `<button class="tab ${tab === key ? 'active' : ''}" id="tab-${key}" data-tab="${key}" role="tab" aria-selected="${tab === key}" aria-controls="lab-panel" tabindex="${tab === key ? '0' : '-1'}">${icon(symbol)}${title}</button>`).join('')}</div><section id="lab-panel" role="tabpanel" aria-labelledby="tab-${tab}">${({ pipeline: pipelineView, architecture: architectureView, infrastructure: infrastructureView, quality: qualityView }[tab])()}</section>`;
  }

  function advance() {
    if ((scenario === 'failure' && progress >= 4) || progress >= list().length - 1) { stop(); draw(); return; }
    progress++;
    selected = progress;
    if ((scenario === 'failure' && progress === 4) || progress === list().length - 1) stop();
    draw();
  }

  function onClick(event) {
    const tabButton = event.target.closest('[data-tab]');
    if (tabButton) { stop(); tab = tabButton.dataset.tab; draw(); container.querySelector(`#tab-${tab}`).focus(); return; }
    const stage = event.target.closest('[data-stage]');
    if (stage) { selected = Number(stage.dataset.stage); draw(); container.querySelector(`[data-stage="${selected}"]`).focus(); return; }
    const action = event.target.closest('[data-lab]')?.dataset.lab;
    if (action === 'next') { stop(); advance(); container.querySelector('[data-lab=next]')?.focus(); }
    if (action === 'reset') { stop(); progress = -1; selected = 0; draw(); container.querySelector('[data-lab=reset]')?.focus(); }
    if (action === 'play') {
      if (running) { stop(); draw(); return; }
      if (progress >= list().length - 1 || (scenario === 'failure' && progress >= 4)) progress = -1;
      if (reducedMotion) { advance(); return; }
      running = true;
      advance();
      if (progress < list().length - 1 && !(scenario === 'failure' && progress >= 4)) {
        running = true;
        timer = setInterval(advance, 1700);
        draw();
      }
    }
    if (action === 'recovery') { stop(); scenario = 'recovery'; selected = 0; progress = -1; tab = 'pipeline'; draw(); }
  }

  function onChange(event) {
    if (event.target.id === 'scenario') {
      stop(); scenario = event.target.value; selected = 0; progress = -1; draw();
      container.querySelector('#scenario').focus();
    }
  }

  function onKey(event) {
    const current = event.target.closest('[role=tab]');
    if (!current || !['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    const keys = ['pipeline', 'architecture', 'infrastructure', 'quality'];
    let index = keys.indexOf(tab);
    index = event.key === 'Home' ? 0 : event.key === 'End' ? 3 : (index + (event.key === 'ArrowRight' ? 1 : 3)) % 4;
    stop(); tab = keys[index]; draw(); container.querySelector(`#tab-${tab}`).focus();
  }

  container.addEventListener('click', onClick);
  container.addEventListener('change', onChange);
  container.addEventListener('keydown', onKey);
  draw();
  return () => {
    stop();
    container.removeEventListener('click', onClick);
    container.removeEventListener('change', onChange);
    container.removeEventListener('keydown', onKey);
  };
}

export function renderTasks(container, { icon, escapeHtml: e, heading }) {
  container.innerHTML = heading('15 tasks. One connected journey.', 'Explore the work, the learning outcomes and the evidence behind the project.')
    + `<div class="task-summary"><span class="pill green">${icon('book')}15 original reports available</span><span class="pill blue">Implementation & historical evidence</span><span class="pill amber">External release gates pending</span></div><div class="notice">${icon('info')}<span>Report availability, implementation and verified execution are different things. Reports 11–15 describe important runtime limitations. Historical files are preserved, not rewritten to imply newer results.</span></div><div class="task-filters"><div class="filter-field"><label for="task-search">Find a task or concept</label><input id="task-search" placeholder="Search Git, testing, Docker, rollback..." maxlength="100"></div><div class="filter-field"><label for="task-group">Learning chapter</label><select id="task-group"><option value="">All chapters</option>${[...new Set(taskData.map(task => task[1]))].map(group => `<option>${e(group)}</option>`).join('')}</select></div><span id="report-count" class="report-count"></span></div><div id="task-grid" class="task-grid"></div>`;

  let previousQuery = null;
  let previousGroup = null;
  function cards() {
    const query = container.querySelector('#task-search').value.toLowerCase();
    const group = container.querySelector('#task-group').value;
    if (query === previousQuery && group === previousGroup) return;
    previousQuery = query;
    previousGroup = group;
    const filtered = taskData.map((task, i) => ({ task, number: i + 1 })).filter(({ task, number }) => (!group || task[1] === group) && `${number} ${task.join(' ')}`.toLowerCase().includes(query));
    container.querySelector('#report-count').textContent = `${filtered.length} of 15 reports`;
    container.querySelector('#task-grid').innerHTML = filtered.length ? filtered.map(({ task, number }) => `<article class="card task-card"><div class="task-top"><span class="task-number">TASK ${String(number).padStart(2, '0')}</span><span class="pill ${number >= 11 ? 'amber' : number >= 7 ? 'blue' : 'outline'}">${number === 15 ? 'Local evidence / rollout pending' : number >= 11 ? 'Runtime evidence pending' : number >= 7 ? 'Historical evidence' : 'Implementation recorded'}</span></div><span class="eyebrow muted">${e(task[1])}</span><h3>${task[0]}</h3><p>${task[2]}</p><details><summary>Deliverables & source</summary><p>${task[4]}</p><p class="file-path">${e(task[3])}</p></details><a class="button small" href="/reports/task-${number}.docx" download>${icon('download')}Download report${icon('arrow')}</a></article>`).join('') : '<div class="empty-state"><h3>No matching tasks</h3><p>Try another concept or learning chapter.</p></div>';
  }
  container.addEventListener('input', cards);
  container.addEventListener('change', cards);
  cards();
  return () => {
    container.removeEventListener('input', cards);
    container.removeEventListener('change', cards);
  };
}
