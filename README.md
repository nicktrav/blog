# nicktrave.rs

// TODO(nickt)

## Production deployment (GCP)

### Initial setup

1. Connect to the cluster

  ```bash
  $ gcloud container clusters get-credentials site \
      --zone ZONE --project PROJECT
  ```

2. Set up a deployment

  ```bash
  $ kubectl create -f ./kube/gcp/deployment-prod.yaml
  ```

3. Expose the service

  ```bash
  $ kubectl expose deployment deployment-blog-prod \
      --name=service-blog-prod \
      --type=NodePort
  ```

3. Allow ingress traffic to the service via a loadbalancer

  ```bash
  $ kubectl create -f ./kube/gcp/ingress-prod.yaml
  ```

This takes a while to expose the service the first time. Monitor progress with

  ```bash
  $ kubectl describe ingress ingress-blog-prod
  ```

### Updates

Connect to the cluster:

```bash
$ gcloud container clusters get-credentials site \
    --zone $ZONE --project $PROJECT
```

Ensure an image exists for the SHA that needs to be deployed:

```bash
$ PROJECT=$(gcloud config list | grep -E '^project' | sed -E 's/project = (.*)/\1/')
$ gcloud container images list-tags gcr.io/$PROJECT/server
$ gcloud container images list-tags gcr.io/$PROJECT/nginx
```

Check status of deployment:

```bash
$ kubectl describe deployment deployment-blog-prod
```

Update the deployment with the new image(s):

```bash
# Webserver
$ kubectl set image deployment/deployment-blog-prod nginx=gcr.io/$PROJECT/nginx:SHA

# Nginx
$ kubectl set image deployment/deployment-blog-prod server=gcr.io/$PROJECT/server:SHA
```

Monitor status of the rollout with:

```bash
$ kubectl get deployment deployment-blog-prod
$ kubectl describe deployment deployment-blog-prod
```

## Local deployment (standalone)

The webserver can be started with the following:

```bash
$ sbt ~run
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
$ kubectl apply -f ./kube/local/deployment.yaml
```

If you want to connect back to the local docker (i.e. the one that is _not_ in
minikube), run the following:

```bash
$ eval $(docker-machine env -u)
```
