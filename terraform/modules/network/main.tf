resource "google_compute_network" "network" {
  name = var.name

  routing_mode            = "GLOBAL"
  auto_create_subnetworks = false
}
