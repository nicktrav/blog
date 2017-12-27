# Setting up the cluster

## Generate certificates

Set up a service account that has DNS Admin privileges. Download the key
locally, and add it to the cluster:

```
$ kubectl create secret generic dns \
    --from-file=/path/to/service-account.json
$ kubectl describe secret dns
```

Create a persistent volume for the certificates:

```
$ gcloud compute disks create kube-cert-manager-staging --size 10GB
```

Create the "Certificate" abstraction:

```
$ kubectl create -f kube/gcp/staging/certs/certificate.yaml
```

Deploy the certificate manager:

```
$ kubectl create -f kube/gcp/staging/certs/kube-cert-manager.yaml
```

Generate TLS certificate and key:

```
$ kubectl create -f kube/gcp/staging/certs/tls-site.yaml
```

Create a Diffie-Hellman group to use, and upload as a secret:

```
$ sudo openssl dhparam -out dhparam.pem 2048
$ kubectl create secret generic tls-dhparam --from-file=dhparam.pem
$ rm dhparam.pem
```

## Backends

### Webserver

Deploy the webserver and nginx backend. Be sure to update the `SHA` to the
latest version:

```
gcloud container images list-tags gcr.io/nicktravers-site/server
gcloud container images list-tags gcr.io/nicktravers-site/nginx
```

```
$ kubectl create -f kube/gcp/staging/webserver.yaml
```

### Load balancer

Set up a default backend to serve 404s for missing routes:

```
$ kubectl create -f kube/gcp/staging/load-balancer/default-backend.yaml
```

Deploy the nginx-controller. Be sure to update the IP to a previously reserved
static IP.

```
$ kubectl create -f kube/gcp/staging/load-balancer/nginx-controller.yaml
```

Deploy the ingress rules to map traffic on all routes to the webserver backend.

```
$ kubectl create -f kube/gcp/staging/load-balancer/ingress.yaml
```
