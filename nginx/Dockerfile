FROM nginx

RUN mkdir -p /data/nginx/cache
ADD nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
ENTRYPOINT ["nginx", "-g", "daemon off;"]

# vi:syntax=dockerfile
