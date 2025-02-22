# Project_k3s-cluster

stage ('Trigger Another Pipeline') {
    steps {
        script {
            build job: 'deploy_pacman_in_to_k3s_cluster'
        }
    }
}