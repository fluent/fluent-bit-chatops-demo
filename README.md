# Introduction

The demo for [Cloud Native Rejects Paris 2024 talk](https://cfp.cloud-native.rejekts.io/cloud-native-rejekts-eu-paris-2024/talk/review/PP8T37LPQUSTTRWBXVCHFJSSKDM9B98J).

The [actual presentation is available in this repo](./docs/Fluent_Bit_for_ChatOps-Cloud_Native_Rejekts_EU_2024.pdf).

Videos covering the demo are here:

* [Demo](https://chronosphere-io.zoom.us/rec/share/acwg8ZR0Laoj7p9GlzBWSmJRTEXoNlGicYIxNPNKvVsey6NvtU44oiReTbmHKWMf.8GMEIpjA2d5pX5rZ?startTime=1710415864000&pwd=LGnKSo-mzFXroR5H04BcVUmoWRFzge4f)
* [Code overview](https://chronosphere-io.zoom.us/rec/share/acwg8ZR0Laoj7p9GlzBWSmJRTEXoNlGicYIxNPNKvVsey6NvtU44oiReTbmHKWMf.8GMEIpjA2d5pX5rZ?startTime=1710416213000&pwd=LGnKSo-mzFXroR5H04BcVUmoWRFzge4f)

This contains all the resources to build a demo-able ChatOps solution using Slack and Fluent Bit.

* all the details are in the [docs](./docs/readme.md) and the specific build docs are in [docs/buildme.md](./docs/buildme.md)
* [fluent-bit-config](./fluent-bit-config/) has the Fluent Bit configuration
* [src](./src/) with all the Java code build with [Helidon](https://helidon.io/).

Create a `.env` file with the following secrets/config set up:

```shell
SLACK_TOKEN=xoxb-XXX
SLACK_CHANNEL_ID=XXX
SLACK_CHANNEL_NAME=XXX
SLACK_WEBHOOK=https://hooks.slack.com/services/XXX

OPS_RETRYINTERVAL=10
OPS_RETRYCOUNT=18
TESTFLB=FALSE
SYNTH_ALERT_PORT=
```

Once this is done, just run up `docker compose up` to start sending an alert and responding.
