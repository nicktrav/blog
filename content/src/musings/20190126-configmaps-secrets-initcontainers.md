# `ConfigMaps`, `Secrets` and `InitContainers`

_January 26th, 2019_

New Year (kind of), new job, and another chance to say that I'm going to be
serious about writing longer form pieces of content for my site. The new job
part is probably the biggest motivation, as I'm going to be working extensively
with scalable cloud infrastructure built out on top of k8s. This which serves
as a great opportunity to write about what I'm learning along the way. Fun!

This post is going to detail a problem I had to work around this week related
to configuration changes and version control.

Specifically, the problem I was faced with was in a project that had a
directory structure like the following:

```bash
repo/
  config-file1.ini
  config-file2.ini
  deployment.yaml
```

The `config-file*.ini` files looked like the following: 


```toml
...

[database]
password = changeme
host = localhost

...
```

The `deployment.yaml` file represents a typical k8s
[`Deployment`](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/).
This case, the end result is simply printing out the contents of the
configuration files, to prove that we can see everything that we need to run
our app (i.e. the passwords):

```yaml
apiVersion: apps/v1
kind: Deployment
...
spec:
  ...
  template:
    spec:
      containers:
      - name: container-1
        image: busybox
        command: ['sh', '-c', 'cat /etc/config/*; sleep 100']
        volumeMounts:
        - name: secrets
          mountPath: /etc/config
          readOnly: true
      volumes:
        - name: secrets
          secret:
            secretName: passwords
            items:
            - key: config-file-1
              path: config1.ini
            - key: config-file-2
              path: config2.ini
```

The config files contained secret information that couldn't be checked in
(obviously), but it still had to be accessible to the k8s cluster. A
[`Secret`](https://kubernetes.io/docs/concepts/configuration/secret/) was used
to store the contents of the `.ini` files in their _entirety_. Herein lies the
problem!

One of the benefits (in theory) of having configuration checked into version
control, and having a solid CI pipeline is that when someone changes a
configuration file, a pipeline is triggered to build and redeploy everything.
In our case, this is done with [Helm](https://helm.sh/) and
[Jenkins](https://jenkins.io/).

Unfortunately, the `Secret` had been created manually,
using something like the following:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: my-secret
stringData:
  config-file-1: |
   ...
   [database]
   password = real-password-1
   host = localhost
   ...
  config-file-2: |
   ...
   [database]
   password = real-password-2
   host = localhost
   ...
type: Opaque
```

The password had been put into place for the purposes of creating the `Secret`
initially, and this had been stored in the cluster. The file had then been
deleted.

The problem arose when trying to alter some other configuration in the `.ini`
files (for example, changing the hostname for the database), and assuming that
the CI pipeline would push these changes out into the cluster. Given that the
configuration was being mapped into the containers from the static `Secret`,
getting the configuration change reflected would mean updating the Secret
_manually_, creating a new yaml file, like before and applying the change to
the cluster. Not ideal.

Here's a little solution I came up with instead, that at its core, relies on a
`ConfigMap` for the configuration _template_, a `Secret` for the passwords, and
an `InitContainer` to take the template and the passwords and populate a file
that could be used by the main container. Easy! ... That said, there were some
gotchas along the way though that I want to point out too.

## A minimal `Secret` for the passwords

The only "secret" information that is contained in the `.ini` files is the
password, so it makes more sense to make a `Secret` that contains just the
password.

Here's the config:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: passwords
stringData:
  password1: real-password-1
  password2: real-password-2
type: Opaque
```

These were then created in the cluster, and the files subsequently deleted,
like before.

Note that this definitely isn't best practice in terms of security. There are
safer and more reliable ways of creating secrets that don't rely on generating
files locally and storing them on disk temporarily. That's outside the scope of
this post though.

## `ConfigMaps` for configuration templates

With the passwords now in their own dedicated `Secret`, we can move the
configuration files out of the existing `Secret` and into a
[`ConfigMap`](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/),
which ended up looking something like this:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: templates
data:
  config-1: |
    [database]
    password = PASSWORD
    host = localhost
  config-2: |
    [database]
    password = PASSWORD
    host = localhost
```

Note that in this example, we end up no longer requiring the `.ini` files,
opting to have this configuration moved directly into the `ConfigMap`. In our
setup we're using Helm, which allows us to use their [templating
language](https://docs.helm.sh/chart_template_guide/) to inline the contents of
files directly into the yaml, so we can still have the `.ini` files. I'll leave
that for another post.

## `Volumes`

The eventual goal is to be able to write the passwords from the `Secret` into
the templates contained in the `ConfigMap`, which are mounted into the main
container.

Here's a first pass at the `deploment.yaml` for this:

```yaml
apiVersion: apps/v1
kind: Deployment
...
spec:
  ...
  template:
    spec:
      containers:
      - name: container-1
        image: busybox
        command: ['sh', '-c', 'cat /etc/config/*; sleep 100']
        volumeMounts:
        - name: templates
          mountPath: /etc/templates
          readOnly: true
        - name: secrets
          mountPath: /etc/secrets
          readOnly: true
      volumes:
        - name: templates
          configMap:
            name: templates
            items:
            - key: config-1
              path: config1.ini
            - key: config-2
              path: config2.ini
        - name: secrets
          secret:
            secretName: passwords
```

Even though we have the templates and passwords mounted into the container,
we're not actually doing anything with them here. We still need to combine
them. This is where the `InitContainers` are useful.

## `InitContainers`

[`InitContainers`](https://kubernetes.io/docs/concepts/workloads/pods/init-containers/)
allow you to start one or more containers _before_ the main containers start that
can do some kind of setup. This might be setting an environment variable, or
blocking on some condition before allowing the main containers to launch. In
our use-case, we're going to use them to put the password into the templates.

Here's what my second pass looked like:

```yaml
apiVersion: apps/v1
kind: Deployment
...
spec:
  ...
  template:
    spec:
      - name: init-password-1
        image: busybox
        command: ['sh', '-c', 'sed -i "s/PASSWORD/$(cat /etc/secrets/password-1)/" /etc/config/config1.ini']
        volumeMounts:
        - name: templates
          mountPath: /etc/config
          readOnly: false
        - name: secrets
          mountPath: /etc/secrets
          readOnly: true
      - name: init-password-2
        image: busybox
        command: ['sh', '-c', 'sed -i "s/PASSWORD/$(cat /etc/secrets/password-2)/" /etc/config/config2.ini']
        volumeMounts:
        - name: templates
          mountPath: /etc/templates
          readOnly: false
        - name: secrets
          mountPath: /etc/secrets
          readOnly: true
      containers:
      - name: container-1
        image: busybox
        command: ['sh', '-c', 'cat /etc/config/*; sleep 100']
        volumeMounts:
        - name: templates
          mountPath: /etc/templates
          readOnly: true
      volumes:
        - name: templates
          configMap:
            name: templates
            items:
            - key: config-1
              path: config1.ini
            - key: config-2
              path: config2.ini
        - name: secrets
          secret:
            secretName: passwords
```

We've used two `InitContainers` that mount in the `Secret` as read only into
`/etc/secrets`, and the `ConfigMap` as read-write into `/etc/config/` and then
inline the password. The same `ConfigMap` is then mounted into the main
container at `/etc/config`, and the container reads the contents of the updated
config files. Great!

Unfortunately, there's a subtle problem in that the `ConfigMap` that is mounted
into the `InitContainers` isn't actually read-write, even though we've asked
for it to be. This makes sense, given that it would be weird for the container
to make changes to the contents of the underlying `ConfigMap`. Are those
changes reflected in the map that's stored in the cluster?, etc., etc. The same
read only constraints apply to `Secrets`.

There's a nice explanation for it
[here](https://github.com/kubernetes/kubernetes/issues/62099).

## `emptyDir` volumes

With the `ConfigMap` and `Secret` being read-only mounts, we need a way to
generate the configuration and persist that somewhere temporarily and make that
accessible to the main container. We can use an
[`emptyDir`](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir)
volume for that!

Here's what the final configuration looked like:

```yaml
apiVersion: apps/v1
kind: Deployment
...
spec:
  ...
  template:
    spec:
      initContainers:
      - name: init-password-1
        image: busybox
        command: ['sh', '-c', 'sed "s/PASSWORD/$(cat /etc/secrets/password-1)/" /etc/templates/config1.ini.tmpl > /etc/config/config1.ini']
        volumeMounts:
        - name: templates
          mountPath: /etc/templates
          readOnly: true
        - name: secrets
          mountPath: /etc/secrets
          readOnly: true
        - name: configs
          mountPath: /etc/config
          readOnly: false
      - name: init-password-2
        image: busybox
        command: ['sh', '-c', 'sed "s/PASSWORD/$(cat /etc/secrets/password-2)/" /etc/templates/config2.ini.tmpl > /etc/config/config2.ini']
        volumeMounts:
        - name: templates
          mountPath: /etc/templates
          readOnly: true
        - name: secrets
          mountPath: /etc/secrets
          readOnly: true
        - name: configs
          mountPath: /etc/config
          readOnly: false
      containers:
      - name: container-1
        image: busybox
        command: ['sh', '-c', 'cat /etc/config/*; sleep 100']
        volumeMounts:
        - name: configs
          mountPath: /etc/config
          readOnly: true
      volumes:
        - name: templates
          configMap:
            name: templates
            items:
            - key: config-1
              path: config1.ini.tmpl
            - key: config-2
              path: config2.ini.tmpl
        - name: secrets
          secret:
            secretName: passwords
        - name: configs
          emptyDir: {}
```

We're now mounting the `emptyDir` volume into each of the `InitContainers` as
read-write and inlining the passwords and into templates and persisting that
into the `emptyDir`, which is then made accessible to the main container. The
container only has read-only access to the final configuration file, so there's
no chance it can try and alter the contents once they are written.

Here's the final output, that proves we wired it all up correctly:

```bash
$ kubectl get pods
NAME                            READY     STATUS    RESTARTS   AGE
deployment-6d8db67956-4vnzr   1/1       Running   1          1m

$ kubectl logs deployment-6d8db67956-4vnzr
[database]
password = real-password-1
host = localhost
[database]
password = real-password-2
host = localhost
```

And done!

## Using tmpfs for secrets

I mentioned as an aside above that it's usually not the best idea to persist
passwords or key material to disk, and that's what we're doing here. That said,
if you're using an `emptyDir`, we can tell it to use an in-memory tmpfs as the
storage medium, which is much safer. To do that, we alter the volume definition
in the `Deployment` as follows:

```
volumes:
...
- name: configs
  emptyDir:
    medium: Memory
```

And prove to ourselves that it worked:

```bash
$ kubectl get pods
NAME                            READY     STATUS    RESTARTS   AGE
deployment-5-6f7c6c7787-95vtr   1/1       Running   1          1m

$ kubectl exec -it deployment-5-6f7c6c7787-95vtr -- /bin/sh -c "df -h | grep '/etc/config'"
tmpfs                     1.8G      8.0K      1.8G   0% /etc/config
```

## Summary

So that's how we can use `ConfigMaps`, `Secrets` and `InitContainers` to enable
us to combine secret information such as passwords with static configuration
that is checked into version control and ensuring that everything is updated
when it changes, rather than manually updating `Secret`s.
