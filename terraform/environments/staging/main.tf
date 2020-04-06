module "network" {
  source = "../../modules/network"

  name = "staging"
}

module "k8s" {
  source = "../../modules/kubernetes"

  name              = "staging"
  network           = module.network.network
  master-version    = "1.15.11-gke.1"
  node-pool-version = "1.15.11-gke.1"
  node-count        = "0"
}

module "services" {
  source = "../../services"

  name-suffix   = "staging"
  site-dns-name = "stage.nicktrave.rs."
}
