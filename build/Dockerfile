FROM golang:1.18.3-buster AS build

RUN \
  apt-get update && \
  apt-get install -y curl gcc && \
  curl -L -o /usr/local/bin/bazel https://github.com/bazelbuild/bazelisk/releases/download/v1.12.0/bazelisk-linux-amd64 && \
  chmod +x /usr/local/bin/bazel

COPY . /build

WORKDIR /build

RUN make build

FROM gcr.io/cloud-marketplace-containers/google/debian10@sha256:9a4a8331d0626e2b56ababd7e4467b8847ef54809d27e721fba2b4b03073ad02

WORKDIR /site

ADD content ./content

COPY --from=build /build/bazel-bin/pkg/cmd/site/site_/site .

ENTRYPOINT ["./site"]
