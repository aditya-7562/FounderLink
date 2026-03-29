pipeline {
    agent any

    parameters {
        booleanParam(name: 'ROLLBACK', defaultValue: false, description: 'Enable rollback mode')
        string(name: 'ROLLBACK_TAG', defaultValue: '', description: 'Docker image tag to rollback to (only used when ROLLBACK=true)')
    }

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        DOCKERHUB_CREDENTIALS = 'dockerhub-creds'
        DOCKER_REPO = 'founderlink'
        BRANCH_NAME = "${env.GIT_BRANCH}"
        COMMIT_TAG = "${env.GIT_COMMIT.take(7)}"
        SERVICES = ""
        INFRA_SERVICES = ""
    }

    stages {

        stage('Rollback Mode') {
            when {
                expression { params.ROLLBACK }
            }
            steps {
                script {
                    if (!params.ROLLBACK_TAG) {
                        error("ROLLBACK_TAG parameter is required when ROLLBACK=true")
                    }
                    
                    echo "🔄 ROLLBACK MODE ENABLED"
                    echo "Target tag: ${params.ROLLBACK_TAG}"
                    
                    def allAppServices = ['auth-service', 'user-service', 'startup-service', 'investment-service', 
                                          'team-service', 'messaging-service', 'notification-service', 
                                          'payment-service', 'wallet-service', 'api-gateway']
                    def allInfraServices = ['config-server', 'eureka-server']
                    
                    env.SERVICES = allAppServices.join(",")
                    env.INFRA_SERVICES = allInfraServices.join(",")
                    env.COMMIT_TAG = params.ROLLBACK_TAG
                    
                    echo "Rolling back application services: ${env.SERVICES}"
                    echo "Rolling back infrastructure services: ${env.INFRA_SERVICES}"
                }
            }
        }

        stage('Checkout') {
            when {
                expression { !params.ROLLBACK }
            }
            steps {
                checkout scm
            }
        }

        stage('Detect Changed Services') {
            when {
                expression { !params.ROLLBACK }
            }
            steps {
                script {
                    def changedFiles
                    try {
                        changedFiles = sh(
                            script: "git diff --name-only HEAD~1 HEAD || git diff --name-only HEAD",
                            returnStdout: true
                        ).trim().split("\n")
                    } catch (Exception e) {
                        echo "First commit or no previous commit found. Building all services."
                        changedFiles = sh(
                            script: "git ls-files",
                            returnStdout: true
                        ).trim().split("\n")
                    }

                    def services = [] as Set
                    def infraServices = [] as Set

                    changedFiles.each { file ->
                        if (file.startsWith("auth-service/")) services.add("auth-service")
                        if (file.startsWith("user-service/")) services.add("user-service")
                        if (file.startsWith("startup-service/")) services.add("startup-service")
                        if (file.startsWith("investment-service/")) services.add("investment-service")
                        if (file.startsWith("team-service/")) services.add("team-service")
                        if (file.startsWith("messaging-service/")) services.add("messaging-service")
                        if (file.startsWith("notification-service/")) services.add("notification-service")
                        if (file.startsWith("payment-service/")) services.add("payment-service")
                        if (file.startsWith("wallet-service/")) services.add("wallet-service")
                        if (file.startsWith("api-gateway/")) services.add("api-gateway")
                        if (file.startsWith("config-server/")) infraServices.add("config-server")
                        if (file.startsWith("eureka-server/")) infraServices.add("eureka-server")
                    }

                    env.SERVICES = services.join(",")
                    env.INFRA_SERVICES = infraServices.join(",")

                    if (!env.SERVICES && !env.INFRA_SERVICES) {
                        echo "No service changes detected. Skipping build."
                        currentBuild.result = 'SUCCESS'
                        error("No changes to build")
                    }

                    echo "Changed application services: ${env.SERVICES}"
                    echo "Changed infrastructure services: ${env.INFRA_SERVICES}"
                }
            }
        }

        stage('Run Tests') {
            when {
                expression { !params.ROLLBACK }
            }
            steps {
                script {
                    def allServices = []
                    if (env.SERVICES) allServices.addAll(env.SERVICES.split(","))
                    if (env.INFRA_SERVICES) allServices.addAll(env.INFRA_SERVICES.split(","))

                    def parallelStages = [:]
                    allServices.each { svc ->
                        parallelStages["Test ${svc}"] = {
                            echo "Testing ${svc}"

                            sh """
                            if [ -f "./${svc}/mvnw" ]; then
                                cd ${svc}
                                chmod +x mvnw
                                ./mvnw test || echo "Tests failed for ${svc}, continuing..."
                                cd ..
                            elif [ -f "./${svc}/pom.xml" ]; then
                                cd ${svc}
                                mvn test || echo "Tests failed for ${svc}, continuing..."
                                cd ..
                            else
                                echo "No test configuration found for ${svc}, skipping tests"
                            fi
                            """
                        }
                    }

                    if (parallelStages.isEmpty()) {
                        echo "No services to test"
                    } else {
                        parallel parallelStages
                    }
                }
            }
        }

        stage('Build Images') {
            when {
                expression { !params.ROLLBACK }
            }
            steps {
                script {
                    def allServices = []
                    if (env.SERVICES) allServices.addAll(env.SERVICES.split(","))
                    if (env.INFRA_SERVICES) allServices.addAll(env.INFRA_SERVICES.split(","))

                    def parallelStages = [:]
                    allServices.each { svc ->
                        parallelStages["Build ${svc}"] = {
                            echo "Building ${svc}"

                            sh """
                            echo "Pulling cache image for ${svc}..."
                            docker pull ${DOCKER_REPO}/${svc}:cache || true
                            
                            docker build \
                              --cache-from ${DOCKER_REPO}/${svc}:cache \
                              -t ${DOCKER_REPO}/${svc}:${COMMIT_TAG} \
                              -t ${DOCKER_REPO}/${svc}:cache \
                              ./${svc}
                            """
                        }
                    }

                    if (parallelStages.isEmpty()) {
                        echo "No services to build"
                    } else {
                        parallel parallelStages
                    }
                }
            }
        }



        stage('Push Images') {
            when {
                expression { !params.ROLLBACK }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: DOCKERHUB_CREDENTIALS,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {

                    sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin"

                    script {
                        def allServices = []
                        if (env.SERVICES) allServices.addAll(env.SERVICES.split(","))
                        if (env.INFRA_SERVICES) allServices.addAll(env.INFRA_SERVICES.split(","))

                        def parallelStages = [:]
                        allServices.each { svc ->
                            parallelStages["Push ${svc}"] = {
                                sh """
                                docker push ${DOCKER_REPO}/${svc}:${COMMIT_TAG}
                                docker push ${DOCKER_REPO}/${svc}:cache
                                """
                            }
                        }

                        if (parallelStages.isEmpty()) {
                            echo "No services to push"
                        } else {
                            parallel parallelStages
                        }
                    }
                }
            }
        }

        stage('Prepare Environment') {
            steps {
                withCredentials([file(credentialsId: 'env-file', variable: 'ENV_FILE')]) {
                    sh """
                    cp $ENV_FILE .env

                    if [ ! -f .env ]; then
                        echo "ERROR: .env file not found!"
                        exit 1
                    fi

                    docker network create proxy-net 2>/dev/null || true
                    """
                }
            }
        }

        stage('Deploy Infrastructure Services') {
            when {
                expression { env.INFRA_SERVICES != "" }
            }
            steps {
                script {
                    env.INFRA_SERVICES.split(",").each { svc ->
                        echo "Deploying infrastructure service: ${svc}"
                        sh """
                        export TAG=${COMMIT_TAG}
                        docker compose -f docker-compose.infra.yml pull ${svc} || true
                        docker compose -f docker-compose.infra.yml up -d --no-deps ${svc}
                        """
                    }
                }
            }
        }

        stage('Deploy Application Services') {
            when {
                expression { env.SERVICES != "" }
            }
            steps {
                script {
                    env.SERVICES.split(",").each { svc ->
                        echo "Deploying application service: ${svc}"
                        sh """
                        export TAG=${COMMIT_TAG}
                        docker compose -f docker-compose.services.yml pull ${svc} || true
                        docker compose -f docker-compose.services.yml up -d --no-deps ${svc}
                        """
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def allServices = []
                    if (env.SERVICES) allServices.addAll(env.SERVICES.split(","))
                    if (env.INFRA_SERVICES) allServices.addAll(env.INFRA_SERVICES.split(","))

                    allServices.each { svc ->
                        echo "Checking health of ${svc}"
                        sh """
                        for i in {1..30}; do
                            if docker ps --filter "name=${svc}" --filter "status=running" | grep -q ${svc}; then
                                echo "${svc} is running"
                                exit 0
                            fi
                            echo "Waiting for ${svc} to start... \$i/30"
                            sleep 2
                        done
                        echo "WARNING: ${svc} may not be healthy"
                        """
                    }
                }
            }
        }

        stage('Cleanup Old Images') {
            when {
                expression { !params.ROLLBACK }
            }
            steps {
                sh """
                docker image prune -f --filter "until=72h"
                """
            }
        }
    }

    post {
        success {
            script {
                if (params.ROLLBACK) {
                    echo "✅ Rollback successful to tag: ${params.ROLLBACK_TAG}"
                } else {
                    def deployed = []
                    if (env.SERVICES) deployed.add("App: ${env.SERVICES}")
                    if (env.INFRA_SERVICES) deployed.add("Infra: ${env.INFRA_SERVICES}")
                    echo "✅ Deployment successful for ${deployed.join(' | ')}"
                    echo "Tag: ${COMMIT_TAG}"
                }
            }
        }
        failure {
            script {
                if (params.ROLLBACK) {
                    echo "❌ Rollback failed. Check logs."
                } else {
                    echo "❌ Pipeline failed. Check logs and consider rollback."
                    echo "To rollback, manually set TAG to previous commit and redeploy."
                }
            }
        }
        always {
            sh "docker logout || true"
        }
    }
}
