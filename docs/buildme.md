# flb-chatops

Sample Helidon MP project that includes multiple REST operations.

## Build and run

curl -X GET <http://localhost:8080/social>

With JDK21

```bash
mvn package
java -jar target/flb-chatops.jar
```

## Exercise the application

Basic:

```bash
curl -X GET http://localhost:8080/simple-greet
Hello World!
```

JSON:

```bash
curl -X GET http://localhost:8080/greet
{"message":"Hello World!"}

curl -X GET http://localhost:8080/greet/Joe
{"message":"Hello Joe!"}

curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Hola"}' http://localhost:8080/greet/greeting

curl -X GET http://localhost:8080/greet/Jose
{"message":"Hola Jose!"}
```

## Try metrics

```bash
# Prometheus Format
curl -s -X GET http://localhost:8080/metrics
# TYPE base:gc_g1_young_generation_count gauge
. . .

# JSON Format
curl -H 'Accept: application/json' -X GET http://localhost:8080/metrics
{"base":...
. . .
```

## Try health

```bash
curl -s -X GET http://localhost:8080/health
{"outcome":"UP",...

```

## Building a Native Image

The generation of native binaries requires an installation of GraalVM 22.1.0+.

You can build a native binary using Maven as follows:

```bash
mvn -Pnative-image install -DskipTests
```

The generation of the executable binary may take a few minutes to complete depending on
your hardware and operating system. When completed, the executable file will be available
under the `target` directory and be named after the artifact ID you have chosen during the
project generation phase.

## Building the Docker Image

```bash
docker build -t flb-chatops .
```

## Running the Docker Image

```bash
docker run --rm -p 8080:8080 flb-chatops:latest
```

Exercise the application as described above.

## Run the application in Kubernetes

If you don’t have access to a Kubernetes cluster, you can [install one](https://helidon.io/docs/latest/#/about/kubernetes) on your desktop.

### Verify connectivity to cluster

```bash
kubectl cluster-info                        # Verify which cluster
kubectl get pods                            # Verify connectivity to cluster
```

### Deploy the application to Kubernetes

```bash
kubectl create -f app.yaml                  # Deploy application
kubectl get pods                            # Wait for quickstart pod to be RUNNING
kubectl get service  flb-chatops         # Get service info
```

Note the PORTs. You can now exercise the application as you did before but use the second
port number (the NodePort) instead of 8080.

After you’re done, cleanup.

```bash
kubectl delete -f app.yaml
```

## Building a Custom Runtime Image

Build the custom runtime image using the jlink image profile:

```bash
mvn package -Pjlink-image
```

This uses the helidon-maven-plugin to perform the custom image generation.
After the build completes it will report some statistics about the build including the reduction in image size.

The target/flb-chatops-jri directory is a self contained custom image of your application. It contains your application,
its runtime dependencies and the JDK modules it depends on. You can start your application using the provide start script:

```bash
./target/flb-chatops-jri/bin/start
```

Class Data Sharing (CDS) Archive
Also included in the custom image is a Class Data Sharing (CDS) archive that improves your application’s startup
performance and in-memory footprint. You can learn more about Class Data Sharing in the JDK documentation.

The CDS archive increases your image size to get these performance optimizations. It can be of significant size (tens of MB).
The size of the CDS archive is reported at the end of the build output.

If you’d rather have a smaller image size (with a slightly increased startup time) you can skip the creation of the CDS
archive by executing your build like this:

```bash
mvn package -Pjlink-image -Djlink.image.addClassDataSharingArchive=false
```

For more information on available configuration options, see the *helidon-maven-plugin* documentation.

## Slack Test Scripts

To verify that Slack has got the correct configurations, there are a couple of test scripts (both .bat and .sh versions). To use these scripts you do need to use CURL and set the environment variables from the set-env,[bat|sh]  These scripts are:

| Script Name                       | Purpose                                                      |
| --------------------------------- | ------------------------------------------------------------ |
| get-slack-conversation-curl       | This retrieves the last three lines of Slack conversation (unlike the App, which limits the retrieved conversation by time).  It includes a toggled property that can enable pretty formatting of the response. |
| send-slack-test-conversation-curl | This sends a message to the channel. If permissions are correct you'll see a message from the app |
