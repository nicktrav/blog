terraform {
  required_version = "0.12.10"
  backend "gcs" {
    bucket = "nicktravers-site-tf-state-production"
    prefix = "terraform/state"
  }
}

provider "google" {
  project = "nicktravers-site"
  version = "2.20.0"
}
