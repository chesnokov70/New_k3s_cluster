pipeline {
    agent any


    environment {
        AWS_REGION = "us-east-1"
        GIT_REPO_URL = 'git@github.com:chesnokov70/Project_k3s-cluster.git'
        CREDENTIALS_ID = 'ssh_github_access_key' // Replace with your credential ID in Jenkins
        KUBECONFIG = "/var/jenkins_home/.kube/config" 
    }

        parameters {
        booleanParam(name: 'DESTROY', defaultValue: false, description: 'Check this to destroy infrastructure')
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'git@github.com:your-repo/infra.git'
            }
        }

        stage('Terraform Init & Apply') {
            when {
                expression { return !params.DESTROY }
            }
            steps {
                dir('terraform') {
                    sh '''
                    terraform init
                    terraform workspace new dev || terraform workspace select dev
                    terraform apply -auto-approve
                    '''
                }
            }
        }

        stage('Wait for Instance') {
            when {
                expression { return !params.DESTROY }
            }
            steps {
                script {
                    sh 'ansible -i ansible/inventory.ini all -m wait_for -a "port=22 timeout=300"'
                }
            }
        }

        stage('Run Ansible Playbook') {
            when {
                expression { return !params.DESTROY }
            }
            steps {
                dir('ansible') {
                    sh 'ansible-playbook -i inventory.ini playbook.yml'
                }
            }
        }

        stage('Deploy to ArgoCD') {
            when {
                expression { return !params.DESTROY }
            }
            steps {
                sh '''
                kubectl apply -f k8s/deployment.yaml
                argocd app sync my-app --async
                '''
            }
        }

        stage('Terraform Destroy') {
            when {
                expression { return params.DESTROY }
            }
            steps {
                dir('terraform') {
                    sh '''
                    terraform init
                    terraform workspace select dev
                    terraform destroy -auto-approve
                    '''
                }
            }
        }
    }
}
