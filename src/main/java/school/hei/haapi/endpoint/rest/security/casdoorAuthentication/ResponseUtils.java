// Copyright 2022 The Casdoor Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package school.hei.haapi.endpoint.rest.security.casdoorAuthentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import school.hei.haapi.endpoint.rest.security.casdoorAuthentication.model.Result;

public class ResponseUtils {

  private static final Logger logger = LoggerFactory.getLogger(ResponseUtils.class);

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static void success(HttpServletResponse response, Object data) {
    Result result = new Result(200, "ok", data);
    writeResultToResponse(response, result);
  }

  public static void fail(HttpServletResponse response, String message) {
    Result result = new Result(500, message, null);
    writeResultToResponse(response, result);
  }

  private static void writeResultToResponse(HttpServletResponse response, Result result) {
    try {
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      PrintWriter writer = response.getWriter();
      String json = objectMapper.writeValueAsString(result);
      writer.write(json);
      writer.flush();
    } catch (Exception e) {
      logger.error("failed to write to the response stream", e);
    }
  }
}
