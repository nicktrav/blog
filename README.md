# nicktrave.rs

My website, accessible at [nicktrave.rs](https://nicktrave.rs).

## Local deployment (standalone)

The webserver can be started with the following:

```bash
$ mvn clean package
$ java -jar webserver/target/server.jar
$ open http://localhost:9000
```

## Local deployment (cluster)

Similar to the steps for the site deployed on GCP, except that it uses Minikube
to set up a Kubernetes cluster locally.

Relies on having minikube and kubectl installed:

```bash
# minikube
$ brew cask install minikube

# kubectl (as per https://kubernetes.io/docs/tasks/tools/install-kubectl/)
$ curl -lo https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/darwin/amd64/kubectl
$ chmod +x ./kubectl
$ sudo mv ./kubectl /usr/local/bin/kubectl
```

Make sure minikube is up and running and that you are pointed at the docker
engine in the minikube environment:

```bash
# Start minikube - this can take some time.
$ minikube start

# Make sure kubectl is talking to the local kubernetes cluster
$ kubectl config use-context minikube

# Connect to minikube's docker engine
$ eval $(minikube docker-env)
```

Build the containers to run on the local cluster, if not done already:

```bash
$ ./bin/build-local
```

This will compile the webserver, and build the server and nginx containers and
push them to the docker registry in minikube.

Check the images that are available:

```bash
$ docker images | grep nickt/blog
```

Start a deployment for the SHA you're interested in. Update
`kube/local/deployment.yaml` with the desired image tags, if required.

```bash
$ kubectl create -f ./kube/local/deployment.yaml
```

Expose a service for the deployment:

```bash
$ kubectl expose deployment deployment-blog-dev \
   --name=service-blog-dev \
   --type=NodePort
```

The site should now be available at:

```bash
$ open $(minikube service service-blog-dev --url)
```

Changes can be made to the `deployment.yaml` file and the synced with the
cluster:

```bash
$ kubectl apply -f ./minikube/local/deployment.yaml
```

If you want to connect back to the local docker (i.e. the one that is _not_ in
minikube), run the following:

```bash
$ eval $(docker-machine env -u)
```

## Cluster setup

### Generate certificates

First, ensure Helm has been installed:

```
# Install
$ brew install kubernetes-helm
$ kubectl apply -f helm/tiller-rbac.yaml
$ helm init --service-account tiller

# Check for Tiller
$ kubectl -n kube-system get pods -l name=tiller
```

Install [cert-manager](https://github.com/jetstack/cert-manager).

```
# Install the CustomResourceDefinition resources separately
$ kubectl apply \
  --validate=false \
  -f https://raw.githubusercontent.com/jetstack/cert-manager/release-0.11/deploy/manifests/00-crds.yaml

# Create the namespace for cert-manager
$ kubectl create namespace cert-manager

# Add the Jetstack Helm repository
$ helm repo add jetstack https://charts.jetstack.io

# Update your local Helm chart repository cache
$ helm repo update

# Install the cert-manager Helm chart
$ helm install \
  --name cert-manager \
  --namespace cert-manager \
  --version v0.11.0 \
  jetstack/cert-manager
```

Set up a service account that has DNS Admin privileges. Download the key
locally, and add it to the cluster:

```
$ kubectl create secret generic dns \
    --namespace=cert-manager \
    --from-file=/path/to/service-account.json
$ kubectl describe secret dns --namespace cert-manager
```

Deploy Helm chart:

```
$ helm upgrade blog helm/ --values helm/values-staging.yaml --install
```

## Staging (GCP)

Follow the steps outlined in the [staging README](kube/gcp/staging/README.md)
to get a cluster up and running.

To test out changes that have yet to be merged to master, use the following:

```bash
$ gcloud builds submit \
  --config cloudbuild-staging.yaml .
```

This will build set of containers with for the current SHA, and also labeled
with `stage`.

## Production (GCP)

Follow the steps outlined in the [production
README](kube/gcp/production/README.md) to get a cluster up and running.
