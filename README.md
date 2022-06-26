# nicktrave.rs

My website, accessible at [nicktrave.rs](https://nicktrave.rs).

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

### Google Container Registry (GCR)

In order for the kubelet to pull the container images from Google Cloud, a
Secret needs to exist with the Docker pull credentials.

Fetch the key from the appropriate service account (i.e. prod / staging):

```bash
# E.g. for staging.
$ gcloud iam service-accounts keys create \
  /dev/shm/key.json \
  --iam-account=site-gcr-reader-staging@$GOOGLE_CLOUD_PROJECT.iam.gserviceaccount.com
```

Create the K8s Secret:

```bash
$ kubectl create secret docker-registry gcr \
  --docker-server https://gcr.io \
  --docker-username=_json_key \
  --docker-email=user@example.com \
  --docker-password="$(cat /dev/shm/key.json)"
```

## Development

Run the site locally with the following:

```bash
$ make run-docker
```

Open the page at [`http://localhost:3000`](http://localhost:3000).

## Staging (GCP)

The site will deploy to staging automatically when a change is pushed to the
`staging` branch.

A deploy can be initiated manually with the following:

View actions [here](https://github.com/nicktrav/blog/actions?query=workflow%3A%22Staging+release%22).

```bash
$ export GCP_PROJECT_ID=...
$ export=DIGITAL_OCEAN_TOKEN=...
$ ./deploy/deploy.sh staging staging
```

## Production (GCP)

The site is deployed to Production automatically when a change lands on the
main branch.

View actions [here](https://github.com/nicktrav/blog/actions?query=workflow%3A%22Production+release%22).

A deploy can be initiated manually with the following:

```bash
$ export GCP_PROJECT_ID=...
$ export=DIGITAL_OCEAN_TOKEN=...
$ ./deploy/deploy.sh prod $(git rev-parse HEAD)
```
