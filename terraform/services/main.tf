module "cert-manager" {
  source = "./cert-manager"

  name-suffix = var.name-suffix
}

module "site" {
  source = "./blog"

  name-suffix   = var.name-suffix
  site-dns-name = var.site-dns-name
}
