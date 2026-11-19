package se.magnus.springcloud.eurekaserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;  // ★ 추가
import org.springframework.boot.resttestclient.TestRestTemplate;  // 교재와 다름 (교재: org.springframework.boot.test.web.client.TestRestTemplate)
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;  // 교재에는 없음 (Boot 4)
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate  // 교재에는 없음 (Boot 4)
class EurekaServerApplicationTests {

  @Test
  void contextLoads() {
  }

  // ★ 추가: yml의 계정으로 HTTP 기본 인증을 붙인 TestRestTemplate을 쓴다
  @Value("${app.eureka-username}")
  private String username;

  @Value("${app.eureka-password}")
  private String password;

  @Autowired
  void setTestRestTemplate(TestRestTemplate testRestTemplate) {
    this.testRestTemplate = testRestTemplate.withBasicAuth(username, password);
  }

  private TestRestTemplate testRestTemplate;  // ★ 변경 (전: @Autowired로 바로 주입)

  @Test
  void catalogLoads() {

    String expectedReponseBody = "{\"applications\":{\"versions__delta\":\"1\",\"apps__hashcode\":\"\",\"application\":[]}}";
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/eureka/apps", String.class);
    assertEquals(HttpStatus.OK, entity.getStatusCode());
    assertEquals(expectedReponseBody, entity.getBody());
  }

  @Test
  void healthy() {
    String expectedReponseBody = "{\"groups\":[\"liveness\",\"readiness\"],\"status\":\"UP\"}";  // 교재와 다름 (교재: {"status":"UP"}, Boot 4는 groups도 보여 줌)
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/actuator/health", String.class);
    assertEquals(HttpStatus.OK, entity.getStatusCode());
    assertEquals(expectedReponseBody, entity.getBody());
  }
}