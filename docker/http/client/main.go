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
	"bytes"
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
	addr         = flag.String("addr", "localhost:19000", "Server address")
	exporterPort = flag.Int("exporter-port", 19991, "Exporter port")
	lifeSec      = flag.Float64("sec", 120, "Client lifetime")
	paths        = []string{"/hello", "/world", "/hello/world"}
)

func main() {
	flag.Parse()
	rand.Seed(time.Now().UnixNano())
	// Register stats exporters to export the collected data.
	exporter, err := prometheus.NewExporter(prometheus.Options{})
	exitIfErr(err)
	view.RegisterExporter(exporter)
	err = view.Register(ochttp.DefaultClientViews...)
	exitIfErr(err)

	// Report stats at every second.
	view.SetReportingPeriod(1 * time.Second)

	// Run exporter
	go func() {
		log.Println("Exporter is listening on", *exporterPort)
		http.Handle("/metrics", exporter)
		log.Fatal(http.ListenAndServe(fmt.Sprintf(":%d", *exporterPort), nil))
	}()

	client := &http.Client{Transport: &ochttp.Transport{}}

	started := time.Now()
	for {
		if time.Since(started).Seconds() > *lifeSec {
			break
		}

		// Random interval(10-1010 msec)
		time.Sleep(time.Duration(rand.Intn(10)*100+10) * time.Millisecond)

		url := fmt.Sprintf("http://%s%s", *addr, paths[rand.Intn(3)])
		var resp *http.Response
		switch rand.Intn(2) {
		case 0:
			resp, err = client.Post(url, "text/plain", bytes.NewBuffer([]byte("Hi")))
		default:
			resp, err = client.Get(url)
		}
		if err != nil {
			log.Printf("Failed to get response: %s: %v", url, err)
		} else {
			log.Println(url, resp.Status)
			resp.Body.Close()
		}
	}

	time.Sleep(30 * time.Second) // Wait until stats are reported.
}

func exitIfErr(e error) {
	if e != nil {
		panic(e)
	}
}
