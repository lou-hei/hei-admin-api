package school.hei.haapi.mapper;

import static school.hei.haapi.web.model.ErrorResource.Type.BAD_REQUEST;
import static school.hei.haapi.web.model.ErrorResource.Type.FORBIDDEN;
import static school.hei.haapi.web.model.ErrorResource.Type.INTERNAL_ERROR;
import static school.hei.haapi.web.model.ErrorResource.Type.NOT_FOUND;
import static school.hei.haapi.web.model.ErrorResource.Type.NOT_IMPLEMENTED;
import static school.hei.haapi.web.model.ErrorResource.Type.TOO_MANY_REQUESTS;

import org.springframework.stereotype.Component;
import school.hei.haapi.exception.BadRequestException;
import school.hei.haapi.exception.ForbiddenException;
import school.hei.haapi.exception.NotFoundException;
import school.hei.haapi.exception.NotImplementedException;
import school.hei.haapi.exception.TooManyRequestsException;
import school.hei.haapi.web.model.ErrorResource;

@Component
public class ErrorMapper {

  public ErrorResource mapToExternal(BadRequestException e) {
    return new ErrorResource(BAD_REQUEST, e.getMessage());
  }

  public ErrorResource mapToExternal(TooManyRequestsException e) {
    return new ErrorResource(TOO_MANY_REQUESTS, e.getMessage());
  }

  public ErrorResource mapToExternal(ForbiddenException e) {
    return new ErrorResource(FORBIDDEN, e.getMessage());
  }

  public ErrorResource mapToExternal(NotFoundException e) {
    return new ErrorResource(NOT_FOUND, e.getMessage());
  }

  public ErrorResource mapToExternal(Exception e) {
    return new ErrorResource(INTERNAL_ERROR, e.getMessage());
  }

  public ErrorResource mapToExternal(NotImplementedException e) {
    return new ErrorResource(NOT_IMPLEMENTED, e.getMessage());
  }
}
