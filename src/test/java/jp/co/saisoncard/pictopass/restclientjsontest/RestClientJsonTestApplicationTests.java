package jp.co.saisoncard.pictopass.restclientjsontest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest
class RestClientJsonTestApplicationTests {

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void objectMapper() throws Exception {
        String json = objectMapper.writeValueAsString(new Person2("Alice", LocalDate.of(1991, 1, 1)));
        assertEquals("{\"name\":\"Alice\",\"birthday\":\"1991-01-01\"}", json);
    }

    @Test
    void restTemplate(@Autowired RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        // モックサーバの設定
        server.expect(requestTo("/api/persons"))  // このURLに
                .andExpect(method(HttpMethod.POST))  // GETリクエストすると
                .andExpect(content().json("""
                            {"name":"Alice","birthday":"1991-01-01"}
                            """))
                .andRespond(withStatus(HttpStatus.OK));

        // テスト実行
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity("/api/persons",
                new Person1("Alice", LocalDate.of(1991, 1, 1)), Void.class);

        // 検証
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    static record Person1(String name, LocalDate birthday) {}

    @Test
    void restClient() {
        // タイムアウト設定
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))  // 接続タイムアウト
                .withReadTimeout(Duration.ofSeconds(5));  // 読み取りタイムアウト
        // RestClient.Builderを生成
        RestClient.Builder restClientBuilder = RestClient.builder()
                .defaultStatusHandler(
                        // ステータスコードが4xx・5xxの場合に例外が出ないようにする
                        status -> true,
                        (request, response) -> { /* 何もしない */ })
                .requestFactory(ClientHttpRequestFactories.get(settings));
        // MockRestServiceServerを生成
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        // モックサーバの設定
        server.expect(requestTo("/api/persons"))  // このURLに
                .andExpect(method(HttpMethod.POST))  // GETリクエストすると
                .andExpect(content().json("""
                            {"name":"Alice","birthday":"1991-01-01"}
                            """))
                .andRespond(withStatus(HttpStatus.OK));

        // テスト実行
        ResponseEntity<Void> responseEntity = restClient.post().uri("/api/persons")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new Person2("Alice", LocalDate.of(1991, 1, 1)))
                .retrieve()
                .toBodilessEntity();

        // 検証
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    static record Person2(String name, @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Tokyo") LocalDate birthday) {}
}
