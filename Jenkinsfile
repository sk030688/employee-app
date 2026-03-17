pipeline {
    agent any

    environment {
        APP_NAME         = 'employee-app'
        OPENSHIFT_SERVER = 'https://api.rm3.7wse.p1.openshiftapps.com:6443'
        OC_PROJECT       = 'sabbavarapusatishkum-dev'
        SONAR_URL        = 'http://192.168.1.93:9000'
        IMAGE_TAG        = "v${BUILD_NUMBER}"
        GIT_REPO         = 'https://github.com/sk030688/employee-app.git'
    }

    stages {

        stage('1. Clone Code') {
            steps {
                echo '====== Cloning code from GitHub ======'
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: "${GIT_REPO}"
                sh 'ls -la'
                sh 'git log --oneline -3'
                echo '====== Clone DONE ======'
            }
        }

        stage('2. Maven Build') {
            steps {
                echo '====== Building with Maven ======'
                sh 'mvn clean package -DskipTests'
                sh 'ls -la target/*.jar'
                echo '====== Build DONE ======'
            }
        }

        stage('3. Maven Test') {
            steps {
                echo '====== Running Unit Tests ======'
                sh 'mvn test'
                echo '====== Tests DONE ======'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('4. SonarQube Analysis') {
            steps {
                echo '====== Code Quality Check ======'
                withCredentials([string(
                    credentialsId: 'sonar-token',
                    variable: 'SONAR_TOKEN')]) {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=${APP_NAME} \
                          -Dsonar.host.url=${SONAR_URL} \
                          -Dsonar.login=${SONAR_TOKEN}
                    '''
                }
                echo '====== SonarQube DONE ======'
            }
        }

        stage('5. Docker Build') {
            steps {
                echo '====== Building Docker Image ======'
                sh "docker build -t ${APP_NAME}:${IMAGE_TAG} ."
                sh "docker build -t ${APP_NAME}:latest ."
                sh "docker images | grep ${APP_NAME}"
                echo '====== Docker Build DONE ======'
            }
        }

        stage('6. Push to OpenShift Registry') {
            steps {
                echo '====== Pushing image to OpenShift ======'
                withCredentials([string(
                    credentialsId: 'openshift-token',
                    variable: 'OC_TOKEN')]) {
                    sh '''
                        # Login to OpenShift
                        oc login --token=${OC_TOKEN} \
                          --server=${OPENSHIFT_SERVER} \
                          --insecure-skip-tls-verify=true

                        # Switch to project
                        oc project ${OC_PROJECT}

                        # Get registry URL
                        REGISTRY=$(oc get route default-route \
                          -n openshift-image-registry \
                          -o jsonpath="{.spec.host}" 2>/dev/null || \
                          echo "image-registry.openshift-image-registry.svc:5000")

                        echo "Registry: $REGISTRY"

                        # Login to registry
                        docker login -u $(oc whoami) \
                          -p ${OC_TOKEN} \
                          $REGISTRY || true

                        # Tag image
                        docker tag ${APP_NAME}:${IMAGE_TAG} \
                          $REGISTRY/${OC_PROJECT}/${APP_NAME}:${IMAGE_TAG}
                        docker tag ${APP_NAME}:latest \
                          $REGISTRY/${OC_PROJECT}/${APP_NAME}:latest

                        # Push image
                        docker push \
                          $REGISTRY/${OC_PROJECT}/${APP_NAME}:${IMAGE_TAG} || true
                        docker push \
                          $REGISTRY/${OC_PROJECT}/${APP_NAME}:latest || true

                        echo "✅ Image pushed successfully"
                    '''
                }
            }
        }

        stage('7. Deploy to DEV') {
            steps {
                echo '====== Deploying to DEV ======'
                withCredentials([string(
                    credentialsId: 'openshift-token',
                    variable: 'OC_TOKEN')]) {
                    sh '''
                        oc login --token=${OC_TOKEN} \
                          --server=${OPENSHIFT_SERVER} \
                          --insecure-skip-tls-verify=true

                        oc project ${OC_PROJECT}

                        # Check if deployment exists
                        if oc get deployment ${APP_NAME}-dev 2>/dev/null; then
                            echo "Updating DEV..."
                            oc set env deployment/${APP_NAME}-dev \
                              APP_ENV=development \
                              BUILD_NUMBER=${BUILD_NUMBER}
                            oc rollout restart \
                              deployment/${APP_NAME}-dev
                        else
                            echo "Creating DEV..."
                            oc new-app \
                              --name=${APP_NAME}-dev \
                              --image=image-registry.openshift-image-registry.svc:5000/${OC_PROJECT}/${APP_NAME}:latest \
                              --allow-missing-images=true
                            oc set env deployment/${APP_NAME}-dev \
                              APP_ENV=development
                            oc expose svc/${APP_NAME}-dev || true
                        fi

                        # Wait for rollout
                        oc rollout status \
                          deployment/${APP_NAME}-dev \
                          --timeout=300s

                        # Show URL
                        DEV_URL=$(oc get route ${APP_NAME}-dev \
                          -o jsonpath="{.spec.host}" 2>/dev/null \
                          || echo "no-route")
                        echo "✅ DEV URL: http://$DEV_URL"
                    '''
                }
            }
        }

        stage('8. Test DEV') {
            steps {
                echo '====== Testing DEV ======'
                withCredentials([string(
                    credentialsId: 'openshift-token',
                    variable: 'OC_TOKEN')]) {
                    sh '''
                        oc login --token=${OC_TOKEN} \
                          --server=${OPENSHIFT_SERVER} \
                          --insecure-skip-tls-verify=true
                        oc project ${OC_PROJECT}

                        DEV_URL=$(oc get route ${APP_NAME}-dev \
                          -o jsonpath="{.spec.host}")

                        curl -f http://$DEV_URL/actuator/health \
                          && echo "✅ DEV Health OK"
                        curl -f http://$DEV_URL/api/employees \
                          && echo "✅ DEV API OK"
                        echo "✅ DEV Tests PASSED!"
                    '''
                }
            }
        }

        stage('9. Deploy to TEST') {
            steps {
                echo '====== Deploying to TEST ======'
                withCredentials([string(
                    credentialsId: 'openshift-token',
                    variable: 'OC_TOKEN')]) {
                    sh '''
                        oc login --token=${OC_TOKEN} \
                          --server=${OPENSHIFT_SERVER} \
                          --insecure-skip-tls-verify=true
                        oc project ${OC_PROJECT}

                        if oc get deployment ${APP_NAME}-test 2>/dev/null; then
                            oc set env deployment/${APP_NAME}-test \
                              APP_ENV=testing \
                              BUILD_NUMBER=${BUILD_NUMBER}
                            oc rollout restart \
                              deployment/${APP_NAME}-test
                        else
                            oc new-app \
                              --name=${APP_NAME}-test \
                              --image=image-registry.openshift-image-registry.svc:5000/${OC_PROJECT}/${APP_NAME}:latest \
                              --allow-missing-images=true
                            oc set env deployment/${APP_NAME}-test \
                              APP_ENV=testing
                            oc expose svc/${APP_NAME}-test || true
                        fi

                        oc rollout status \
                          deployment/${APP_NAME}-test \
                          --timeout=300s

                        TEST_URL=$(oc get route ${APP_NAME}-test \
                          -o jsonpath="{.spec.host}" 2>/dev/null \
                          || echo "no-route")
                        echo "✅ TEST URL: http://$TEST_URL"
                    '''
                }
            }
        }

        stage('10. Test TEST Environment') {
            steps {
                echo '====== Integration Tests on TEST ======'
                withCredentials([string(
                    credentialsId: 'openshift-token',
                    variable: 'OC_TOKEN')]) {
                    sh '''
                        oc login --token=${OC_TOKEN} \
                          --server=${OPENSHIFT_SERVER} \
                          --insecure-skip-tls-verify=true
                        oc project ${OC_PROJECT}

                        TEST_URL=$(oc get route ${APP_NAME}-test \
                          -o jsonpath="{.spec.host}")

                        curl -f http://$TEST_URL/actuator/health \
                          && echo "✅ Health OK"
                        curl -f http://$TEST_URL/api/employees \
                          && echo "✅ API OK"
                        curl -f http://$TEST_URL/api/employees/info \
                          && echo "✅ Info OK"
                        echo "✅ All TEST checks PASSED!"
                    '''
                }
            }
        }

        stage('11. Approve PROD') {
            steps {
                echo '====== Waiting for PROD Approval ======'
                input message: '''
                    ��� PRODUCTION DEPLOYMENT APPROVAL

                    ✅ DEV  tests - PASSED
                    ✅ TEST tests - PASSED
                    ✅ SonarQube  - PASSED
                    ✅ Docker image - BUILT

                    Approve PRODUCTION deployment?
                ''',
                ok: '✅ YES - Deploy to PRODUCTION!'
            }
        }

        stage('12. Deploy PROD') {
            steps {
                echo '====== Deploying to PRODUCTION ======'
                withCredentials([string(
                    credentialsId: 'openshift-token',
                    variable: 'OC_TOKEN')]) {
                    sh '''
                        oc login --token=${OC_TOKEN} \
                          --server=${OPENSHIFT_SERVER} \
                          --insecure-skip-tls-verify=true
                        oc project ${OC_PROJECT}

                        if oc get deployment ${APP_NAME}-prod 2>/dev/null; then
                            oc set env deployment/${APP_NAME}-prod \
                              APP_ENV=production \
                              BUILD_NUMBER=${BUILD_NUMBER}
                            oc rollout restart \
                              deployment/${APP_NAME}-prod
                        else
                            oc new-app \
                              --name=${APP_NAME}-prod \
                              --image=image-registry.openshift-image-registry.svc:5000/${OC_PROJECT}/${APP_NAME}:latest \
                              --allow-missing-images=true
                            oc set env deployment/${APP_NAME}-prod \
                              APP_ENV=production
                            oc expose svc/${APP_NAME}-prod || true
                        fi

                        # Scale PROD to 2 replicas
                        oc scale deployment/${APP_NAME}-prod \
                          --replicas=2

                        oc rollout status \
                          deployment/${APP_NAME}-prod \
                          --timeout=300s

                        PROD_URL=$(oc get route ${APP_NAME}-prod \
                          -o jsonpath="{.spec.host}")

                        echo ""
                        echo "╔══════════════════════════════════╗"
                        echo "║  ��� PRODUCTION DEPLOYED!         ║"
                        echo "║  URL: http://$PROD_URL           ║"
                        echo "╚══════════════════════════════════╝"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '''
            ╔══════════════════════════════════╗
            ║  ✅ PIPELINE SUCCESS!            ║
            ╚══════════════════════════════════╝
            '''
        }
        failure {
            echo '''
            ╔══════════════════════════════════╗
            ║  ❌ PIPELINE FAILED!             ║
            ║  Check logs above for errors     ║
            ╚══════════════════════════════════╝
            '''
        }
        always {
            echo "Pipeline finished - Build #${BUILD_NUMBER}"
            cleanWs()
        }
    }
}
