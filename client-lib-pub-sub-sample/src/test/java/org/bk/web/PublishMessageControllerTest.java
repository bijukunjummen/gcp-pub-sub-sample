package org.bk.web;

import org.bk.model.Message;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@WebFluxTest(controllers = PublishMessageController.class)
class PublishMessageControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testBasicPublish() {
        webTestClient.post().uri("/messages")
                .body(fromValue(new Message("1", "one")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(""" 
                        {
                        "id": "1",
                        "payload": "one"
                        } """);
    }

}
