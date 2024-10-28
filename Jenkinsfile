pipeline {
  agent any
  stages {
    stage('Build Common') {
      parallel {
        stage('Build Common') {
          steps {
            withGradle() {
              sh './gradlew :common:build --rerun-tasks -x check'
            }

          }
        }

        stage('test') {
          steps {
            echo 'test'
          }
        }

      }
    }

    stage('Build Hocon Lang') {
      parallel {
        stage('Build Hocon Lang') {
          steps {
            withGradle() {
              sh './gradlew :lang:hocon:build --rerun-tasks -x check'
            }

          }
        }

        stage('Build YAML Lang') {
          agent {
            node {
              label 'tmp'
            }

          }
          environment {
            TEST = 'true'
          }
          steps {
            withGradle() {
              sh './gradlew :lang:hocon:build --rerun-tasks -x check'
            }

          }
        }

      }
    }

  }
}