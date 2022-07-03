FROM golang:1.18.3-alpine3.16 AS build

RUN apk add make

COPY . /build

WORKDIR /build

RUN make build

FROM alpine:3.16.0

WORKDIR /site

ADD content ./content

COPY --from=build /build/bin/site .

EXPOSE 8080

CMD ["./site", "run", "--manifest", "./content/manifest.yaml"]
