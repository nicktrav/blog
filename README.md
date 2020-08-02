# nicktrave.rs

My website, accessible at [blog.nicktrave.rs](https://nicktrave.rs).

## Cluster setup

### Certificates

First, ensure you have [Helm](https://helm.sh).

Create a Cloudflare origin certificate for the domain and create a TLS Secret
from it.

```
$ kubectl create secret tls envoy \
  --key=/dev/shm/key.pem \
  --cert=/dev/shm/cert.pem
```

## Development

Run the site locally with the following:

```bash
$ docker run --rm -it 
..
```

## Staging (GCP)

The site will deploy to staging automatically when a change is pushed to the
`staging` branch.

A deploy can be initiated manually with the following:

View actions [here](https://github.com/nicktrav/blog/actions?query=workflow%3A%22Staging+release%22).

```bash
GCP_KEY_FILE=/path/to/service/account/key.json
$ ./deploy/deploy.sh staging staging
```

## Production (GCP)

The site is deployed to Production automatically when a change lands on the
main branch.

View actions [here](https://github.com/nicktrav/blog/actions?query=workflow%3A%22Production+release%22).

A deploy can be initiated manually with the following:

```bash
GCP_KEY_FILE=/path/to/service/account/key.json
$ ./deploy/deploy.sh prod $(git rev-parse HEAD)
```
