resource "google_compute_subnetwork" "k8s-cluster-uc1" {
  name = "kubenetes-uc1-${var.name}"

  network = var.network
  region  = "us-central1"

  ip_cidr_range = "10.4.0.0/22" # K8s nodes range

  secondary_ip_range {
    range_name    = "pods"
    ip_cidr_range = "10.0.0.0/14"
  }

  secondary_ip_range {
    range_name    = "services"
    ip_cidr_range = "10.4.4.0/22"
  }

  private_ip_google_access = true
}

resource "google_container_cluster" "uc1" {
  name = format("%s-uc1", var.name)

  network            = var.network
  subnetwork         = google_compute_subnetwork.k8s-cluster-uc1.self_link
  location           = "us-central1"
  node_locations     = ["us-central1-a", "us-central1-b"]
  min_master_version = var.master-version

  ip_allocation_policy {
    cluster_secondary_range_name  = "pods"
    services_secondary_range_name = "services"
  }

  network_policy {
    provider = "CALICO"
    enabled  = true
  }

  master_auth {
    client_certificate_config {
      issue_client_certificate = false
    }
  }

  remove_default_node_pool = true
  initial_node_count       = 1
}

resource "google_container_node_pool" "preemptible" {
  name    = "preemptible"
  cluster = google_container_cluster.uc1.name

  location   = "us-central1"
  version    = var.node-pool-version
  node_count = var.node-count

  node_config {
    preemptible  = true
    machine_type = "n1-standard-1"
    metadata = {
      disable-legacy-endpoints = "true"
    }
    oauth_scopes = [
      "https://www.googleapis.com/auth/devstorage.read_only",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
    ]
  }

  management {
    auto_upgrade = false
    auto_repair  = true
  }
}
