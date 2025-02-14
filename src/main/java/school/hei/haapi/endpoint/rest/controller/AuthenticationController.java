package school.hei.haapi.endpoint.rest.controller; // Copyright 2022 The Casdoor

// Authors. All Rights
// Reserved.

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

import org.casbin.casdoor.exception.CasdoorAuthException;
import org.casbin.casdoor.service.CasdoorAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.security.casdoorAuthentication.model.Result;
import school.hei.haapi.endpoint.rest.security.model.Principal;

@RestController
public class AuthenticationController {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

  private final CasdoorAuthService casdoorAuthService;
  private final String redirectUrl;

  public AuthenticationController(
      CasdoorAuthService casdoorAuthService, @Value("${casdoor.redirect-url}") String redirectUrl) {
    this.casdoorAuthService = casdoorAuthService;
    this.redirectUrl = redirectUrl;
  }

  @GetMapping("/authentication/casdoor/login-url")
  public Result getRedirectUrl() {
    try {
      String signinUrl = casdoorAuthService.getSigninUrl(redirectUrl);
      return Result.success(signinUrl);
    } catch (CasdoorAuthException exception) {
      logger.error("casdoor auth exception", exception);
      return Result.failure(exception.getMessage());
    }
  }

  @PostMapping("/authentication/casdoor/signin")
  public Result signin(@RequestParam("code") String code, @RequestParam("state") String state) {
    try {
      String token = casdoorAuthService.getOAuthToken(code, state);
      return Result.success(token);
    } catch (CasdoorAuthException exception) {
      logger.error("casdoor auth exception", exception);
      return Result.failure(exception.getMessage());
    }
  }

  @GetMapping("/authentication/casdoor/userinfo")
  public Result userinfo(Authentication authentication) {
    Principal customUserDetails = (Principal) authentication.getPrincipal();
    return Result.success(customUserDetails);
  }
}
