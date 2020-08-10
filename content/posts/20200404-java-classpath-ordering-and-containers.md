# Java classpaths, containers and Kubernetes

 key  | value
 ---  | ---
name  | 2020-04-04-java-classpath

_April 4, 2020_

Here's a weird bug I had the pleasure of digging into. Nothing like being
trapped at home during a pandemic to force you into technical writing!

It started with a bunch of Kubernetes Pods that were stuck in a crash loop. The
main container in the Pod runs a Java app, and these were crashing due to a
missing field in a gRPC class:

```bash
Exception in thread "main" java.lang.NoSuchFieldError: STUB_TYPE_OPTION
  at io.grpc.stub.AbstractBlockingStub.newStub(AbstractBlockingStub.java:64) at
  at io.grpc.stub.AbstractBlockingStub.newStub(AbstractBlockingStub.java:51) at 
```

Weirdly, this was only happening in our production environment, and not
staging, even though the same container image was being run across all of our
clusters.

Most of the time with Java, crashes like these are due to some weirdness in the
way dependencies end up on the classpath. When the JVM boots, the order jars on
the classpath determines which class is used at runtime. In the case that there
are two jars for the same library, but for different versions of the library,
the class from the first jar encountered by the class loader wins.

In our case, it turns out that a recent change dependencies had pulled in a
"fat jar", a monolithic jar published by another library that bundled all of
its dependencies together. Unfortunately, this jar contained an older version
of gRPC. This fat jar was working its way onto the classpath _earlier_ than the
jar for the desired gRPC version. As code executed, it would pull uncached
classes from the fat jar, rather than from the desired jars.

I rebuilt the container with a simple class that would print out the classpath
order as well as the order in which classes were being loaded by the
classloader at runtime. This required some reflection grossness to reach inside
the class loader to pull out the `Vector<Class<?>>`:

```java
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Vector;

class ClasspathPrinter {

  static void print() {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    URLClassLoader uCl = (URLClassLoader) ClassLoader.getSystemClassLoader();

    System.out.println("---------- CLASSPATH ----------");
    for (URL clURL : uCl.getURLs()) {
      System.out.println(clURL);
    }
    System.out.println("---------- CLASSPATH ----------");

    Field clClassVector;
    try {
      clClassVector = ClassLoader.class.getDeclaredField("classes");
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("cloud not access private field:", e);
    }
    clClassVector.setAccessible(true);

    Vector<Class<?>> classes;
    try {
      classes = (Vector<Class<?>>) clClassVector.get(cl);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("cloud not access class:", e);
    }

    System.out.println("---------- CLASSES ----------");
    for (Class<?> klass : classes) {
      System.out.println(klass);
    }
    System.out.println("---------- CLASSES ----------");
  }
}
```

From this we confirmed that the fat jar was ending up on the classpath earlier
in the case where the container was broken. The class being pulled in was from
gRPC 1.22.0 and was missing the following code from the version 1.28.0 jar:

```java
  /**
   * Internal {@link CallOptions.Key} to indicate stub types.
   */
  static final CallOptions.Key<StubType> STUB_TYPE_OPTION =
      CallOptions.Key.create("internal-stub-type");
```

This lines up with the original stack trace.

As a brief side note, it was interesting to compare the output of the `CLASSES`
section of the output, as it highlights the way in which the JVM class loader
recursively loads the classes needed for execution. You can trace through the
execution in terms of what classes are loaded:

```
class com.example.Main
class org.slf4j.LoggerFactory
interface org.slf4j.ILoggerFactory
interface org.slf4j.event.LoggingEvent
class org.slf4j.helpers.SubstituteLoggerFactory
interface org.slf4j.Logger
... etc.
```

From the main class, the first code we call is a static method to set up
logging:

```java
public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
```

The contents of the LoggerFactory are loaded, and the code continues to
execute, and eventually reaches a definition for an interface it doesn't have
loaded yet in the SLF4J code (`ILoggerFactory`). The interface doesn't have
anything else to load, so the classloader keeps executing until it encounters
the next class it doesn't know about. This happens recursively.

The issue with the fat jar being present earlier on the classpath caused the
incorrect classes to be cached by the classloader earlier on. When code from a
more recent version of gRPC tried to execute, it called into an older class
which was missing the field it needed, resulting in the runtime exception.

The simple fix for this issue was to eliminate the need for the fat jar, which
was within our control.

However, at this point we were still curious as to why this worked in some of
our clusters and not others. We had to dive deeper to solve this particular
mystery.

The containers that were crashing in our production clusters were running on
different version of Kubernetes (GKE) than the containers that were not failing
in our staging environment. But why would that affect whether or not a
particular version of class file was loaded at runtime?

Comparing the output from the `ClasspathPrinter`, we noticed that the even
though the containers contained the same jars on the classpath, the _order_ in
which there were being loaded was dependent on which cluster they run in.

In our case, were were telling the JVM to include classes from various paths
via the `-cp` (or classpath) directive. This parameter was passed a _wildcard_
path as its value when the JVM was executed:

```bash
$ exec java $JAVA_OPTS -cp /app/resources:/app/classes:/app/libs/* "$MAIN_CLASS_NAME"
```

To understand how this wildcard turns into the jar paths passed to a
classloader, we needed to understand some primordial code, early in the JVMs
lifecycle, in
[`wildcard.c`](https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/jdk8u232-b09/jdk/src/share/bin/wildcard.c#L356):

```c
static FileList
wildcardFileList(const char *wildcard)
{
    const char *basename;
    FileList fl = FileList_new(16);
    WildcardIterator it = WildcardIterator_for(wildcard);

    if (it == NULL)
    {
        FileList_free(fl);
        return NULL;
    }

    while ((basename = WildcardIterator_next(it)) != NULL)
        if (isJarFileName(basename))
            FileList_add(fl, wildcardConcat(wildcard, basename));
    WildcardIterator_close(it);
    return fl;
}
```

This function creates an iterator over all of the files specified by the
wildcard, and adds them to a list of files. You can see this in action if you
specify the `_JAVA_LAUNCHER_DEBUG=1` environment variable:

```bash
Expanded wildcards:
    before: "/app/resources:/app/classes:/app/libs/*"
    after : "/app/resources:/app/classes:/app/libs/httpcore-4.4.11.jar:/...
```

We discovered that the expanded path differed between clusters. The difference
was enough to place the fat jar with the old dependencies before the desired
dependencies on the classpath!

When running on a Unix-like system (we're running on Linux in our deployed
environments), the items added to the classpath are fetched via a call to the
[`readdir`](http://man7.org/linux/man-pages/man3/readdir.3.html) syscall:

```c
static char *
WildcardIterator_next(WildcardIterator it)
{
    struct dirent* dirp = readdir(it->dir);
    return dirp ? dirp->d_name : NULL;
}
```

Deep in the manpage, there's this comment:

```
The order in which filenames are read by successive calls to
readdir() depends on the filesystem implementation; it is unlikely
that the names will be sorted in any fashion.
```

Could it be that on different virtual machines, running different Kubernetes
versions, we could be returned differing file orders? We cooked up a test to
confirm.

We compiled the following into our containers and deployed them:

```C
#include <stdio.h>
#include <dirent.h>

int main()
{
    DIR *dir
    struct dirent *entry;
    int files = 0;

    dir = opendir("./libs");
    if(dir == NULL)
    {
        perror("Unable to read directory");
        return(1);
    }

    while( (entry=readdir(dir)) )
    {
        files++;
        printf("File %3d: %s\n", files, entry->d_name);
    }

    closedir(dir);

    return(0);
}
```

The program simply walks the files in a directory and prints them to stdout. In
this case `libs/` is the directory that contains all of our jars in the
container:

Running this in two different containers on different VMs with varying
Kubernetes versions produced different results (we output into a checksum to
easily verify):

```bash
# older GKE version
root@76931ebe7ed1:/app# ./read_dir | md5sum
de2c0f91c1decde06a9d9b27f8c3d44e  -

# newer GKE version
root@5cea2d0dcd62:/app# ./read_dir | md5sum
c63c00360794942e2d531f3e4c005363  -
```

So there we have it, proof that in different containers the order of the jars
provided for the classpath are different, and this difference produces the
eventual runtime exception that crashes the processes. Wild.

We wanted to keep digging to understand this difference, as it seemed strange
that the Linux kernel, within a few minor revisions, would return a different
sort order for a directory. We realized that our initial test takes into
account what is happening _inside_ a container. Was there a difference on the
raw VM itself?

We run Google's Container OS on our VMs, and they provide a means of debugging
on the raw VM (i.e. outside of the K8s runtime). We used the `toolbox` to install
`gcc` so we could compile the same program:

```bash
# First boot a container with a volume mounted so we can transfer files out of
the container
nick.travers@gke-preemptible-b955-44d9a6e6-z43d ~ $ docker run --rm -it \
  -v /tmp/test:/tmp/test \
  --entrypoint /bin/bash \
  container-with-file-walker:latest
6bef
root@40a6294950e7:/app# cp libs/* /tmp/test/
exit

# Boot the toolbox with the directory mounted
nick.travers@gke-preemptible-b955-44d9a6e6-z43d ~ $ toolbox --bind /tmp/test/:/tmp/test/
Spawning container nick.travers-gcr.io_google-containers_toolbox-20190523-00 on /var/lib/toolbox/nick.travers-gcr.io_google-containers_toolbox-20190523-00.
Press ^] three times within 1s to kill container.
root@gke--preemptible-b955-44d9a6e6-z43d:~#

# Install GCC
root@gke-preemptible-b955-44d9a6e6-z43d:~# apt-get update && apt-get install -y gcc

# Compile our program (heredoc it into place, or use VIM)
root@gke--preemptible-b955-44d9a6e6-z43d:~# gcc -o read_dir read_dir.c
```

Running the program in the toolbox on two different GKE VMs revealed the
following:

```bash
# Older GKE version
root@gke-highmem-preem-04caffad-fc7s:~# ./read_dir | md5sum
9ff8c2a1302363a0e2271726011b92e3  -

# Newer GKE version
root@gke-preemptible-b955-44d9a6e6-z43d:~# ./read_dir | md5sum
9ff8c2a1302363a0e2271726011b92e3  -
```

Ha! The output is the same, and therefore the order my be the same on the
underlying OS. The difference is introduced by the volume mounted for the
containers, and this differs only between GKE VM versions.

At this point, I'd run out of time and patience to invest in poking around with
the filesystem, having sunk in at least a day poking around the JVM (Java and
C, argh!), and testing across multiple GKE node versions. I hope to be able to
pick this back up at some point soon to understand how the Docker container
filesystem affects the sort order of `readdir`.

Some closing comments:

- Don't rely on anything related to Java's classpath (there are no guarantees
  here around ordering). This one is obvious.
- Fat jars are bad news when publishing _libraries_. This one is obvious but
  it's easy to get bitten by if you're not paying attention to the "provenance"
  of your dependencies.
- Java's classpath expansion happens very early in the lifecycle of the JVM,
  and the code governing this is written in (reasonably accessible) C.
- There are no guarantees around the ordering of the `readdir` syscall on
  Linux. Testing in practice has revealed stability when running on a raw GKE
  VM, but shows differences across GKE versions when running inside a
  container.
