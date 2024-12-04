package school.hei.haapi.service.ownCloud;

import static org.springframework.http.HttpMethod.POST;
import static school.hei.haapi.endpoint.rest.security.AuthProvider.getPrincipal;
import static school.hei.haapi.service.utils.OwnCloudUtils.getBasicAuth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import school.hei.haapi.model.User;
import school.hei.haapi.model.notEntity.OcsData;

@Service
@AllArgsConstructor
public class OwnCloudService {

  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;
  private final OwnCloudConf conf;

  public OcsData createShareLink(String path) throws JsonProcessingException {
    User currentUser = getPrincipal().getUser();
    Integer permission =
        switch (currentUser.getRole()) {
          case ADMIN, MANAGER -> 15;
          case STUDENT, TEACHER, MONITOR, STAFF_MEMBER -> 1;
        };

    HttpHeaders headers = new HttpHeaders();
    String authHeader = getBasicAuth(conf.getUsername(), conf.getPassword());
    headers.set("Authorization", authHeader);

    HttpEntity<Void> entity = new HttpEntity<>(null, headers);
    ResponseEntity<String> response =
        restTemplate.exchange(conf.getURI(path, permission), POST, entity, String.class);

    return objectMapper.readValue(response.getBody(), OcsData.class);
  }
}
