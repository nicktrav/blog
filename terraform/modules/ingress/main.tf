resource "google_compute_global_address" "ingress" {
  name = "site-ingress-${var.name-suffix}"
}
