FROM rust:1.28.0

RUN cargo install mdbook --vers '0.2.0'

RUN apt-get update -y && \
  apt-get install -y dumb-init && \
  apt-get remove --purge -y && \
  rm -rf /var/lib/apt/lists/*

ADD . /site

WORKDIR /site

EXPOSE 9000

ENTRYPOINT ["/usr/bin/dumb-init", "--"]

CMD ["mdbook", "serve", "-p", "9000", "-n", "0.0.0.0"]
