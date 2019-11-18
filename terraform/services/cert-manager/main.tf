resource "google_service_account" "cert-manager" {
  account_id   = "cert-manager-${var.name-suffix}"
  display_name = "cert-manager-${var.name-suffix}"
  description  = "Allows the altering of CloudDNS records"
}

resource "google_project_iam_member" "dns-admin" {
  role   = "roles/dns.admin"
  member = "serviceAccount:${google_service_account.cert-manager.email}"
}
