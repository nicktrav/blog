module "network" {
  source = "../../modules/network"

  name = "production"
}

module "k8s" {
  source = "../../modules/kubernetes"

  name              = "production"
  network           = module.network.network
  master-version    = "1.14.8-gke.12"
  node-pool-version = "1.14.8-gke.12"
  node-count        = "1"
}

module "services" {
  source = "../../services"

  name-suffix   = "production"
  site-dns-name = "nicktrave.rs."
}
