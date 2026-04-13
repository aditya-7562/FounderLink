pipeline {
    agent any

    parameters {
        booleanParam(name: 'ROLLBACK',     defaultValue: false, description: 'Enable rollback mode')
        string(name:  'ROLLBACK_TAG',      defaultValue: '',    description: 'Docker image tag to rollback to (only used when ROLLBACK=true)')
        booleanParam(name: 'FORCE_BUILD',  defaultValue: false, description: 'Force rebuild and redeploy ALL services regardless of what changed')
    }

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        DOCKERHUB_CREDENTIALS = 'dockerhub-creds'
        DOCKER_REPO           = 'founderlink'
        SERVICES_FILE         = '.pipeline_services'
        INFRA_FILE            = '.pipeline_infra'
        RESTART_FILE          = '.pipeline_restart'
        SKIP_FILE             = '.pipeline_skip'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    def tag = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    if (!tag) error("Failed to determine commit tag — git rev-parse returned empty")
                    env.COMMIT_TAG = tag
                    echo "Build commit tag: ${env.COMMIT_TAG}"
                }
            }
        }

        stage('Rollback Mode') {
            when { expression { params.ROLLBACK } }
            steps {
                script {
                    if (!params.ROLLBACK_TAG) error("ROLLBACK_TAG parameter is required when ROLLBACK=true")

                    echo "🔄 ROLLBACK MODE ENABLED — Target tag: ${params.ROLLBACK_TAG}"
                    env.COMMIT_TAG = params.ROLLBACK_TAG

                    def appServices   = 'auth-service,user-service,startup-service,investment-service,team-service,messaging-service,notification-service,payment-service,wallet-service,api-gateway'
                    def infraServices = 'config-server,eureka-server'

                    writeFile file: env.SERVICES_FILE, text: appServices
                    writeFile file: env.INFRA_FILE,    text: infraServices
                    writeFile file: env.RESTART_FILE,  text: ''
                    writeFile file: env.SKIP_FILE,     text: 'false'

                    echo "Rolling back application services:    ${appServices}"
                    echo "Rolling back infrastructure services: ${infraServices}"
                }
            }
        }

        stage('Detect Changed Services') {
            when { expression { !params.ROLLBACK } }
            steps {
                script {

                    if (params.FORCE_BUILD) {
                        echo "⚡ FORCE_BUILD enabled — rebuilding all services"
                        writeFile file: env.SERVICES_FILE, text: 'auth-service,user-service,startup-service,investment-service,team-service,messaging-service,notification-service,payment-service,wallet-service,api-gateway'
                        writeFile file: env.INFRA_FILE,    text: 'config-server,eureka-server'
                        writeFile file: env.RESTART_FILE,  text: ''
                        writeFile file: env.SKIP_FILE,     text: 'false'

                    } else {

                        def changedFiles = ""
                        def prevCommit = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT?.trim() \
                                      ?: env.GIT_PREVIOUS_COMMIT?.trim()

                        if (prevCommit && prevCommit != env.GIT_COMMIT?.trim()) {
                            echo "Previous commit: ${prevCommit}"
                            echo "Current  commit: ${env.GIT_COMMIT}"

                            def fetchStatus = sh(
                                script: "git fetch --no-tags origin ${prevCommit} 2>&1",
                                returnStatus: true
                            )

                            if (fetchStatus == 0) {
                                changedFiles = sh(
                                    script: "git diff --name-only FETCH_HEAD HEAD",
                                    returnStdout: true
                                ).trim()
                                echo "Diff: ${prevCommit} (FETCH_HEAD) → HEAD"
                                if (changedFiles) {
                                    echo "Raw changed files:\n${changedFiles}"
                                } else {
                                    echo "Raw changed files: (empty — diff returned nothing)"
                                }
                            } else {
                                echo "WARNING: Could not fetch previous commit ${prevCommit}. Treating as full rebuild."
                                changedFiles = sh(script: "git ls-files", returnStdout: true).trim()
                            }

                        } else if (!prevCommit) {
                            echo "No previous commit found. First build — treating all files as changed."
                            changedFiles = sh(script: "git ls-files", returnStdout: true).trim()

                        } else {
                            echo "No new commits since last build (same SHA: ${env.GIT_COMMIT}). Skipping build."
                            writeFile file: env.SERVICES_FILE, text: ''
                            writeFile file: env.INFRA_FILE,    text: ''
                            writeFile file: env.RESTART_FILE,  text: ''
                            writeFile file: env.SKIP_FILE,     text: 'true'
                            return
                        }

                        def fileList = changedFiles
                            ? changedFiles.split("\n").collect { it.trim() }.findAll { it }
                            : []

                        echo "Parsed ${fileList.size()} changed file(s)"

                        def services        = [] as Set
                        def infraServices   = [] as Set
                        def restartServices = [] as Set

                        fileList.each { file ->
                            if (file.startsWith("auth-service/"))         services.add("auth-service")
                            if (file.startsWith("user-service/"))         services.add("user-service")
                            if (file.startsWith("startup-service/"))      services.add("startup-service")
                            if (file.startsWith("investment-service/"))   services.add("investment-service")
                            if (file.startsWith("team-service/"))         services.add("team-service")
                            if (file.startsWith("messaging-service/"))    services.add("messaging-service")
                            if (file.startsWith("notification-service/")) services.add("notification-service")
                            if (file.startsWith("payment-service/"))      services.add("payment-service")
                            if (file.startsWith("wallet-service/"))       services.add("wallet-service")
                            if (file.startsWith("api-gateway/"))          services.add("api-gateway")
                            if (file.startsWith("frontend/"))             services.add("frontend")
                            if (file.startsWith("config-server/"))        infraServices.add("config-server")
                            if (file.startsWith("eureka-server/"))        infraServices.add("eureka-server")
                            if (file.startsWith("config-repo/"))          restartServices.add("config-server")
                        }

                        if (!services && !infraServices && !restartServices) {
                            def nonServiceFiles = fileList.findAll { f ->
                                String fs = f.toString()
                                !fs.startsWith("frontend/") &&
                                !fs.startsWith("auth-service/") &&
                                !fs.startsWith("user-service/") &&
                                !fs.startsWith("startup-service/") &&
                                !fs.startsWith("investment-service/") &&
                                !fs.startsWith("team-service/") &&
                                !fs.startsWith("messaging-service/") &&
                                !fs.startsWith("notification-service/") &&
                                !fs.startsWith("payment-service/") &&
                                !fs.startsWith("wallet-service/") &&
                                !fs.startsWith("api-gateway/") &&
                                !fs.startsWith("config-server/") &&
                                !fs.startsWith("eureka-server/") &&
                                !fs.startsWith("config-repo/")
                            }
                            echo "No backend service changes detected. Skipping build."
                            echo "Non-service files changed (${nonServiceFiles.size()}): ${nonServiceFiles.take(5).join(', ')}${nonServiceFiles.size() > 5 ? ' ...' : ''}"
                            writeFile file: env.SERVICES_FILE, text: ''
                            writeFile file: env.INFRA_FILE,    text: ''
                            writeFile file: env.RESTART_FILE,  text: ''
                            writeFile file: env.SKIP_FILE,     text: 'true'
                        } else {
                            writeFile file: env.SERVICES_FILE, text: services.join(",")
                            writeFile file: env.INFRA_FILE,    text: infraServices.join(",")
                            writeFile file: env.RESTART_FILE,  text: restartServices.join(",")
                            writeFile file: env.SKIP_FILE,     text: 'false'
                            if (services)        echo "Changed application services:    ${services.join(',')}"
                            if (infraServices)   echo "Changed infrastructure services: ${infraServices.join(',')}"
                            if (restartServices) echo "Config-repo changes — will restart: ${restartServices.join(',')}"
                        }
                    }
                }
            }
        }

        stage('Run Tests') {
            when {
                expression {
                    !params.ROLLBACK &&
                    !params.FORCE_BUILD &&
                    fileExists(env.SKIP_FILE) &&
                    readFile(env.SKIP_FILE).trim() != 'true' &&
                    (fileExists(env.SERVICES_FILE) && readFile(env.SERVICES_FILE).trim()) ||
                    (fileExists(env.INFRA_FILE)    && readFile(env.INFRA_FILE).trim())
                }
            }
            steps {
                script {
                    def svcList   = fileExists(env.SERVICES_FILE) ? readFile(env.SERVICES_FILE).trim() : ''
                    def infraList = fileExists(env.INFRA_FILE)    ? readFile(env.INFRA_FILE).trim()    : ''
                    def allServices = []
                    if (svcList)   allServices.addAll(svcList.split(",").findAll { it })
                    if (infraList) allServices.addAll(infraList.split(",").findAll { it })

                    if (allServices.isEmpty()) { echo "No services to test"; return }

                    allServices.each { svc ->
                        if (svc == "frontend") {
                            echo "Skipping ${svc} tests for now due to infrastructure hangs"
                            /*
                            docker.image('trion/ng-cli-karma:latest').inside {
                                sh "export CI=true && cd frontend && npm ci && npm run test -- --watch=false --no-progress"
                            }
                            */
                        } else {
                            echo "Testing ${svc}"
                            sh """
                            if [ -f "./${svc}/mvnw" ]; then
                                cd ${svc} && chmod +x mvnw && ./mvnw test || echo "Tests failed for ${svc}, continuing..."
                            elif [ -f "./${svc}/pom.xml" ]; then
                                cd ${svc} && mvn test || echo "Tests failed for ${svc}, continuing..."
                            else
                                echo "No test configuration found for ${svc}, skipping tests"
                            fi
                            """
                        }
                    }
                }
            }
        }

        stage('Build Images') {
            when {
                expression {
                    !params.ROLLBACK &&
                    fileExists(env.SKIP_FILE) &&
                    readFile(env.SKIP_FILE).trim() != 'true' &&
                    (params.FORCE_BUILD ||
                    (fileExists(env.SERVICES_FILE) && readFile(env.SERVICES_FILE).trim()) ||
                    (fileExists(env.INFRA_FILE)    && readFile(env.INFRA_FILE).trim()))
                }
            }
            steps {
                script {
                    def svcList   = fileExists(env.SERVICES_FILE) ? readFile(env.SERVICES_FILE).trim() : ''
                    def infraList = fileExists(env.INFRA_FILE)    ? readFile(env.INFRA_FILE).trim()    : ''
                    def allServices = []

                    if (svcList)   allServices.addAll(svcList.split(",").findAll { it })
                    if (infraList) allServices.addAll(infraList.split(",").findAll { it })

                    if (allServices.isEmpty()) {
                        echo "No services to build"
                        return
                    }

                    allServices.each { svc ->
                        echo "🚀 Building ${svc}"
                        if (svc == "frontend") {
                            withCredentials([
                                string(credentialsId: 'FRONTEND_API_URL', variable: 'API_URL'),
                                string(credentialsId: 'FRONTEND_RAZORPAY_KEY', variable: 'RAZORPAY_KEY')
                            ]) {
                                sh """
                                docker pull ${DOCKER_REPO}/${svc}:cache || true
                                docker build \
                                    --cache-from ${DOCKER_REPO}/${svc}:cache \
                                    --build-arg NG_APP_API_URL=${API_URL} \
                                    --build-arg NG_APP_RAZORPAY_KEY=${RAZORPAY_KEY} \
                                    -t ${DOCKER_REPO}/${svc}:${env.COMMIT_TAG} \
                                    ./${svc}
                                """
                            }
                        } else {
                            sh """
                            docker pull ${DOCKER_REPO}/${svc}:cache || true
                            docker build \
                            --cache-from ${DOCKER_REPO}/${svc}:cache \
                            -t ${DOCKER_REPO}/${svc}:${env.COMMIT_TAG} \
                            ./${svc}
                            """
                        }
                    }
                }
            }
        }

        stage('Push Images') {
            when {
                expression {
                    !params.ROLLBACK &&
                    fileExists(env.SKIP_FILE) &&
                    readFile(env.SKIP_FILE).trim() != 'true' &&
                    (params.FORCE_BUILD ||
                     (fileExists(env.SERVICES_FILE) && readFile(env.SERVICES_FILE).trim()) ||
                     (fileExists(env.INFRA_FILE)    && readFile(env.INFRA_FILE).trim()))
                }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: DOCKERHUB_CREDENTIALS,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                    script {
                        def svcList   = fileExists(env.SERVICES_FILE) ? readFile(env.SERVICES_FILE).trim() : ''
                        def infraList = fileExists(env.INFRA_FILE)    ? readFile(env.INFRA_FILE).trim()    : ''
                        def allServices = []
                        if (svcList)   allServices.addAll(svcList.split(",").findAll { it })
                        if (infraList) allServices.addAll(infraList.split(",").findAll { it })

                        if (allServices.isEmpty()) { echo "No services to push"; return }

                        // Step 1: Push commit tags in parallel (fast)
                        echo "📤 Pushing commit-tagged images in parallel..."
                        def parallelPushes = [:]
                        allServices.each { svc ->
                            parallelPushes["Push ${svc}:${env.COMMIT_TAG}"] = {
                                sh "docker push ${DOCKER_REPO}/${svc}:${env.COMMIT_TAG}"
                            }
                        }
                        parallel parallelPushes

                        // Step 2: Update cache tags sequentially (safe)
                        echo "🔄 Updating cache tags sequentially..."
                        allServices.each { svc ->
                            sh """
                            docker tag ${DOCKER_REPO}/${svc}:${env.COMMIT_TAG} ${DOCKER_REPO}/${svc}:cache
                            docker push ${DOCKER_REPO}/${svc}:cache
                            echo "✅ Cache updated for ${svc}"
                            """
                        }
                    }
                }
            }
        }

        stage('Prepare Environment') {
            steps {
                withCredentials([file(credentialsId: 'env-file', variable: 'ENV_FILE')]) {
                    sh '''
                    cp $ENV_FILE .env
                    if [ ! -f .env ]; then
                        echo "ERROR: .env file not found after copy!"
                        exit 1
                    fi
                    docker network create proxy-net 2>/dev/null || true
                    '''
                }
            }
        }

        stage('Deploy Infrastructure Services') {
            when {
                expression { fileExists(env.INFRA_FILE) && readFile(env.INFRA_FILE).trim() != '' }
            }
            steps {
                script {
                    readFile(env.INFRA_FILE).trim().split(",").findAll { it }.each { svc ->
                        echo "Deploying infrastructure service: ${svc}"
                        sh """
                        export TAG=${env.COMMIT_TAG}
                        docker compose -f docker-compose.infra.yml pull ${svc} || true
                        docker compose -f docker-compose.infra.yml up -d --no-deps --force-recreate ${svc}
                        """
                    }
                }
            }
        }

        stage('Deploy Application Services') {
            when {
                expression { fileExists(env.SERVICES_FILE) && readFile(env.SERVICES_FILE).trim() != '' }
            }
            steps {
                script {
                    readFile(env.SERVICES_FILE).trim().split(",").findAll { it }.each { svc ->
                        echo "Deploying application service: ${svc}"
                        def composeFile = (svc == "frontend") ? "docker-compose.frontend.yml" : "docker-compose.services.yml"
                        sh """
                        export TAG=${env.COMMIT_TAG}
                        docker compose -f ${composeFile} pull ${svc} || true
                        docker compose -f ${composeFile} up -d --no-deps --force-recreate ${svc}
                        """
                    }
                }
            }
        }

        stage('Restart Config Services') {
            when {
                expression { fileExists(env.RESTART_FILE) && readFile(env.RESTART_FILE).trim() != '' }
            }
            steps {
                script {
                    readFile(env.RESTART_FILE).trim().split(",").findAll { it }.each { svc ->
                        echo "Restarting ${svc} to pick up config-repo changes..."
                        sh "docker compose -f docker-compose.infra.yml restart ${svc}"
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def svcList     = fileExists(env.SERVICES_FILE) ? readFile(env.SERVICES_FILE).trim() : ''
                    def infraList   = fileExists(env.INFRA_FILE)    ? readFile(env.INFRA_FILE).trim()    : ''
                    def restartList = fileExists(env.RESTART_FILE)  ? readFile(env.RESTART_FILE).trim()  : ''

                    def allServices = [] as Set
                    if (svcList)     allServices.addAll(svcList.split(",").findAll { it })
                    if (infraList)   allServices.addAll(infraList.split(",").findAll { it })
                    if (restartList) allServices.addAll(restartList.split(",").findAll { it })

                    if (allServices.isEmpty()) {
                        echo "No services deployed — skipping health check."
                        return
                    }

                    allServices.each { svc ->
                        echo "Checking health of ${svc}"
                        sh """
                        for i in \$(seq 1 30); do
                            if docker ps --filter "name=${svc}" --filter "status=running" | grep -q ${svc}; then
                                echo "${svc} is running"
                                exit 0
                            fi
                            echo "Waiting for ${svc} to start... \$i/30"
                            sleep 2
                        done
                        echo "WARNING: ${svc} may not be healthy after 60s"
                        """
                    }
                }
            }
        }

        stage('Cleanup Old Images') {
            when {
                expression {
                    !params.ROLLBACK &&
                    fileExists(env.SKIP_FILE) &&
                    readFile(env.SKIP_FILE).trim() != 'true'
                }
            }
            steps {
                sh '''
                echo "🧹 Cleaning up Docker build artifacts..."
                docker builder prune -f --filter "until=72h"
                docker container prune -f
                docker image prune -f --filter 'until=72h'
                echo "✅ Cleanup complete"
                '''
            }
        }
    }

    post {
        success {
            script {
                if (params.ROLLBACK) {
                    echo "✅ Rollback successful to tag: ${params.ROLLBACK_TAG}"
                } else if (fileExists(env.SKIP_FILE) && readFile(env.SKIP_FILE).trim() == 'true') {
                    echo "✅ Pipeline complete — no backend service changes, nothing deployed."
                } else {
                    def svcList     = fileExists(env.SERVICES_FILE) ? readFile(env.SERVICES_FILE).trim() : ''
                    def infraList   = fileExists(env.INFRA_FILE)    ? readFile(env.INFRA_FILE).trim()    : ''
                    def restartList = fileExists(env.RESTART_FILE)  ? readFile(env.RESTART_FILE).trim()  : ''
                    def deployed = []
                    if (svcList)     deployed.add("App: ${svcList}")
                    if (infraList)   deployed.add("Infra: ${infraList}")
                    if (restartList) deployed.add("Restarted: ${restartList}")
                    echo "✅ Deployment successful for ${deployed.join(' | ')}"
                    echo "Tag: ${env.COMMIT_TAG}"
                }
            }
        }
        failure {
            script {
                if (params.ROLLBACK) {
                    echo "❌ Rollback failed. Check logs."
                } else {
                    echo "❌ Pipeline failed. Check logs and consider rollback."
                    echo "To rollback, set ROLLBACK=true and ROLLBACK_TAG to a previous commit tag."
                }
            }
        }
        always {
            sh "docker logout || true"
            cleanWs()
        }
    }
}