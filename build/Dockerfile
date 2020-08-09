FROM golang:1.14.4 AS build

WORKDIR /build

COPY . .

RUN go build ./cmd/site

FROM gcr.io/cloud-marketplace-containers/google/debian10@sha256:f57a7583f196eeeda9ee2c06470e51bbbfa18c7362fd6965d2eb91bbd7f9708a

WORKDIR /site

ADD config.yaml .
ADD content ./content
ADD templates ./templates
ADD static ./static

COPY --from=build /build/site .

ENTRYPOINT ["./site"]
