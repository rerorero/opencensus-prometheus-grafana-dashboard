```
docker-compuse up -d
```

- Grafana: [localhost:3000](http://localhost:3000/)
- Prometheus: [localhost:9090](http://localhost:9090/)
- Example gRPC server's exporter: [localhost:19000](http://localhost:19000/metrics)
- Example gRPC client's exporter: [localhost:19001](http://localhost:19001/metrics)

You need to import `opencensus-prometheus.json` manually (Grafana can login with admin/admin).
