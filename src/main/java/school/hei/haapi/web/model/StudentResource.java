package school.hei.haapi.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResource {
  String id;
  String firstName;
  String lastName;
  String email;
  String role;
}
