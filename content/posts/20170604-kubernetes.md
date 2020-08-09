# Kubernetes and SSL

 key  | value   
 ---  | ---      
name  | 2017-06-04-kubernetes

_June 4th, 2017_

In 2017 it's a little scary to have to browse to a website that isn't "secure".
By secure I mean that it's not using
[HTTPS](https://en.wikipedia.org/wiki/HTTPS") to encrypt the traffic to and
from the client (your browser) to the server (some boxes in a datacenter
somewhere). Even for sites like this, which are serving purely static content,
it's enough to [get your side pushed down in search engine
rankings]("https://webmasters.googleblog.com/2014/08/https-as-ranking-signal.html)
Not that I'm one for rankings, but I spent part of my weekend working out how
I'd go about getting my site (more) secure as an exercise in familiarising
oneself with some of the new Kubernetes features.

## SSL Certificates via Let's Encrypt

[Let's Encrypt](https://letsencrypt.org/") describe themselves as a free and
automated [Certificate
Authority](https://en.wikipedia.org/wiki/Certificate_authority) (or CA).  They
can generate certificates for you which you stick in your webservers to ensure
that you get that nice little "Secure" label next to your site's URLs in
Chrome.

While I could roll my own automation for certificate generation via Let's
Encrypt and renew them periodically (certificates are valid for 90 days), I'm
using Kubernetes to manage the deployment of my site, so it makes sense to see
what out there already and make use of someone else's hard work.

The top search result for _"kubernetes letsencrypt"_ is a Github project from a
dude called Kelsey Hightower. Kelsey works for Google and does ton of awesome
work in the Kubernetes. His
[kube-cert-manager](https://github.com/kelseyhightower/kube-cert-manager) adds
some extra functionality to your Kube cluster that lets you manage your Let's
Encrypt certificates. The README is a good place to start, but I've summarized
the main parts.

The following sections are adapted from Kelsey's repo. Full props to him.

## Setup

Create a third-party resource that models a "Certificate" resource:

```bash
$ kubectl create -f certificate.yaml
```

Where the contents of the YAML is as follows:

```yaml
apiVersion: extensions/v1beta1
kind: ThirdPartyResource
description: "A specification of a Let's Encrypt Certificate to manage."
metadata:
  name: "certificate.stable.hightower.com"
versions:
  - name: v1
</code></pre>
```

You'll need a persistent disk resource to store the certificates that the tool
generates:

```bash
$ gcloud compute disks create kube-cert-manager --size 10GB
```

The cert manager also needs to be able to create / delete DNS records for your
GCP project. This allows you to prove to Let's Encrypt that you're the one in
control of the site. More on that process (the ACME protocol)
[here](https://ietf-wg-acme.github.io/acme/).  Create a new service account
that has admin access to DNS for your project. Save that somewhere locally.

Create a secret in your Kubernetes cluster from this secret:

```bash
# Create
$ kubectl create secret generic your-site-dns \
  --from-file=path/to/your/service-account.json

# Verify
$ kubectl describe secret nicktrav-site-dns
```

## Deploy

The cert manager can now be deployed with the following:

```bash
$ kubectl create -f kube-cert-manager.yaml
```

Where the content of the file is as follows:

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  labels:
    app: kube-cert-manager
  name: kube-cert-manager
spec:
  replicas: 1
  template:
    metadata:
      labels:
        app: kube-cert-manager
      name: kube-cert-manager
    spec:
      containers:
        - name: kube-cert-manager
          image: gcr.io/hightowerlabs/kube-cert-manager:0.5.0
          imagePullPolicy: Always
          args:
            - "-data-dir=/var/lib/cert-manager"
            - "-acme-url=https://acme-v01.api.letsencrypt.org/directory"
            - "-sync-interval=30"
          volumeMounts:
            - name: data
              mountPath: /var/lib/cert-manager
        - name: kubectl
          image: gcr.io/google_containers/hyperkube:v1.5.2
          command:
            - "/hyperkube"
          args:
            - "kubectl"
            - "proxy"
      volumes:
        - name: "data"
          emptyDir: {}
```

Watch the deployment of this pod with:

```bash
$ kubectl describe pod kube-cert-manager
```

One this is up and running, you're good to start generating certificate
resources.

## Generate

Certificate resources can be created by moulding the following to suit your
requirements. Place in a file (in this case `your-site-dot-com.yaml`).

```yaml
apiVersion: "stable.hightower.com/v1"
kind: "Certificate"
metadata:
  name: "something-descriptive"
spec:
  domain: "your.domain.com"
  email: "you@email.com"
  provider: "googledns"
  secret: "service-account-secret"  # This was named your-site-dns above
  secretKey: "service-account.json"
```

The most important part is the domain. In my case, I have my main site, as well
as a staging domain, so I created a certificate for each (`stage.nicktrave.rs`
as well as `nicktrave.rs`). Note that you could probably wildcard your domain
you don't have to generate as many certificates. I haven't experimented with
this.

Generate the certificates:

```bash
$ kubectl create -f your-site-dot-com.yaml
```

At this point if you tail the logs of the `kube-cert-manager` pod you'll see
that it's requesting certificates for you from Let's Encrypt. Part of this
process (as alluded to earlier) is to add some DNS records so that Let's
Encrypt can verify that it's actually you making these requests for certs.
Given that you have control of your site's DNS, Let's Encrypt will respond to a
request for a certificate by asking you to create a DNS record.  You go ahead
and do this, Let's Encrypt checks that the DNS records it asked for have been
created, and then goes ahead and sends you the cert.

## Confirm

If `kube-cert-manager` was successful, it will create a new secret for each
domain that you requested. You can list the certificates and secrets via:

```bash
$ kubectl get certificates
$ kubectl get secrets
```

Inspecting each secret you'll notice that there's a `.key` and `.crt` file
inside. These are what you'll provide the load balancer for it to set up SSL
termination.

## TLS Termination

By far the easiest way of securing your site that runs on GCP is to place the
certificates in Google's L7 load balancer. If you're running on Kubernetes, you
can use an [Ingress
resource](https://kubernetes.io/docs/concepts/services-networking/ingress/)
resource to manage this for you. The details, with extensive examples can be
found [here](https://github.com/kubernetes/ingress/tree/master/examples), but
the TLDR is that you need to create a new
[Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/),
which you expose via
[Service](https://kubernetes.io/docs/concepts/services-networking/service/).
Then you use an Ingress to tell Google's LBs about it. The Ingress also defines
the certificate that you want to use, as well as the hostnames that it will
cover. Here's an example:

```yaml
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: ingress-your-site
  annotations:
    kubernetes.io/ingress.global-static-ip-name: my-site
spec:
  tls:
   - secretName: your.site.com
  backend:
    serviceName: service-for-your-site
    servicePort: 80
```

Note the extra annotation there that tells Kubernetes that I want this Ingress
to be exposed to the outside world via a static global IP. I've got an
additional A-record in my DNS entries that resolve to this IP.

After creating the Ingress, it takes a while for it to create the entries in
the load balancers.  The load balancers use health-checks to check to see that
your backends are healthy. You can see these under Compute / Health Checks.

And voila! Now your site should be talking HTTPS. Depending on how your
backends are set up, you'll probably be handling HTTP too. More on how to deal
with that now.

## Extras

While my site uses HTTPS now, that's only the case if you explicitly _ask_ to
speak HTTPs.  If a client was to ask for the same content from a `http://`
resource, the backend would still serve it up to them. Because I'm using Nginx
as a reverse-proxy in front of my backends, there's a [nice little
trick](https://github.com/kubernetes/ingress/tree/master/controllers/gce#redirecting-http-to-https)
you can use to tell any request for http content to redirect to https.

Place the following in your `nginx.conf`:

```nginx
if ($http_x_forwarded_proto = "http") {
  return 301 https://$host$request_uri;
}
```

This works because Google's load balancers set the
[X-forwarded-proto](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Proto)
header on each request. Nginx will examine these headers and upon seeing HTTP
as the request protocol it responds to the load balancer with a 301 [moved
permanently](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/301).
This is considered to be a best practice for upgrading users from HTTP to
HTTPS.

## Next steps

While traffic to and from site is now much more secure and clients know they
are talking to servers who are who they say their are, my site still isn't as
secure as I'd like it to be.  Traffic is only encrypted _up to Google's load
balancer_. While I have ample faith in Google's ability to handle my boring
content traffic within their own datacenters, an even safer solution would be
to encrypt traffic _between the LBs and my webservers_ (nginx). Traffic from
the webserver to the application server is, in my case, on the same VM, so it's
less important for it to be encrypted for the last stage of its journey.

I made a fruitless attempt to get the LBs to talk to my backends using HTTPS,
but it looks like [this is a shortcoming](https://github.com/kubernetes/ingress/tree/master/controllers/gce#wishlist).

While Google's load balancers are easy to use, they are also pretty expensive
(roughly $20 a month for a single rule). An alternative approach would be to
place an
[nginx-ingress-controller](https://github.com/kubernetes/ingress/tree/master/controllers/nginx)
in front of your services, which obviates the need for Google's Load Balancer
altogether. That's some more material for another post.
