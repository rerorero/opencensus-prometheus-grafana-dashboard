FROM hseeberger/scala-sbt as builder

# build directories
RUN mkdir -p /http-scala
WORKDIR /http-scala
ADD . .

RUN sbt assembly

# runner container
FROM openjdk:8
COPY --from=builder /http-scala/modules/client/target/scala-2.12/client-assembly-0.1-SNAPSHOT.jar /bin/client.jar
COPY --from=builder /http-scala/modules/server/target/scala-2.12/server-assembly-0.1-SNAPSHOT.jar /bin/server.jar
