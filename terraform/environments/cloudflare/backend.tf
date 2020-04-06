terraform {
  required_version = "0.12.24"
  backend "gcs" {
    bucket = "nicktravers-site-tf-dns"
    prefix = "terraform/state"
  }
}

provider "cloudflare" {
  version   = "2.5.1"
  api_token = "HDOVO4qVYliM_XrqwPd6xEDHk9WaYnv6NdEW9fmo"
}

provider "google" {
  project = "nicktravers-site"
  version = "2.20.0"
}
