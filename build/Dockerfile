FROM golang:1.15.3-buster AS build

RUN \
  apt-get update && \
  apt-get install -y curl gcc && \
  curl -L -o /usr/local/bin/bazel https://github.com/bazelbuild/bazelisk/releases/download/v1.7.3/bazelisk-linux-amd64 && \
  chmod +x /usr/local/bin/bazel

COPY . /build

WORKDIR /build

RUN make build
#
#FROM gcr.io/cloud-marketplace-containers/google/debian10@sha256:f57a7583f196eeeda9ee2c06470e51bbbfa18c7362fd6965d2eb91bbd7f9708a
#
#WORKDIR /site
#
#ADD config.yaml .
#ADD content ./content
#ADD templates ./templates
#ADD static ./static
#
#COPY --from=build /build/bazel-bin/cmd/site/site_/site .
#
#ENTRYPOINT ["./site"]
