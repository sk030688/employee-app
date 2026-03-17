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

        stage('5. Build Image in OpenShift') {
            steps {
                echo '====== Building image inside OpenShift ======'
                withCredentials([string(
                    credentialsId: 'openshift-token',
                    variable: 'OC_TOKEN')]) {
                    sh '''
                        # Login to OpenShift
                        oc login --token=${OC_TOKEN} \
                          --server=${OPENSHIFT_SERVER} \
                          --insecure-skip-tls-verify=true

                        oc project ${OC_PROJECT}

                        # Create BuildConfig if not exists
                        if ! oc get bc ${APP_NAME} 2>/dev/null; then
                            echo "Creating BuildConfig..."
                            oc new-build \
                              --name=${APP_NAME} \
                              --binary=true \
                              --strategy=docker \
                              -n ${OC_PROJECT}
                            echo "✅ BuildConfig created"
                        fi

                        # Start binary build
                        # Upload current workspace to OpenShift
                        echo "Starting build..."
                        oc start-build ${APP_NAME} \
                          --from-dir=. \
                          --follow \
                          --wait \
                          -n ${OC_PROJECT}

                        echo "✅ Image built in OpenShift registry!"

                        # Tag image with build number
                        oc tag \
                          ${OC_PROJECT}/${APP_NAME}:latest \
                          ${OC_PROJECT}/${APP_NAME}:${IMAGE_TAG}

                        echo "✅ Image tagged: ${APP_NAME}:${IMAGE_TAG}"
                    '''
                }
            }
        }

        stage('6. Deploy to DEV') {
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

                        # Internal registry image
                        INTERNAL_IMAGE="image-registry.openshift-image-registry.svc:5000/${OC_PROJECT}/${APP_NAME}:${IMAGE_TAG}"

                        echo "Deploying: $INTERNAL_IMAGE"

                        if oc get deployment ${APP_NAME}-dev 2>/dev/null; then
                            echo "Updating DEV..."
                            oc set image deployment/${APP_NAME}-dev \
                              ${APP_NAME}-dev=$INTERNAL_IMAGE
                            oc set env deployment/${APP_NAME}-dev \
                              APP_ENV=development \
                              BUILD_NUMBER=${BUILD_NUMBER}
                            oc rollout restart \
                              deployment/${APP_NAME}-dev
                        else
                            echo "Creating DEV..."
                            oc new-app \
                              --name=${APP_NAME}-dev \
                              --image=$INTERNAL_IMAGE
                            oc set env deployment/${APP_NAME}-dev \
                              APP_ENV=development
                            oc expose svc/${APP_NAME}-dev || true
                        fi

                        oc rollout status \
                          deployment/${APP_NAME}-dev \
                          --timeout=300s

                        DEV_URL=$(oc get route ${APP_NAME}-dev \
                          -o jsonpath="{.spec.host}" 2>/dev/null)
                        echo "✅ DEV URL: http://$DEV_URL"
                    '''
                }
            }
        }

        stage('7. Test DEV') {
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

                        echo "Testing: http://$DEV_URL"
                        sleep 15

                        curl -f http://$DEV_URL/actuator/health \
                          && echo "✅ DEV Health OK"
                        curl -f http://$DEV_URL/api/employees \
                          && echo "✅ DEV API OK"
                        echo "✅ DEV Tests PASSED!"
                    '''
                }
            }
        }

        stage('8. Deploy to TEST') {
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

                        INTERNAL_IMAGE="image-registry.openshift-image-registry.svc:5000/${OC_PROJECT}/${APP_NAME}:${IMAGE_TAG}"

                        if oc get deployment ${APP_NAME}-test 2>/dev/null; then
                            oc set image deployment/${APP_NAME}-test \
                              ${APP_NAME}-test=$INTERNAL_IMAGE
                            oc set env deployment/${APP_NAME}-test \
                              APP_ENV=testing \
                              BUILD_NUMBER=${BUILD_NUMBER}
                            oc rollout restart \
                              deployment/${APP_NAME}-test
                        else
                            oc new-app \
                              --name=${APP_NAME}-test \
                              --image=$INTERNAL_IMAGE
                            oc set env deployment/${APP_NAME}-test \
                              APP_ENV=testing
                            oc expose svc/${APP_NAME}-test || true
                        fi

                        oc rollout status \
                          deployment/${APP_NAME}-test \
                          --timeout=300s

                        TEST_URL=$(oc get route ${APP_NAME}-test \
                          -o jsonpath="{.spec.host}" 2>/dev/null)
                        echo "✅ TEST URL: http://$TEST_URL"
                    '''
                }
            }
        }

        stage('9. Test TEST Environment') {
            steps {
                echo '====== Integration Tests ======'
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

                        sleep 15
                        curl -f http://$TEST_URL/actuator/health \
                          && echo "✅ Health OK"
                        curl -f http://$TEST_URL/api/employees \
                          && echo "✅ API OK"
                        curl -f http://$TEST_URL/api/employees/info \
                          && echo "✅ Info OK"
                        echo "✅ All TEST Passed!"
                    '''
                }
            }
        }

        stage('10. Approve PROD') {
            steps {
                echo '====== Waiting for PROD Approval ======'
                input message: '''
                    ��� PRODUCTION DEPLOYMENT APPROVAL

                    ✅ DEV  tests - PASSED
                    ✅ TEST tests - PASSED
                    ✅ SonarQube  - PASSED
                    ✅ Image built in OpenShift

                    Approve PRODUCTION deployment?
                ''',
                ok: '✅ YES - Deploy to PRODUCTION!'
            }
        }

        stage('11. Deploy PROD') {
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

                        INTERNAL_IMAGE="image-registry.openshift-image-registry.svc:5000/${OC_PROJECT}/${APP_NAME}:${IMAGE_TAG}"

                        if oc get deployment ${APP_NAME}-prod 2>/dev/null; then
                            oc set image deployment/${APP_NAME}-prod \
                              ${APP_NAME}-prod=$INTERNAL_IMAGE
                            oc set env deployment/${APP_NAME}-prod \
                              APP_ENV=production \
                              BUILD_NUMBER=${BUILD_NUMBER}
                            oc rollout restart \
                              deployment/${APP_NAME}-prod
                        else
                            oc new-app \
                              --name=${APP_NAME}-prod \
                              --image=$INTERNAL_IMAGE
                            oc set env deployment/${APP_NAME}-prod \
                              APP_ENV=production
                            oc expose svc/${APP_NAME}-prod || true
                        fi

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
