package com.salaboy.fmtok8scloudevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.spring.http.CloudEventHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import io.cloudevents.jackson.JsonFormat;
import java.net.URI;
import java.util.UUID;

@SpringBootApplication
@RestController
@Slf4j
public class Fmtok8sCloudeventsApplication {

	public static void main(String[] args) {
		SpringApplication.run(Fmtok8sCloudeventsApplication.class, args);
	}

	@Value("${SINK:http://localhost:8080}")
	private String SINK;

	@PostMapping(value = "/produce")
	public ResponseEntity<Void> produceCloudEvent(){
		// This is my custom payload for the CloudEvent, usually this will be your application data
		MyCloudEventData data = new MyCloudEventData();
		data.setMyData("Hello CloudEvents Data");
		data.setMyCounter(1);

		CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
				.withId(UUID.randomUUID().toString())
				.withType("MyCloudEvent")
				.withSource(URI.create("application-a"))
				.withDataContentType("application/json; charset=UTF-8");

		CloudEvent cloudEvent = cloudEventBuilder.build();

		logCloudEvent(cloudEvent);

		HttpHeaders outgoing = CloudEventHttpUtils.toHttp(cloudEvent);

		log.info("Producing CloudEvent with MyCloudEventData: " + data);

		WebClient webClient = WebClient.builder().baseUrl(SINK).filter(logRequest()).build();
		webClient.post().headers(httpHeaders -> httpHeaders.putAll(outgoing)).bodyValue(data).retrieve()
				.bodyToMono(String.class)
				.doOnError(t -> t.printStackTrace())
				.doOnSuccess(s -> log.info("Result -> " + s)).subscribe();

		return ResponseEntity.ok().build();

	}

	@PostMapping(value = "/")
	public ResponseEntity<Void> consumeCloudEvent(@RequestHeader HttpHeaders headers, @RequestBody MyCloudEventData myCloudEventData) throws JsonProcessingException {

		CloudEvent cloudEvent = CloudEventHttpUtils.fromHttp(headers).build();
		logCloudEvent(cloudEvent);
		if (!cloudEvent.getType().equals("MyCloudEvent")) {
			throw new IllegalStateException("Wrong Cloud Event Type, expected: 'MyCloudEvent' and got: " + cloudEvent.getType());
		}

		// Here you can do whatever you want with your Application data:
		log.info("Consuming CloudEvent with MyCloudEventData: " + myCloudEventData);
		return ResponseEntity.ok().build();

	}

	private void logCloudEvent(CloudEvent cloudEvent) {
		EventFormat format = EventFormatProvider
				.getInstance()
				.resolveFormat(JsonFormat.CONTENT_TYPE);

		log.info("Cloud Event: " + new String(format.serialize(cloudEvent)));

	}

	private static ExchangeFilterFunction logRequest() {
		return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
			log.info("Request: " + clientRequest.method() + " - " + clientRequest.url());
			clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info(name + "=" + value)));
			return Mono.just(clientRequest);
		});
	}

}
