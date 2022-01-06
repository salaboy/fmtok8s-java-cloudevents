# Simple CloudEvents Application using Spring Boot

This application exposes two endpoints, one which produces a CloudEvent and one that consumes a CloudEvent. 


## Building and running the application

This project is built with Maven, so you need to have Maven and Java installed in your local environment to build and run this project from source: 

To package the project: 
```
mvn package 
```

To run the project using the Maven Spring Boot plugin: 
```
mvn spring-boot:run
```

Alternatively, if you have docker installed in your local environment you can run: 

```
docker run -e SINK=http://localhost:8081 -p 8080:8080 -t salaboy/fmtok8s-java-cloudevents 
```


## Sending Requests and CloudEvents

```
curl -X POST localhost:8080/produce -v
```

Consume a CloudEvent and print it out: 

```
curl -X POST http://localhost:8080/ -H "Content-Type: application/json" -H "ce-type: MyCloudEvent"  -H "ce-id: 123"  -H "ce-specversion: 1.0" -H "ce-source: curl-command" -d '{"myData" : "hello from curl", "myCounter" : 1 }'
```