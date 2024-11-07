# swt-automation

An attempt to develop a API for automating SWT applications (for functional testing).
 
## Format

```shell
mvn fmt:format
```

## Build

```shell
mvn clean package
```

## Run

```shell
java -javaagent:./swt-robot-agent/target/swt-robot-agent-1.0-SNAPSHOT.jar=9000 \
  -jar ./demo-app/target/demo-app-1.0-SNAPSHOT.jar
```

And then you can run the demo tests:

```shell
cd e2e-test
npm run test
```