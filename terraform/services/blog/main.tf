module "ingress" {
  source = "../../modules/ingress"

  name-suffix = var.name-suffix
  dns-name    = var.site-dns-name
}
