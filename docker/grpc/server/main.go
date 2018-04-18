package main

import (
	"context"
	"flag"
	"fmt"
	"io"
	"log"
	"math/rand"
	"net"
	"net/http"
	"time"

	"google.golang.org/grpc/codes"

	pb "github.com/rerorero/opencensus-prometheus-grafana-dashboard/docker/grpc/proto"
	"go.opencensus.io/exporter/prometheus"
	"go.opencensus.io/plugin/ocgrpc"
	"go.opencensus.io/stats/view"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
	"google.golang.org/grpc/status"
)

type server struct{}

var (
	port         = flag.Int("port", 18888, "Port number of gRPC service")
	exporterPort = flag.Int("exporter-port", 18889, "Port number of Prometheus exporter")
)

func (s *server) SayHelloStream(stream pb.Greeter_SayHelloStreamServer) error {
	for {
		in, err := stream.Recv()
		if err == io.EOF {
			log.Println("EOF")
			return nil
		}
		if err != nil {
			return err
		}
		log.Println("received stream: name is", in.GetName())

		// Respond after a random interval (0 - 300msec)
		for i := 0; i < 3; i++ {
			time.Sleep(time.Duration(rand.Intn(4)*100) * time.Millisecond)
			res := &pb.HelloReply{
				Message: fmt.Sprintf("Hello, %s (%d).", in.GetName(), i),
			}
			if err := stream.Send(res); err != nil {
				return err
			}
		}
		// Sometimes fail
		if rand.Intn(30) == 0 {
			return status.New(codes.InvalidArgument, "stream error").Err()
		}
	}
}

func (s *server) SayHello(ctx context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
	log.Println("received: name is", in.GetName())
	// Respond after a random interval (0 - 300msec)
	time.Sleep(time.Duration(rand.Intn(4)*100) * time.Millisecond)

	res := &pb.HelloReply{
		Message: fmt.Sprintf("Hello, %s.", in.GetName()),
	}
	// Sometimes fail
	if rand.Intn(30) == 0 {
		return res, status.New(codes.InvalidArgument, "simple error").Err()
	}
	return res, nil
}

func main() {
	flag.Parse()
	rand.Seed(time.Now().UnixNano())

	exporter, err := prometheus.NewExporter(prometheus.Options{})
	exitIfErr(err)

	view.RegisterExporter(exporter)
	err = view.Register(ocgrpc.DefaultServerViews...)
	exitIfErr(err)

	view.SetReportingPeriod(5 * time.Second)

	// run exporter
	go func() {
		log.Println("Exporter is listening on", *exporterPort)
		http.Handle("/metrics", exporter)
		log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", *exporterPort), nil))
	}()

	// Start greeter service
	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", *port))
	exitIfErr(err)

	s := grpc.NewServer(grpc.StatsHandler(&ocgrpc.ServerHandler{}))
	pb.RegisterGreeterServer(s, &server{})
	reflection.Register(s)

	log.Println("Greeter service is listening on ", *port)
	err = s.Serve(lis)
	exitIfErr(err)
}

func exitIfErr(e error) {
	if e != nil {
		panic(e)
	}
}
