pipeline {
    agent any

    environment {
        AWS_REGION = "us-east-1"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'git@github.com:your-repo/infra.git'
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
                    sh 'ansible-playbook -i inventory.ini playbook.yml'
                }
            }
        }

        stage('Deploy to ArgoCD') {
            steps {
                sh '''
                kubectl apply -f k8s/deployment.yaml
                '''
            }
        }
    }
}
