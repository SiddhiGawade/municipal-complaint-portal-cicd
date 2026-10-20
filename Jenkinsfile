pipeline {
    agent any

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['staging', 'production', 'development'], description: 'Deployment Target Environment')
        string(name: 'APP_PORT', defaultValue: '8085', description: 'Municipal Complaint Portal Backend Service Port')
        string(name: 'NGINX_PORT', defaultValue: '8090', description: 'Nginx Front-Facing Reverse Proxy Web Server Port')
        string(name: 'DOCKERHUB_REPOSITORY', defaultValue: '', description: 'Docker Hub namespace/repository, for example myuser/municipal-complaint-portal')
    }

    environment {
        BUILD_ID = 'dontKillMe'
        JENKINS_NODE_COOKIE = 'dontKillMe'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "================================================="
                echo " Stage 1: Checkout SCM - Municipal Complaint Portal"
                echo " Environment: ${params.DEPLOY_ENV}"
                echo "================================================="
                checkout scm
                bat 'git log -n 1 --oneline'
            }
        }

        stage('Prepare Image Version') {
            steps {
                script {
                    def repository = params.DOCKERHUB_REPOSITORY?.trim()
                    if (!repository) {
                        repository = env.DOCKERHUB_REPOSITORY_CONFIG?.trim()
                    }
                    if (!(repository ==~ /[a-z0-9]+(?:[._-][a-z0-9]+)*\/[a-z0-9]+(?:[._-][a-z0-9]+)*/)) {
                        error('Set the DOCKERHUB_REPOSITORY build parameter or configure DOCKERHUB_REPOSITORY_CONFIG for automatic builds.')
                    }

                    [params.APP_PORT, params.NGINX_PORT].each { port ->
                        if (!(port ==~ /[0-9]{1,5}/) || port.toInteger() < 1 || port.toInteger() > 65535) {
                            error("Port must be an integer from 1 to 65535: ${port}")
                        }
                    }

                    env.APP_VERSION = bat(
                        returnStdout: true,
                        script: '@echo off\r\npowershell -NoProfile -NonInteractive -Command "[xml]$pom=Get-Content -Raw pom.xml; $pom.project.version"'
                    ).trim()
                    env.GIT_COMMIT_SHORT = bat(
                        returnStdout: true,
                        script: '@echo off\r\ngit rev-parse --short=12 HEAD'
                    ).trim()

                    if (!(env.APP_VERSION ==~ /[A-Za-z0-9_][A-Za-z0-9_.-]*/) ||
                        !(env.GIT_COMMIT_SHORT ==~ /[0-9a-fA-F]{7,40}/)) {
                        error('Could not create a valid Docker tag from the Maven version and Git commit.')
                    }

                    env.IMAGE_VERSION = "${env.APP_VERSION}-${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                    env.IMAGE_REF = "${repository}:${env.IMAGE_VERSION}"
                    env.LATEST_REF = "${repository}:latest"
                    env.CONTAINER_NAME = "municipal-portal-${params.DEPLOY_ENV}"
                    echo "Versioned image: ${env.IMAGE_REF}"
                }
            }
        }

        stage('Build') {
            steps {
                echo "================================================="
                echo " Stage 2: Build - Compiling Java Source Files"
                echo "================================================="
                bat 'cmd.exe /C "mvn clean compile"'
            }
        }

        stage('Package and Unit Tests') {
            steps {
                echo "================================================="
                echo " Stage 3: Package - Executing Unit Tests & Creating JAR"
                echo "================================================="
                bat 'cmd.exe /C "mvn test package"'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, onlyIfSuccessful: true
            }
        }

        stage('Selenium Integration Tests') {
            steps {
                echo "================================================="
                echo " Stage 4: Selenium Integration Tests - Headless E2E"
                echo "================================================="
                bat 'cmd.exe /C "mvn failsafe:integration-test failsafe:verify -Pselenium"'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "================================================="
                echo " Stage 5: Build versioned Docker image"
                echo " Image: ${env.IMAGE_REF}"
                echo "================================================="
                bat 'docker version'
                bat 'docker build --pull --label "org.opencontainers.image.revision=%GIT_COMMIT_SHORT%" --label "org.opencontainers.image.version=%IMAGE_VERSION%" --tag "%IMAGE_REF%" --tag "%LATEST_REF%" .'
            }
        }

        stage('Publish Docker Image') {
            steps {
                script {
                    env.DOCKER_CONFIG = "${pwd()}\\.docker-config-${env.BUILD_NUMBER}"
                }
                withEnv(["DOCKER_CONFIG=${env.DOCKER_CONFIG}"]) {
                    bat '@echo off\r\nif not exist "%DOCKER_CONFIG%" mkdir "%DOCKER_CONFIG%"'
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USERNAME',
                        passwordVariable: 'DOCKERHUB_TOKEN'
                    )]) {
                        bat '''
                            @echo off
                            powershell -NoProfile -NonInteractive -Command "$env:DOCKERHUB_TOKEN | docker login --username $env:DOCKERHUB_USERNAME --password-stdin; exit $LASTEXITCODE"
                            if errorlevel 1 exit /b 1
                        '''
                    }
                    bat 'docker push "%IMAGE_REF%"'
                    bat 'docker push "%LATEST_REF%"'
                    echo "Published immutable build tag ${env.IMAGE_REF} and convenience tag ${env.LATEST_REF}"
                }
            }
        }

        stage('Deploy Fresh Container') {
            steps {
                echo "================================================="
                echo " Stage 6: Pull published image and deploy a fresh container"
                echo " Image: ${env.IMAGE_REF}"
                echo " Container: ${env.CONTAINER_NAME}"
                echo "================================================="
                withEnv(["DOCKER_CONFIG=${env.DOCKER_CONFIG}"]) {
                    bat """
                        powershell -NoProfile -ExecutionPolicy Bypass -File deploy\\deploy.ps1 -DeployEnv "${params.DEPLOY_ENV}" -AppPort ${params.APP_PORT} -NginxPort ${params.NGINX_PORT} -ContainerImage "${env.IMAGE_REF}" -ContainerName "${env.CONTAINER_NAME}" -EvidencePath "target\\docker-deployment-evidence.json"
                    """
                }
                archiveArtifacts artifacts: 'target/docker-deployment-evidence.json', fingerprint: true
            }
        }
    }

    post {
        always {
            script {
                if (env.DOCKER_CONFIG?.trim()) {
                    withEnv(["DOCKER_CONFIG=${env.DOCKER_CONFIG}"]) {
                        bat(returnStatus: true, script: '@echo off\r\ndocker logout docker.io')
                        bat(returnStatus: true, script: '@echo off\r\nif exist "%DOCKER_CONFIG%" rmdir /s /q "%DOCKER_CONFIG%"')
                    }
                }
            }
            echo "================================================="
            echo " [Test Quality Gate] Publishing JUnit & Failsafe Reports"
            echo "================================================="
            junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
        }
        success {
            echo "================================================="
            echo " [CI/CD SUCCESS] Pipeline Completed Successfully!"
            echo " All Unit & Selenium Quality Gates Passed."
            echo " Published Image         : ${env.IMAGE_REF}"
            echo " Deployed Application URL: http://localhost:${params.NGINX_PORT}/"
            echo " Health Check Endpoint   : http://localhost:${params.NGINX_PORT}/api/health"
            echo " Deployed Environment    : ${params.DEPLOY_ENV}"
            echo "================================================="
        }
        failure {
            echo "================================================="
            echo " [CI/CD FAILURE] Quality Gate or Delivery Failed."
            echo " Docker build, publish and deploy only run after tests pass."
            echo "================================================="
        }
    }
}
