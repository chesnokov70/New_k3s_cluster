pipeline {
    agent any

    tools {
        terraform 'tf1.6'
    }
    

    environment {
        AWS_REGION = "us-east-1"
        GIT_REPO_URL = 'git@github.com:chesnokov70/Project_k3s-cluster.git'
        CREDENTIALS_ID = 'ssh_github_access_key' // Replace with your credential ID in Jenkins
        KUBECONFIG = "/var/jenkins_home/.kube/config" 
    }

    stages {
        stage('Sparse Checkout') {
            steps {
                script {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: 'main']],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [[
                            $class: 'SparseCheckoutPaths',
                            sparseCheckoutPaths: [[path: 'K8S_Project/']]
                        ]],
                        userRemoteConfigs: [[
                            url: env.GIT_REPO_URL,
                            credentialsId: env.CREDENTIALS_ID
                        ]]
                    ])
                }
            }
        }

        stage('Terraform Init & Apply') {
            steps {
                dir('terraform') {
                    sh 'terraform init'
                    sh 'terraform apply -auto-approve'
                }
            }
        }

        stage('Wait for Instance') {
            steps {
                script {
                    sleep 60  // Waiting 60 sec, until EC2 will start
                }
            }
        }

        stage('Run Ansible Playbook') {
            steps {
                dir('ansible') {
                    sh 'ansible-playbook -i inventory.ini playbook.yaml'
                }
            }
        }

        stage('Deploy to ArgoCD') {
            steps {
                sh '''
                kubectl apply -f k8s/argocd-app.yaml
                '''
            }
        }
    }
}
