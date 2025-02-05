package school.hei.haapi.endpoint.rest.mapper;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.Cor;
import school.hei.haapi.endpoint.rest.model.CrupdateCor;
import school.hei.haapi.service.UserService;

@Component
@AllArgsConstructor
public class CorMapper {
  private final UserMapper userMapper;
  private final UserService userService;

  public Cor toRest(school.hei.haapi.model.Cor cor) {
    return new Cor()
        .id(cor.getId())
        .creationDatetime(cor.getCreationDatetime())
        .concernedStudent(userMapper.toRestStudent(cor.getConcernedStudent()))
        .description(cor.getDescription());
  }

  public school.hei.haapi.model.Cor toDomain(Cor cor) {
    return new school.hei.haapi.model.Cor()
        .toBuilder()
            .concernedStudent(userMapper.toDomain(cor.getConcernedStudent()))
            .description(cor.getDescription())
            .creationDatetime(cor.getCreationDatetime())
            .build();
  }

  public school.hei.haapi.model.Cor toDomain(CrupdateCor cor, String studentId) {
    return new school.hei.haapi.model.Cor()
        .toBuilder()
            .concernedStudent(userService.findById(studentId))
            .description(cor.getDescription())
            .creationDatetime(cor.getCreationDatetime())
            .build();
  }

  public List<Cor> toRestList(List<school.hei.haapi.model.Cor> cors) {
    return cors.stream().map(this::toRest).toList();
  }

  public List<school.hei.haapi.model.Cor> toDomainList(List<CrupdateCor> cors, String studentId) {
    return cors.stream().map(cor -> toDomain(cor, studentId)).toList();
  }
}
