pipeline {
    agent any

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['staging', 'production', 'development'], description: 'Deployment Target Environment')
        string(name: 'APP_PORT', defaultValue: '8085', description: 'Municipal Complaint Portal Backend Service Port')
        string(name: 'NGINX_PORT', defaultValue: '8090', description: 'Nginx Front-Facing Reverse Proxy Web Server Port')
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

        stage('Build') {
            steps {
                echo "================================================="
                echo " Stage 2: Build - Compiling Java Source Files"
                echo "================================================="
                bat 'cmd.exe /C "mvn clean compile"'
            }
        }

        stage('Package') {
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

        stage('Deploy') {
            steps {
                echo "================================================="
                echo " Stage 5: Deploy - Deploying Application to Nginx"
                echo " Target Environment : ${params.DEPLOY_ENV}"
                echo " Backend Port       : ${params.APP_PORT}"
                echo " Nginx Proxy Port   : ${params.NGINX_PORT}"
                echo "================================================="
                bat """
                    powershell -ExecutionPolicy Bypass -File deploy\\deploy.ps1 -DeployEnv "${params.DEPLOY_ENV}" -AppPort ${params.APP_PORT} -NginxPort ${params.NGINX_PORT}
                """
            }
        }
    }

    post {
        always {
            echo "================================================="
            echo " [Test Quality Gate] Publishing JUnit & Failsafe Reports"
            echo "================================================="
            junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
        }
        success {
            echo "================================================="
            echo " [CI/CD SUCCESS] Pipeline Completed Successfully!"
            echo " All Quality Gates Passed (Unit & Selenium Tests)!"
            echo " Deployed Application URL : http://localhost:${params.NGINX_PORT}/"
            echo " Health Check Endpoint   : http://localhost:${params.NGINX_PORT}/api/health"
            echo " Deployed Environment    : ${params.DEPLOY_ENV}"
            echo "================================================="
        }
        failure {
            echo "================================================="
            echo " [CI/CD FAILURE] Quality Gate Failed! Deployment stopped due to test failure."
            echo "================================================="
        }
    }
}
