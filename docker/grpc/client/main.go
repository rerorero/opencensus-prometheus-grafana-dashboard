package main

import (
	"flag"
	"fmt"
	"io"
	"log"
	"math/rand"
	"net/http"
	"sync"
	"time"

	pb "github.com/rerorero/opencensus-prometheus-grafana-dashboard/docker/grpc/proto"
	"go.opencensus.io/exporter/prometheus"
	"go.opencensus.io/plugin/ocgrpc"
	"go.opencensus.io/stats/view"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
)

var (
	addr         = flag.String("addr", "localhost:18888", "Server address")
	exporterPort = flag.Int("exporter-port", 18887, "Exporter port")
	name         = flag.String("name", "world", "Name")
	lifeSec      = flag.Float64("sec", 120, "Client lifetime")
	mutex        = new(sync.Mutex)
)

func main() {
	flag.Parse()
	rand.Seed(time.Now().UnixNano())
	ctx := context.Background()

	exporter, err := prometheus.NewExporter(prometheus.Options{})
	exitIfErr(err)

	view.RegisterExporter(exporter)
	err = view.Register(ocgrpc.DefaultClientViews...)
	exitIfErr(err)

	view.SetReportingPeriod(5 * time.Second)

	// Run exporter
	go func() {
		log.Println("Exporter is listening on", *exporterPort)
		http.Handle("/metrics", exporter)
		log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", *exporterPort), nil))
	}()

	// Start client
	conn, err := grpc.Dial(*addr, grpc.WithStatsHandler(&ocgrpc.ClientHandler{}), grpc.WithInsecure())
	exitIfErr(err)
	defer conn.Close()
	client := pb.NewGreeterClient(conn)

	var stream pb.Greeter_SayHelloStreamClient
	var i int
	for i = 0; i < 3; i++ {
		stream, err = client.SayHelloStream(ctx)
		if err != nil {
			// retry
			time.Sleep(5 * time.Second)
		} else {
			break
		}
	}
	exitIfErr(err)

	waitc := make(chan struct{})
	log.Println("Client started. ", *addr)
	go func() {
		for {
			in, err := stream.Recv()
			if err == io.EOF {
				close(waitc)
				log.Println("EOF")
				return
			}
			if err != nil {
				log.Println("error", err.Error())
				stream.CloseSend()
				mutex.Lock()
				var err2 error
				stream, err2 = client.SayHelloStream(ctx)
				mutex.Unlock()
				if err2 != nil {
					log.Fatal(err2)
				}
			} else {
				log.Println("Received stream response", in.Message)
			}
		}
	}()

	started := time.Now()
	for {
		if time.Since(started).Seconds() > *lifeSec {
			break
		}

		// Random interval(300-2200 msec)
		time.Sleep(time.Duration(rand.Intn(20)*100+300) * time.Millisecond)

		req := &pb.HelloRequest{
			Name: *name,
		}
		mutex.Lock()
		err := stream.Send(req)
		mutex.Unlock()
		if err != nil {
			log.Printf("could not greet: %+v\n", err)
		}
		response, err := client.SayHello(ctx, req)
		if err != nil {
			log.Printf("could not simple greet: %+v\n", err)
		} else {
			log.Println("Received simple response", response.Message)
		}
	}
	stream.CloseSend()
}

func exitIfErr(e error) {
	if e != nil {
		panic(e)
	}
}
