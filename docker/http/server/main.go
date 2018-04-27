// Copyright 2018, OpenCensus Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"flag"
	"fmt"
	"log"
	"math/rand"
	"net/http"
	"time"

	"go.opencensus.io/exporter/prometheus"

	"go.opencensus.io/plugin/ochttp"
	"go.opencensus.io/stats/view"
)

var (
	port         = flag.Int("port", 19000, "Port number of gRPC service")
	exporterPort = flag.Int("exporter-port", 19001, "Port number of Prometheus exporter")
)

func main() {
	flag.Parse()
	rand.Seed(time.Now().UnixNano())

	// Register stats exporters to export the collected data.
	exporter, err := prometheus.NewExporter(prometheus.Options{})
	exitIfErr(err)
	view.RegisterExporter(exporter)
	err = view.Register(ochttp.DefaultServerViews...)
	exitIfErr(err)

	// Report stats at every second.
	view.SetReportingPeriod(1 * time.Second)

	// run exporter
	go func() {
		log.Println("Exporter is listening on", *exporterPort)
		http.Handle("/metrics", exporter)
		log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", *exporterPort), nil))
	}()

	handler := func(w http.ResponseWriter, req *http.Request) {
		time.Sleep(time.Duration(rand.Intn(3)*100) * time.Millisecond)
		switch rand.Intn(30) {
		case 0:
			fmt.Println(req.Method, req.URL, http.StatusBadRequest)
			w.WriteHeader(http.StatusBadRequest)
		case 1:
			fmt.Println(req.Method, req.URL, http.StatusInternalServerError)
			w.WriteHeader(http.StatusInternalServerError)
		default:
			fmt.Println(req.Method, req.URL, http.StatusOK)
			fmt.Fprintf(w, "hello world")
		}
	}

	http.HandleFunc("/hello", handler)
	http.HandleFunc("/world", handler)
	http.HandleFunc("/hello/world", handler)
	log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", *port), &ochttp.Handler{}))
}

func exitIfErr(e error) {
	if e != nil {
		panic(e)
	}
}
