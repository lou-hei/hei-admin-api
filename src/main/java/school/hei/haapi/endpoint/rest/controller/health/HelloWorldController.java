package school.hei.haapi.endpoint.rest.controller.health;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.PojaGenerated;
import school.hei.haapi.repository.DummyRepository;
import school.hei.haapi.repository.DummyUuidRepository;

@PojaGenerated
@RestController
@AllArgsConstructor
public class HelloWorldController {

  @GetMapping("/hello-world")
  public String hello() {
    return "hello world";
  }
}
