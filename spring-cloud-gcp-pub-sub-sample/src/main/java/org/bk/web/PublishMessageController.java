package org.bk.web;

import org.bk.model.Message;
import org.bk.service.PubSubService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/messages")
public class PublishMessageController {

    private final PubSubService pubSubService;

    public PublishMessageController(PubSubService pubSubService) {
        this.pubSubService = pubSubService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Message>> publishMessage(@RequestBody Message message) {
        return pubSubService.publish(message).thenReturn(ResponseEntity.ok(message));
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Message>> getMessages() {
        return pubSubService.retrieve().map(message -> ServerSentEvent.builder(message).build());
    }
}
