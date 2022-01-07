package com.salaboy.fmtok8scloudevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@SpringBootApplication
@RestController
@Slf4j
public class Fmtok8sCloudeventsApplication {

	private ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private WebClient.Builder rest;

	@Configuration
	public static class CloudEventHandlerConfiguration implements CodecCustomizer {

		@Override
		public void customize(CodecConfigurer configurer) {
			configurer.customCodecs().register(new CloudEventHttpMessageReader());
			configurer.customCodecs().register(new CloudEventHttpMessageWriter());
		}

	}

	public static void main(String[] args) {
		SpringApplication.run(Fmtok8sCloudeventsApplication.class, args);
	}

	@Value("${SINK:http://localhost:8080}")
	private String SINK;

	@PostMapping(value = "/produce")
	public ResponseEntity<String> produceCloudEvent() throws JsonProcessingException {
		// This is my custom payload for the CloudEvent, usually this will be your application data
		MyCloudEventData data = new MyCloudEventData();
		data.setMyData("Hello from Java");
		data.setMyCounter(1);

		CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
				.withId(UUID.randomUUID().toString())
				.withType("MyCloudEvent")
				.withSource(URI.create("application-a"))
				.withDataContentType("application/json; charset=UTF-8")
				.withData(objectMapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8));

		CloudEvent cloudEvent = cloudEventBuilder.build();

		logCloudEvent(cloudEvent);

		log.info("Producing CloudEvent with MyCloudEventData: " + data);

		rest.baseUrl(SINK).filter(logRequest()).build()
				.post().bodyValue(cloudEvent)
				.retrieve()
				.bodyToMono(String.class)
				.doOnError(t -> t.printStackTrace())
				.doOnSuccess(s -> log.info("Result -> " + s)).subscribe();

		return ResponseEntity.ok("OK");

	}

	@PostMapping(value = "/")
	public ResponseEntity<Void> consumeCloudEvent(@RequestBody CloudEvent cloudEvent) throws IOException {

		logCloudEvent(cloudEvent);
		if (!cloudEvent.getType().equals("MyCloudEvent")) {
			throw new IllegalStateException("Wrong Cloud Event Type, expected: 'MyCloudEvent' and got: " + cloudEvent.getType());
		}

		MyCloudEventData data = objectMapper.readValue(cloudEvent.getData().toBytes(), MyCloudEventData.class);
		// Here you can do whatever you want with your Application data:
		log.info("MyCloudEventData Data: " + data.getMyData());
		log.info("MyCloudEventData Counter: " + data.getMyCounter());
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

