# BattleNet Server Events Emulator

## Running Kafka with Schema Registry locally

1. Download Confluent Platform
2. Run Zookeeper
```shell
./bin/zookeeper-server-start ./etc/kafka/zookeeper.properties
```
3. Start Kafka Broker
```shell
./bin/kafka-server-start ./etc/kafka/server.properties
```
4. Start Schema Registry
```shell
./bin/schema-registry-start ./etc/schema-registry/schema-registry.properties
```

Running broker can be verified using this command:
```shell
./bin/kafka-topics --list --bootstrap-server=localhost:9092
```

Running schema registry can be verified by:
```shell
curl http://localhost:8081/subjects
# OR
http curl http://localhost:8081/subjects
```

Schema registry compatibility settings can be adjusted via:
```shell
curl -X PUT -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data '{"compatibility": "NONE"}' \
  http://localhost:8081/config
# OR
http PUT http://localhost:8081/config "Content-Type: application/vnd.schemaregistry.v1+json" compatibility="NONE" 
```

A nice way of getting schema printed out:
```shell
http localhost:8081/subjects/battlenet.server.events.v1-value/versions/1 | jq ".schema" | sed 's/^"\(.*\)"$/\1/' | sed 's/\\"/"/g' | jq
```
