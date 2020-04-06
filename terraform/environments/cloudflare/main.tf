// Zone

resource cloudflare_zone nicktrave-rs {
  zone = "nicktrave.rs"
}

resource cloudflare_zone_settings_override nicktrave-rs {
  zone_id = cloudflare_zone.nicktrave-rs.id

  settings {
    automatic_https_rewrites = "on"
    brotli                   = "on"
    min_tls_version          = "1.3"
    security_level           = "high"
    ssl                      = "full"
    tls_1_3                  = "on"
  }
}

// A

data google_compute_global_address lb-prod {
  name = "site-ingress-production"
}

resource cloudflare_record site-prod {
  zone_id = cloudflare_zone.nicktrave-rs.id
  type    = "A"
  name    = "nicktrave.rs" // apex
  value   = data.google_compute_global_address.lb-prod.address
  proxied = true
}

data google_compute_global_address lb-staging {
  name = "site-ingress-staging"
}

resource cloudflare_record site-staging {
  zone_id = cloudflare_zone.nicktrave-rs.id
  type    = "A"
  name    = "stage"
  value   = data.google_compute_global_address.lb-staging.address
  proxied = true
}

// MX

resource cloudflare_record mx-1 {
  zone_id  = cloudflare_zone.nicktrave-rs.id
  type     = "MX"
  name     = "nicktrave.rs"
  value    = "aspmx.l.google.com"
  priority = 1
}

resource cloudflare_record mx-5-1 {
  zone_id  = cloudflare_zone.nicktrave-rs.id
  type     = "MX"
  name     = "nicktrave.rs"
  value    = "alt1.aspmx.l.google.com"
  priority = 5
}

resource cloudflare_record mx-5-2 {
  zone_id  = cloudflare_zone.nicktrave-rs.id
  type     = "MX"
  name     = "nicktrave.rs"
  value    = "alt2.aspmx.l.google.com"
  priority = 5
}

resource cloudflare_record mx-10-1 {
  zone_id  = cloudflare_zone.nicktrave-rs.id
  type     = "MX"
  name     = "nicktrave.rs"
  value    = "alt3.aspmx.l.google.com"
  priority = 10
}

resource cloudflare_record mx-10-2 {
  zone_id  = cloudflare_zone.nicktrave-rs.id
  type     = "MX"
  name     = "nicktrave.rs"
  value    = "alt4.aspmx.l.google.com"
  priority = 10
}

// TXT

resource cloudflare_record keybase {
  zone_id = cloudflare_zone.nicktrave-rs.id
  type    = "TXT"
  name    = "_keybase"
  value   = "keybase-site-verification=-aV0m7W9FJWZvi5XXyXTCJoIPtifbm1PvmbF3ie6wbU"
}
