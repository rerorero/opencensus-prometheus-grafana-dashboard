FROM golang:1.9 as builder

# build directories
RUN mkdir -p /go/src/github.com/rerorero/opencensus-prometheus-grafana-dashboard/docker/grpc
WORKDIR /go/src/github.com/rerorero/opencensus-prometheus-grafana-dashboard/docker/grpc
ADD . .

# Build meshem
RUN go get github.com/golang/dep/...
RUN dep ensure
RUN CGO_ENABLED=0 GOOS=linux go build -a -tags netgo -installsuffix netgo --ldflags '-extldflags "-static"' -o /server ./server
RUN CGO_ENABLED=0 GOOS=linux go build -a -tags netgo -installsuffix netgo --ldflags '-extldflags "-static"' -o /client ./client

# runner container
FROM alpine:latest
COPY --from=builder /server /bin/server
COPY --from=builder /client /bin/client
