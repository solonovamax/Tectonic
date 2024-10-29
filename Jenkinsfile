pipeline {
    agent any

    tools {
        jdk "Temurin Java 21"
    }

    triggers {
        githubPush()
        // githubPullRequests(
        //         spec: 'H/5 * * * *',
        //         events: [Open(), commitChanged(), commentPattern('/build')],
        //         triggerMode: 'HEAVY_HOOKS',
        //         userRestriction: [orgs: 'PolyhedralDev', users: '']
        // )
    }

    stages {
        stage('Checkout') {
            steps {
                scmSkip(deleteBuild: true)
            }
        }

        stage('Setup Gradle') {
            steps {
                sh 'chmod +x gradlew'
            }
        }

        stage('Build') {
            steps {
                withGradle {
                    sh './gradlew build --rerun-tasks -x check'
                }
            }

            post {
                success {
                    archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true, onlyIfSuccessful: true

                    javadoc javadocDir: 'build/docs/javadoc/', keepAll: true
                }
            }
        }

        stage('Tests') {
            steps {
                withGradle {
                    sh './gradlew test --rerun-tasks'
                }
            }

            post {
                success {
                    junit testResults: '**/build/test-results/*/*.xml'
                }
            }
        }
    }

    post {
        always {
            discoverReferenceBuild()

            recordIssues(
                    aggregatingResults: true,
                    enabledForFailure: true,
                    minimumSeverity: 'ERROR',
                    sourceCodeEncoding: 'UTF-8',
                    checksAnnotationScope: 'ALL',
                    sourceCodeRetention: 'LAST_BUILD',
                    tools: [java(), javaDoc()]
            )

            cleanWs()
        }
    }
}