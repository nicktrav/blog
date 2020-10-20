Crypto crashing curl
====================

 key  | value
 ---  | ---
name  | 2020-10-19-crypto-crashing-curl

_October 19, 2020_

_I needed a good excuse to use aliteration._

I've been building something on the job that uses [HTTP
CONNECT](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/CONNECT) to
connect to an upstream backend with a proxy in the middle\*. Both the "outer"
(the connection carrying the initial CONNECT request) and "inner" connections
(the raw TCP connection mediated by the connect proxy) are TLS encrypted.

While testing the end-to-end flow, I hit a segfault in `curl`, running
something the following:

```bash
$ curl \
  -v \
  -x https://localhost:4433 \
  --proxy-cacert /etc/tls/ca.pem \
  --cacert /etc/tls/ca.pem \
  https://localhost:4444
```

I get the dreaded segfault 🙀:

```console
...
Segmentation fault (core dumped)
```

It was pretty clean the bug was in the TLS backend I was using in `curl`. I use
[BoringSSL](https://boringssl.googlesource.com/boringssl/), coz why not build
your own version of a TLS library to link against when your build your own
version of curl!

Debugging was pretty straight forward, and I could tell the issue was in
BoringSSL as the crash didn't occur when using a version of curl linked against
OpenSSL.

The core file was super useful in this case. I dumped it and took a look in
`gdb`:

```bash
root@nickt:/# gdb $(which curl) /cores/core
(gdb) bt
#0  0x000055c0f43b4692 in BIO_get_retry_flags (bio=0x0) at /build/boringssl/crypto/bio/bio.c:280
#1  0x000055c0f43b4706 in BIO_copy_next_retry (bio=0x55c0f65f13c8) at /build/boringssl/crypto/bio/bio.c:292
#2  0x000055c0f435593e in ssl_ctrl (bio=0x55c0f65f13c8, cmd=11, num=0, ptr=0x0) at /build/boringssl/ssl/bio_ssl.cc:122
#3  0x000055c0f43b4495 in BIO_ctrl (bio=0x55c0f65f13c8, cmd=11, larg=0, parg=0x0) at /build/boringssl/crypto/bio/bio.c:212
#4  0x000055c0f43b440c in BIO_flush (bio=0x55c0f65f13c8) at /build/boringssl/crypto/bio/bio.c:199
#5  0x000055c0f439d343 in bssl::tls_flush_flight (ssl=0x55c0f65f0668) at /build/boringssl/ssl/s3_both.cc:339
#6  0x000055c0f4391409 in bssl::ssl_run_handshake (hs=0x55c0f65d5fc8, out_early_return=0x7ffdfefbd9c3) at /build/boringssl/ssl/handshake.cc:561
#7  0x000055c0f435d698 in SSL_do_handshake (ssl=0x55c0f65f0668) at /build/boringssl/ssl/ssl_lib.cc:889
#8  0x000055c0f435d73f in SSL_connect (ssl=0x55c0f65f0668) at /build/boringssl/ssl/ssl_lib.cc:911
#9  0x000055c0f4351ac1 in ossl_connect_step2 (conn=0x55c0f65b7608, sockindex=0) at vtls/openssl.c:3212
#10 0x000055c0f4353ec6 in ossl_connect_common (conn=0x55c0f65b7608, sockindex=0, nonblocking=true, done=0x7ffdfefbdd85) at vtls/openssl.c:4025
#11 0x000055c0f4353ffa in Curl_ossl_connect_nonblocking (conn=0x55c0f65b7608, sockindex=0, done=0x7ffdfefbdd85) at vtls/openssl.c:4059
#12 0x000055c0f432e7a5 in Curl_ssl_connect_nonblocking (conn=0x55c0f65b7608, sockindex=0, done=0x7ffdfefbdd85) at vtls/vtls.c:334
#13 0x000055c0f42e36a6 in https_connecting (conn=0x55c0f65b7608, done=0x7ffdfefbdd85) at http.c:1497
#14 0x000055c0f42e34ca in Curl_http_connect (conn=0x55c0f65b7608, done=0x7ffdfefbdd85) at http.c:1424
#15 0x000055c0f42fa185 in multi_runsingle (multi=0x55c0f65b7278, nowp=0x7ffdfefbded0, data=0x55c0f65b85d8) at multi.c:1941
#16 0x000055c0f42fb616 in curl_multi_perform (multi=0x55c0f65b7278, running_handles=0x7ffdfefbdf24) at multi.c:2559
#17 0x000055c0f42d4771 in easy_transfer (multi=0x55c0f65b7278) at easy.c:592
#18 0x000055c0f42d499a in easy_perform (data=0x55c0f65b85d8, events=false) at easy.c:682
#19 0x000055c0f42d49e4 in curl_easy_perform (data=0x55c0f65b85d8) at easy.c:701
#20 0x000055c0f42c9dda in serial_transfers (global=0x7ffdfefbe140, share=0x55c0f65b3c38) at tool_operate.c:2322
#21 0x000055c0f42ca271 in run_all_transfers (global=0x7ffdfefbe140, share=0x55c0f65b3c38, result=CURLE_OK) at tool_operate.c:2500
#22 0x000055c0f42ca58d in operate (global=0x7ffdfefbe140, argc=9, argv=0x7ffdfefbe2a8) at tool_operate.c:2616
#23 0x000055c0f42c0594 in main (argc=9, argv=0x7ffdfefbe2a8) at tool_main.c:323
```

This shows the exact location of the issue, which helped when filing the bug.

As someone that loves reporting a good bug, I like to make sure that it's
easily reproducible for the maintainers of a project. There's nothing worse
than someone saying "I found a bug" and offering nothing in the way of helping
to reproduce it.

So I [put together a little reproducer and threw it into a git
repo](https://github.com/nicktrav/boringssl-nullptr-repro), which I sent as an
archive to the BoringSSL maintainers.

The reproducer sets up a client (`curl`) talking via a proxy (I use
[Envoy](https://github.com/envoyproxy/envoy) for everything these days) to a
backend (a small Go binary that echoes back a simple response). I put
everything in containers (my day job is all about containers, and they're still
in vogue these days so why not).

The fun part was building BoringSSL and curl from scratch, which is nice and
repeatable in a Dockerfile.

Anyway, I sent this off to the security list of the BoringSSL project to see
what they thought. Better to play it safe with these kinds of bugs as you never
know when a crash due to some invalid memory reference bug or null pointer
dereference is going to bite you in the arse.

Turns out it's a bug (duh), and the fix was made in a couple of hours of me
reporting it. The commit can be found
[here](https://boringssl.googlesource.com/boringssl/+/3989c99706bf30054798ff82f1cb010e50e385f5).
While communicating with some folks on the list, I was pointed in the direction
of the breaking change in the library, which I found interesting.

BoringSSL is a slimmed down, more or less API compatible implementation of
OpenSSL, written and maintained by Google. Certain features of the OpenSSL
implementation aren't present in BoringSSL (for good reason). Turns out this
bug was due to a case of [functionality being added in
`curl`](https://github.com/curl/curl/commit/cb4e2be7c6d42ca0780f8e0a747cecf9ba45f151)
(support for TLS-in-TLS HTTP CONNECT requests) that is handled by OpenSSL but
not by BoringSSL.

There's an [interesting commit
message](https://boringssl.googlesource.com/boringssl/+/f5b30cc28c685b7744c3752a2a78cb920eae7ced%5E%21/)
in BoringSSL that points to this "TLS-in-TLS" implementation being smelly,
which is why it was stripped. This was a nice find, as it validated an initial
concern I had with the original version of the thing I was building that
surfaced the bug in the first place. Nothing like validating a concern by
finding a bug in an external library that crashes your library!

Anyway, all in all, this was fun to dig into, and resulted in a fix to a
project run by some people I have a lot of respect for. Kudos to David and
Piotr from Google for helping me out.

\*: the project itself is kinda cool and something I hope to write about.
