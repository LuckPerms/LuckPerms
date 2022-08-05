FROM alpine
RUN apk add --no-cache openjdk17 netcat-openbsd

# create a simple 'send' command that will allow users
# to run, for example: docker exec <container> send lp info
RUN printf '#!/bin/sh\n\
echo "$@" | nc -N localhost 3000\n' >> /usr/bin/send && chmod 777 /usr/bin/send

# setup user
RUN addgroup -S app && adduser -S -G app app
USER app

# copy jar file into image
WORKDIR /opt/luckperms
COPY LuckPerms-Standalone-*.jar .
RUN mv * luckperms-standalone.jar

# create volume for data directory
RUN mkdir data
VOLUME ["/opt/luckperms/data"]

CMD ["java", "-jar", "luckperms-standalone.jar", "--docker"]

HEALTHCHECK --interval=30s --timeout=15s --start-period=20s \
    CMD wget http://localhost:3001/health -q -O - | grep -c '"up":true' || exit 1
