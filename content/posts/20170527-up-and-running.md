# Up and running!

 key  | value
 ---  | ---
name  | 2017-05-27-up-and-running

_May 27th, 2017_

It took me a little while, but I finally got this thing up and running!
_Hello, world!?_

I thought building a site was going to be easy. Sure, it's simple to build
something to host static pages built on top of WordPress in PHP, but that's so
mid-2000's [LAMP](https://en.wikipedia.org/wiki/LAMP_\(software_bundle\))
anyone?!).  I was up for more of a challenge when I started building this, so I
picked some building blocks that satisfied the following:

- *(Relative) Simplicity* - I needed to be able to hack something together
  quickly and be able to iterate on it easily without my house of cards falling
  over

- *Familiarity* - I wanted to write some code in a language that have a passing
  familiarity with

- *Experimental* - part of putting this together was an excuse to pick up some
  new tools and concepts that I don't have a chance to work with in my day-job

- *Open source* - there's nothing worse that not being able to crack something
  open and see how it works. All the buidling blocks of the site need to be
  available to anyone else to pick up and use

## Backend

A site (usually) needs a webserver backing it. Something to serve up the
content to the clients that connect to it. While the initial goal is to serve
up static content, I wanted to use somthing that was going to be a little more
versatile and flexible if I wanted to experiment with something fancier in the
future.

I use Java at work, so I'm confident working in the JVM ecosystem. The tooling
and support is pretty extensive and mature, and it's easy to StackOverflow your
way out of a problem if / when you get stuck. I've been learning
[Scala](https://www.scala-lang.org) on the side for a while as an excuse to get
my head out of the "everything is a noun" approach to software development in
Java
[this](http://steve-yegge.blogspot.com/2006/03/execution-in-kingdom-of-nouns.html)
if you haven't already), so wanted to get my teeth sunk into a decent project.

Googling "Scala" and "web development" will probably lead you to the [Play
framework](https://www.playframework.com/). It's pretty easy to use if you're
like me and have used another MVC framework like
[Rails](http://rubyonrails.org/). Given that you're writing Scala, it's all
statically typed, incuding the HTML templates. Less of those gross runtime
errors.

I've recently started doing some Golang at work, so maybe if I get sick of Play
I might have a play around with porting some of this to Go. Who says I can't be
irresponsible and do a re-write when I want to?!

I've slapped [Nginx](https://nginx.org/en/) in front of the backends as a
reverse proxy. It's 2017 and it's better than Apache. Sure, I'm not serving up
thousands of requests a second, but we use it at work so I know a thing or two
about how to configure it. It can cache things nicely, and terminate SSL (when
I get around to it!).

## Frontend

While HTML was probably the "language" that got me into programming at an early
age, building small web pages that I could run on the computer I'd built from
parts I'd picked up at garage sales in the [small country town in which I grew
up](https://en.wikipedia.org/wiki/Bridgetown,_Western_Australia) (represent!),
it's never really been a strong point. Javascript is gross.  *\#sorrynotsorry*.

Every week there's a new framework on the Orange Website that all the cool kids
are using. It's exhausting to keep up with. So I'm sticking to my roots here
and the content for the site is nothing fancy. It's just static HTML with some
CSS to make things look pretty.

Given the backend is Play / Scala, maybe I'll get around to checking out
something like [scala.js](https://www.scala-js.org/) ... maybe.

## Hosting

I've always been a bit of a Google fan-boy, and have used
[GCP](https://cloud.google.com/) for a while now, both at work and for various
side projects, so this was a pretty easy choice. That said, you could probably
throw this site up on AWS or Digial Ocean pretty easily.

I like how easy and fast it is to iterate on GCP relative to the competition.
I'm slowly coming around to the idea of "immutable infrastructure", where you
can treat your VMs as immutable, expendable and ephemeral. If something stops
working, spin up a replacement and keep moving.
[GCE](https://cloud.google.com/products/compute/) is pretty good for this, and
you can have an instance up and SSH-able in less than a minute.

## Deployment

Ask any hipster what they're using these days for deploying their artisinal,
hand-rolled, functional, stateless, reactive microservices and they'll probably
drop the C-word. "Containers" are pretty much synonomous with
[Docker](https://www.docker.com/) these days (although obviously there are
others - see [LXC](https://linuxcontainers.org/) ad and
[Rkt](https://coreos.com/rkt) for two notable examples). Using Docker
containers unlocks cool frameworks like [Kubernetes](https://kubernetes.io/)
for service discovery and deployment. We've got a pretty similar framework at
Square, [p2](https://github.com/square/p2) that is _heavily_ inspired by
Kubernetes, so the concepts and abstractions are familiar to me, which is a
bonus.

Google has a hosted Kubernetes service called [Google Container Engine
(GKE)](https://cloud.google.com/container-engine/) which abstracts away the
pain of adding and removing VMs from your cluster and getting them talking to
one another.

Kubernetes is another tool I can add to the tool-belt. A bunch of companies are
already using it (probably in production, but maybe they'd be reluctant to
admit that openly). The open source community is huge, and the project is
constantly evolving. No doubt things will look different in a month's time.
There will be different abstractions to use. Hey - if I ever end up with a
database backing this thing (god forbid), I'll be keen to checkout how
Kubernetes deals with
[StatefulSets](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/).

So ... here we are. We've got the broad strokes of a website engraved in a
single HTML document, [checked into a Github
repo](https://github.com/nicktrav/blog), building and deploying on GCP. I know
it works coz you're reading it, and I know a thing or two from a past life on
how to scrape and analyze web logs to tell what kind of browser you're reading
it from and where you are! ... or at least where the IP of your Tor exit node
appears to be.

Stay tuned ... my creative juices have to refill. Writing is hard.

_-nt_
