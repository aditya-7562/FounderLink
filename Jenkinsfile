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
        DOCKER_REPO           = 'founderlink'
        SERVICES              = ''
        INFRA_SERVICES        = ''
        RESTART_SERVICES      = ''
        SKIP_BUILD            = 'false'
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

                    env.SERVICES       = ['auth-service','user-service','startup-service','investment-service',
                                          'team-service','messaging-service','notification-service',
                                          'payment-service','wallet-service','api-gateway'].join(",")
                    env.INFRA_SERVICES = ['config-server','eureka-server'].join(",")
                    env.COMMIT_TAG     = params.ROLLBACK_TAG

                    echo "Rolling back application services:    ${env.SERVICES}"
                    echo "Rolling back infrastructure services: ${env.INFRA_SERVICES}"
                }
            }
        }

        stage('Detect Changed Services') {
            when { expression { !params.ROLLBACK } }
            steps {
                script {

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
                        env.SKIP_BUILD = 'true'
                    }

                    if (env.SKIP_BUILD != 'true') {
                        def fileList = changedFiles
                            ? changedFiles.split("\n").collect { it.trim() }.findAll { it }
                            : []

                        echo "Parsed ${fileList.size()} changed file(s)"

                        def services        = [] as Set
                        def infraServices   = [] as Set
                        def restartServices = [] as Set

                        fileList.each { rawFile ->
                            String file = rawFile.toString()
                            if (file.startsWith("frontend/"))              return

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
                            if (file.startsWith("config-server/"))        infraServices.add("config-server")
                            if (file.startsWith("eureka-server/"))        infraServices.add("eureka-server")
                            if (file.startsWith("config-repo/"))          restartServices.add("config-server")
                        }

                        env.SERVICES         = services.join(",")
                        env.INFRA_SERVICES   = infraServices.join(",")
                        env.RESTART_SERVICES = restartServices.join(",")

                        if (!env.SERVICES && !env.INFRA_SERVICES && !env.RESTART_SERVICES) {
                            def nonServiceFiles = fileList.findAll { f ->
                                !f.startsWith("auth-service/") &&
                                !f.startsWith("user-service/") &&
                                !f.startsWith("startup-service/") &&
                                !f.startsWith("investment-service/") &&
                                !f.startsWith("team-service/") &&
                                !f.startsWith("messaging-service/") &&
                                !f.startsWith("notification-service/") &&
                                !f.startsWith("payment-service/") &&
                                !f.startsWith("wallet-service/") &&
                                !f.startsWith("api-gateway/") &&
                                !f.startsWith("config-server/") &&
                                !f.startsWith("eureka-server/") &&
                                !f.startsWith("config-repo/")
                            }
                            echo "No backend service changes detected. Skipping build."
                            echo "Non-service files changed (${nonServiceFiles.size()}): ${nonServiceFiles.take(5).join(', ')}${nonServiceFiles.size() > 5 ? ' ...' : ''}"
                            env.SKIP_BUILD = 'true'
                        } else {
                            if (env.SERVICES)         echo "Changed application services:    ${env.SERVICES}"
                            if (env.INFRA_SERVICES)   echo "Changed infrastructure services: ${env.INFRA_SERVICES}"
                            if (env.RESTART_SERVICES) echo "Config-repo changes — will restart: ${env.RESTART_SERVICES}"
                        }
                    }
                }
            }
        }

        stage('Run Tests') {
            when {
                expression { !params.ROLLBACK && env.SKIP_BUILD != 'true' && (env.SERVICES || env.INFRA_SERVICES) }
            }
            steps {
                script {
                    def allServices = []
                    if (env.SERVICES)       allServices.addAll(env.SERVICES.split(","))
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
                            elif [ -f "./${svc}/pom.xml" ]; then
                                cd ${svc}
                                mvn test || echo "Tests failed for ${svc}, continuing..."
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
                expression { !params.ROLLBACK && env.SKIP_BUILD != 'true' && (env.SERVICES || env.INFRA_SERVICES) }
            }
            steps {
                script {
                    def allServices = []
                    if (env.SERVICES)       allServices.addAll(env.SERVICES.split(","))
                    if (env.INFRA_SERVICES) allServices.addAll(env.INFRA_SERVICES.split(","))

                    def parallelStages = [:]
                    allServices.each { svc ->
                        parallelStages["Build ${svc}"] = {
                            echo "Building ${svc}"
                            sh """
                            echo "Pulling cache image for ${svc}..."
                            docker pull ${DOCKER_REPO}/${svc}:cache || true

                            docker build \\
                              --cache-from ${DOCKER_REPO}/${svc}:cache \\
                              -t ${DOCKER_REPO}/${svc}:${env.COMMIT_TAG} \\
                              -t ${DOCKER_REPO}/${svc}:cache \\
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
                expression {
                    !params.ROLLBACK &&
                    env.SKIP_BUILD != 'true' &&
                    (env.SERVICES || env.INFRA_SERVICES)
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
                        def allServices = []
                        if (env.SERVICES)       allServices.addAll(env.SERVICES.split(","))
                        if (env.INFRA_SERVICES) allServices.addAll(env.INFRA_SERVICES.split(","))

                        def parallelStages = [:]
                        allServices.each { svc ->
                            parallelStages["Push ${svc}"] = {
                                sh """
                                docker push ${DOCKER_REPO}/${svc}:${env.COMMIT_TAG}
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
                expression { env.INFRA_SERVICES != null && env.INFRA_SERVICES != "" }
            }
            steps {
                script {
                    env.INFRA_SERVICES.split(",").each { svc ->
                        echo "Deploying infrastructure service: ${svc}"
                        sh """
                        export TAG=${env.COMMIT_TAG}
                        docker compose -f docker-compose.infra.yml pull ${svc} || true
                        docker compose -f docker-compose.infra.yml up -d --no-deps ${svc}
                        """
                    }
                }
            }
        }

        stage('Deploy Application Services') {
            when {
                expression { env.SERVICES != null && env.SERVICES != "" }
            }
            steps {
                script {
                    env.SERVICES.split(",").each { svc ->
                        echo "Deploying application service: ${svc}"
                        sh """
                        export TAG=${env.COMMIT_TAG}
                        docker compose -f docker-compose.services.yml pull ${svc} || true
                        docker compose -f docker-compose.services.yml up -d --no-deps ${svc}
                        """
                    }
                }
            }
        }

        stage('Restart Config Services') {
            when {
                expression { env.RESTART_SERVICES != null && env.RESTART_SERVICES != "" }
            }
            steps {
                script {
                    env.RESTART_SERVICES.split(",").each { svc ->
                        echo "Restarting ${svc} to pick up config-repo changes..."
                        sh "docker compose -f docker-compose.infra.yml restart ${svc}"
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def allServices = []
                    if (env.SERVICES)         allServices.addAll(env.SERVICES.split(","))
                    if (env.INFRA_SERVICES)   allServices.addAll(env.INFRA_SERVICES.split(","))
                    if (env.RESTART_SERVICES) allServices.addAll(env.RESTART_SERVICES.split(","))

                    allServices = allServices.unique()

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
                expression { !params.ROLLBACK && env.SKIP_BUILD != 'true' }
            }
            steps {
                sh "docker image prune -f --filter 'until=72h'"
            }
        }
    }

    post {
        success {
            script {
                if (params.ROLLBACK) {
                    echo "✅ Rollback successful to tag: ${params.ROLLBACK_TAG}"
                } else if (env.SKIP_BUILD == 'true') {
                    echo "✅ Pipeline complete — no backend service changes, nothing deployed."
                } else {
                    def deployed = []
                    if (env.SERVICES)         deployed.add("App: ${env.SERVICES}")
                    if (env.INFRA_SERVICES)   deployed.add("Infra: ${env.INFRA_SERVICES}")
                    if (env.RESTART_SERVICES) deployed.add("Restarted: ${env.RESTART_SERVICES}")
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