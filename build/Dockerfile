# Build container - mdbook
FROM gcr.io/cloud-marketplace-containers/google/debian10@sha256:54e0e8e69dd42cb76c3737c8f44c88bf5ad4ecd187aa6412ad08778254353988 AS build

RUN apt-get update -y && \
  apt-get install -y curl gcc && \
  curl -L -O https://static.rust-lang.org/rustup/dist/x86_64-unknown-linux-gnu/rustup-init && \
  chmod +x rustup-init && \
  echo 1 | ./rustup-init

ENV PATH $PATH:/root/.cargo/bin

RUN cargo install mdbook --vers '0.4.1'

ADD content/ /site

WORKDIR /site

RUN mdbook build

# Runtime conatiner - nginx
FROM gcr.io/cloud-marketplace-containers/google/debian10@sha256:54e0e8e69dd42cb76c3737c8f44c88bf5ad4ecd187aa6412ad08778254353988

RUN apt-get update -y && \
  apt-get install -y nginx && \
  apt-get remove --purge -y && \
  rm -rf /var/lib/apt/lists/*

ADD build/nginx/nginx.conf /etc/nginx/nginx.conf

COPY --from=build /site/_site /usr/share/nginx/html/

ENTRYPOINT ["nginx", "-g", "daemon off;"]
