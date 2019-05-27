# Build container - mdbook
FROM gcr.io/cloud-marketplace-containers/google/debian9@sha256:208b40bf4fe0a6f1085353e0e69d10150a68cbd67f700bcd386ba070c0765a29 AS build

RUN apt-get update -y && \
  apt-get install -y curl gcc && \
  curl -L -O https://static.rust-lang.org/rustup/dist/x86_64-unknown-linux-gnu/rustup-init && \
  chmod +x rustup-init && \
  echo 1 | ./rustup-init

ENV PATH $PATH:/root/.cargo/bin

RUN cargo install mdbook --vers '0.2.3'

ADD content/ /site

WORKDIR /site

RUN mdbook build

# Runtime conatiner - nginx
FROM gcr.io/cloud-marketplace-containers/google/debian9@sha256:208b40bf4fe0a6f1085353e0e69d10150a68cbd67f700bcd386ba070c0765a29

RUN apt-get update -y && \
  apt-get install -y nginx && \
  apt-get remove --purge -y && \
  rm -rf /var/lib/apt/lists/*

ADD nginx/nginx.conf /etc/nginx/nginx.conf

COPY --from=build /site/_site /usr/share/nginx/html/

EXPOSE 80

ENTRYPOINT ["nginx", "-g", "daemon off;"]
