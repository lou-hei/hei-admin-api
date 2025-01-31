package school.hei.haapi.endpoint.rest.controller.health;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.PojaGenerated;

@PojaGenerated
@RestController
@AllArgsConstructor
public class HelloWorldController {

  @GetMapping("/hello-world")
  public String hello() {
    return "hello hei";
  }
}
