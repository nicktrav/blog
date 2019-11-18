resource "google_compute_global_address" "ingress" {
  name = "site-ingress-${var.name-suffix}"
}

data "google_dns_managed_zone" "site" {
  name = "site"
}

resource "google_dns_record_set" "ingress" {
  managed_zone = data.google_dns_managed_zone.site.name
  name         = var.dns-name
  type         = "A"
  rrdatas      = [google_compute_global_address.ingress.address]
  ttl          = 60
}
