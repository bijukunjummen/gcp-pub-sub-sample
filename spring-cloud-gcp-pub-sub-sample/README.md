
## Start the pubsub emulator locally
```shell
gcloud beta emulators pubsub start --project=sampleproj --host-port=localhost:8934
```


## Create topic and subscription
```shell
gcloud config configurations create emulator
gcloud config set auth/disable_credentials true
gcloud config set project sampleproj
gcloud config set api_endpoint_overrides/pubsub http://localhost:8934/

export PUBSUB_EMULATOR_HOST=localhost:8934
export PUBSUB_PROJECT_ID=sampleproj

gcloud pubsub topics create sample-topic

gcloud pubsub subscriptions create sample-subscription --topic=sample-topic
```

## Request Messages
```shell
curl http://localhost:8080/messages
```

## Publish
```shell
curl -v \
  -H "Content-type: application/json" \
  -H "Accept: application/json" \
   http://localhost:8080/messages \
   -d '{
   "id": "test-id",
   "payload": "sample-payload"
}'
```