package school.hei.haapi.endpoint.rest.security;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;
import static school.hei.haapi.endpoint.rest.security.model.Role.MANAGER;
import static school.hei.haapi.endpoint.rest.security.model.Role.MONITOR;
import static school.hei.haapi.endpoint.rest.security.model.Role.STUDENT;
import static school.hei.haapi.endpoint.rest.security.model.Role.TEACHER;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;
import school.hei.haapi.model.exception.ForbiddenException;
import school.hei.haapi.service.AwardedCourseService;
import school.hei.haapi.service.UserService;

@Configuration
@Slf4j
@EnableWebSecurity
public class SecurityConf {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String STUDENT_COURSE = "/students/*/courses";
  private final AwardedCourseService awardedCourseService;
  private final UserService userService;
  private final AuthProvider authProvider;
  private final HandlerExceptionResolver exceptionResolver;

  public SecurityConf(
      AuthProvider authProvider,
      // InternalToExternalErrorHandler behind
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
      AwardedCourseService awardedCourseService,
      UserService userService) {
    this.authProvider = authProvider;
    this.exceptionResolver = exceptionResolver;
    this.awardedCourseService = awardedCourseService;
    this.userService = userService;
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    return new ProviderManager(authProvider);
  }

  @Bean
  public SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception {
    // @formatter:off
    AntPathRequestMatcher nonAccessibleBySuspendedUserPath =
        antMatcher(GET, "/non-accessible-by-suspended");
    httpSecurity
        .exceptionHandling(
            exceptionHandlingConfigurer ->
                exceptionHandlingConfigurer
                    .authenticationEntryPoint(
                        // note(spring-exception)
                        // https://stackoverflow.com/questions/59417122/how-to-handle-usernamenotfoundexception-spring-security
                        // issues like when a user tries to access a resource
                        // without appropriate authentication elements
                        (req, res, e) ->
                            exceptionResolver.resolveException(
                                req, res, null, forbiddenWithRemoteInfo(req)))
                    .accessDeniedHandler(
                        // note(spring-exception): issues like when a user not having required roles
                        (req, res, e) ->
                            exceptionResolver.resolveException(
                                req, res, null, forbiddenWithRemoteInfo(req))))

        // authenticate
        .authenticationProvider(authProvider)
        .addFilterBefore(
            bearerFilter(
                new OrRequestMatcher(
                    antMatcher(GET, "/whoami"),
                    antMatcher(GET, "/teachers/announcements"),
                    antMatcher(GET, "/students/announcements"),
                    antMatcher(GET, "/students/announcements/*"),
                    antMatcher(GET, "/announcements"),
                    antMatcher(GET, "/announcements/*"),
                    antMatcher(POST, "/announcements"),
                    antMatcher(POST, "/school/files/raw"),
                    antMatcher(GET, "/school/files"),
                    antMatcher(GET, "/school/files/*"),
                    antMatcher(GET, "/school/files/share_link"),
                    antMatcher(GET, "/students/*/work_files"),
                    antMatcher(GET, "/students/*/work_files/*"),
                    antMatcher(POST, "/students/*/group_flows"),
                    antMatcher(GET, "/students/*/work_files"),
                    antMatcher(GET, "/students/*/work_files/*"),
                    antMatcher(POST, "/students/*/work_files/raw"),
                    antMatcher(POST, "/users/*/files/raw"),
                    antMatcher(GET, "/users/*/files"),
                    antMatcher(GET, "/users/*/files/*"),
                    antMatcher(GET, "/monitors/*"),
                    antMatcher(PUT, "/monitors/*"),
                    antMatcher(POST, "/students/*/picture/raw"),
                    antMatcher(POST, "/teachers/*/picture/raw"),
                    antMatcher(POST, "/managers/*/picture/raw"),
                    antMatcher(GET, "/students"),
                    antMatcher(PUT, "/fees"),
                    antMatcher(GET, "/fees"),
                    antMatcher(GET, "/fees/raw"),
                    antMatcher(GET, "/fees/stats"),
                    antMatcher(GET, "/fees/*"),
                    antMatcher(PUT, "/students/*/fees/*/mpbs"),
                    antMatcher(GET, "/students/*/fees/*/mpbs"),
                    antMatcher(GET, "/students/*/fees/*/mpbs/verifications"),
                    antMatcher(GET, "/students/*/fees/*"),
                    antMatcher(GET, "/students/*/fees/*/payments/*/receipt/raw"),
                    antMatcher(DELETE, "/students/*/fees/*"),
                    antMatcher(GET, "/students/*/fees/*/payments"),
                    antMatcher(POST, "/students/*/fees/*/payments"),
                    antMatcher(DELETE, "/students/*/fees/*/payments/*"),
                    antMatcher(GET, "/students/*/fees"),
                    antMatcher(POST, "/students/*/fees"),
                    antMatcher(PUT, "/students/*/fees"),
                    antMatcher(GET, "/students/*"),
                    antMatcher(PUT, "/students/**"),
                    antMatcher(GET, "/students/*/grades"),
                    antMatcher(GET, "/monitors"),
                    antMatcher(PUT, "/monitors"),
                    antMatcher(PUT, "/monitors/*/students"),
                    antMatcher(GET, "/monitors/*/students"),
                    antMatcher(GET, "/teachers"),
                    antMatcher(GET, "/teachers/*"),
                    antMatcher(GET, "/students/*/scholarship_certificate/raw"),
                    antMatcher(PUT, "/students/**"),
                    antMatcher(GET, "/fees/templates"),
                    antMatcher(PUT, "/fees/templates/*"),
                    antMatcher(GET, "/fees/templates/*"),
                    antMatcher(GET, "/teachers"),
                    antMatcher(GET, "/teachers/*"),
                    antMatcher(GET, "/teachers/**"),
                    antMatcher(PUT, "/teachers/*"),
                    antMatcher(PUT, "/teachers/**"),
                    antMatcher(PUT, "/managers/*"),
                    antMatcher(GET, "/managers/*"),
                    antMatcher(GET, "/managers/**"),
                    antMatcher(GET, "/groups"),
                    antMatcher(GET, "/groups/*"),
                    antMatcher(GET, "/groups/*/awarded_courses"),
                    antMatcher(GET, "/groups/*/awarded_courses/*"),
                    antMatcher(PUT, "/groups/*/awarded_courses"),
                    antMatcher(GET, "/teachers/*/awarded_courses"),
                    antMatcher(PUT, "/teachers/*/awarded_courses"),
                    antMatcher(PUT, "/groups/*/awarded_courses/*/exams"),
                    antMatcher(GET, "/groups/*/awarded_courses/*/exams"),
                    antMatcher(GET, "/groups/*/awarded_courses/*/exams/*"),
                    antMatcher(GET, "/groups/*/awarded_courses/*/exams/*/grades"),
                    antMatcher(GET, "/groups/*/awarded_courses/*/exams/*/students/*/grade"),
                    antMatcher(GET, "/awarded_courses"),
                    antMatcher(GET, "/awarded_courses/*/exams"),
                    antMatcher(PUT, "/awarded_courses/*/exams"),
                    antMatcher(PUT, "/groups/*/awarded_courses/*/exams"),
                    antMatcher(GET, "/exams"),
                    antMatcher(GET, "/exams/*"),
                    antMatcher(PUT, "/exams"),
                    antMatcher(GET, "/exams/*/students/*/grade"),
                    antMatcher(PUT, "/exams/*/students/*/grade"),
                    antMatcher(GET, "/groups/*/students"),
                    antMatcher(GET, "/groups/*/students/raw"),
                    antMatcher(GET, "/groups/**"),
                    antMatcher(PUT, "/groups/**"),
                    antMatcher(GET, "/courses"),
                    antMatcher(PUT, "/courses"),
                    antMatcher(PUT, "/courses/**"),
                    antMatcher(GET, "/courses/*"),
                    antMatcher(GET, "/courses/*/exams"),
                    antMatcher(GET, "/courses/*/exams/*"),
                    antMatcher(GET, "/courses/*/exams/*/details"),
                    antMatcher(GET, "/courses/*/exams/*/participants/*"),
                    antMatcher(GET, STUDENT_COURSE),
                    antMatcher(GET, "/comments"),
                    antMatcher(GET, "/students/*/comments"),
                    antMatcher(POST, "/students/*/comments"),
                    antMatcher(GET, "/events"),
                    antMatcher(PUT, "/events"),
                    antMatcher(GET, "/events/*"),
                    antMatcher(GET, "/events/*/participants"),
                    antMatcher(PUT, "/events/*/participants"),
                    antMatcher(GET, "/events/*/stats"),
                    antMatcher(GET, "/promotions"),
                    antMatcher(PUT, "/promotions"),
                    antMatcher(GET, "/promotions/*"),
                    antMatcher(GET, "/promotions/*/students"),
                    antMatcher(PUT, "/promotions/*/groups"),
                    antMatcher(GET, "/attendance"),
                    antMatcher(POST, "/attendance/movement"),
                    antMatcher(GET, "/letters"),
                    antMatcher(PUT, "/letters"),
                    antMatcher(GET, "/letters/*"),
                    antMatcher(GET, "/users/*/letters"),
                    antMatcher(POST, "/users/*/letters"),
                    antMatcher(PUT, STUDENT_COURSE),
                    nonAccessibleBySuspendedUserPath)),
            AnonymousAuthenticationFilter.class)
        .addFilterAfter(
            new SuspendedStudentFilter(
                new OrRequestMatcher(
                    antMatcher(GET, "/school/files"),
                    antMatcher(GET, "/school/files/*"),
                    antMatcher(GET, "/school/files"),
                    antMatcher(GET, "/school/files/*"),
                    antMatcher(GET, "/students/*/work_files"),
                    antMatcher(GET, "/students/*/work_files/*"),
                    antMatcher(GET, "/users/*/files"),
                    antMatcher(GET, "/users/*/files/*"),
                    antMatcher(GET, "/students/*/scholarship_certificate/raw"),
                    antMatcher(GET, "/announcements"),
                    antMatcher(GET, "/announcements/*"),
                    nonAccessibleBySuspendedUserPath)),
            BearerAuthFilter.class)

        // authorize
        .authorizeHttpRequests(
            request ->
                request
                    .requestMatchers(
                        new OrRequestMatcher(
                            new AntPathRequestMatcher("/ping", GET.name()),
                            new AntPathRequestMatcher("/uuid-created", GET.name()),
                            new AntPathRequestMatcher("/health/db", GET.name()),
                            new AntPathRequestMatcher("/health/email", GET.name()),
                            new AntPathRequestMatcher("/health/event1", GET.name()),
                            new AntPathRequestMatcher("/health/event2", GET.name()),
                            new AntPathRequestMatcher("/health/event/uuids", POST.name()),
                            new AntPathRequestMatcher("/health/bucket", GET.name()),
                            new AntPathRequestMatcher("/**", OPTIONS.toString())))
                    .permitAll()
                    .requestMatchers(GET, "/whoami")
                    .authenticated()

                    //
                    // Announcements resources
                    //
                    .requestMatchers(GET, "/teachers/announcements")
                    .hasRole(TEACHER.getRole())
                    .requestMatchers(GET, "/teachers/announcements/*")
                    .hasRole(TEACHER.getRole())
                    .requestMatchers(GET, "/students/announcements")
                    .hasAnyRole(STUDENT.getRole(), MONITOR.getRole())
                    .requestMatchers(GET, "/students/announcements/*")
                    .hasAnyRole(STUDENT.getRole(), MONITOR.getRole())
                    .requestMatchers(GET, "/announcements")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(GET, "/announcements/*")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(POST, "/announcements")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole())
                    //
                    // Student files resources
                    //

                    .requestMatchers(POST, "/school/files/raw")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(GET, "/school/files")
                    .hasAnyRole(
                        MANAGER.getRole(), TEACHER.getRole(), STUDENT.getRole(), MONITOR.getRole())
                    .requestMatchers(GET, "/school/files/*")
                    .hasAnyRole(
                        MANAGER.getRole(), STUDENT.getRole(), TEACHER.getRole(), MONITOR.getRole())
                    .requestMatchers(GET, "/school/files/share_link")
                    .hasAnyRole(MANAGER.getRole(), STUDENT.getRole(), TEACHER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*/work_files", "students"))
                    .hasRole(STUDENT.getRole())
                    .requestMatchers(
                        new StudentMonitorMatcher(
                            GET, "/students/*/work_files", "students", userService))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*/work_files/*", "students"))
                    .hasRole(STUDENT.getRole())
                    .requestMatchers(POST, "/students/*/group_flows")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(GET, "/students/*/work_files")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(GET, "/students/*/work_files/*")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(
                        new StudentMonitorMatcher(
                            GET, "/students/*/work_files/*", "students", userService))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(POST, "/students/*/work_files/raw")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(POST, "/users/*/files/raw")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/users/*/files", "users"))
                    .hasAnyRole(STUDENT.getRole(), TEACHER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/users/*/files/*", "users"))
                    .hasAnyRole(STUDENT.getRole(), TEACHER.getRole())
                    .requestMatchers(
                        new StudentMonitorMatcher(GET, "/users/*/files", "users", userService))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(
                        new StudentMonitorMatcher(GET, "/users/*/files/*", "users", userService))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(GET, "/users/*/files")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/users/*/files/*")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(POST, "/students/*/picture/raw")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(POST, "/teachers/*/picture/raw", "teachers"))
                    .hasRole(TEACHER.getRole())
                    .requestMatchers(POST, "/teachers/*/picture/raw")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(POST, "/managers/*/picture/raw", "managers"))
                    .hasRole(MANAGER.getRole())
                    // STUDENTS
                    .requestMatchers(GET, "/students")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole(), MONITOR.getRole())
                    // STUDENTS
                    //
                    // MONITORS FOLLOWING STUDENTS
                    .requestMatchers(PUT, "/monitors/*/students")
                    .hasAnyRole(MANAGER.getRole(), MONITOR.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/monitors/*/students", "monitors"))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(GET, "/monitors/*/students")
                    .hasRole(MANAGER.getRole())
                    //
                    // Fees resources
                    //
                    .requestMatchers(GET, "/fees/templates")
                    .hasAnyRole(STUDENT.getRole(), MANAGER.getRole())
                    .requestMatchers(PUT, "/fees/templates/*")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/fees/templates/*")
                    .hasAnyRole(MANAGER.getRole(), STUDENT.getRole())
                    .requestMatchers(PUT, "/fees")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/fees/*")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(GET, "/fees")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/fees/raw")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(GET, "/fees/stats")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*/fees/*/mpbs", "students"))
                    .hasRole(STUDENT.getRole())
                    .requestMatchers(
                        new StudentMonitorMatcher(
                            GET, "/students/*/fees/*/mpbs", "students", userService))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(new SelfMatcher(PUT, "/students/*/fees/*/mpbs", "students"))
                    .hasRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*/fees/*/mpbs")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(PUT, "/students/*/fees/*/mpbs")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(
                        new SelfMatcher(GET, "/students/*/fees/*/mpbs/verifications", "students"))
                    .hasRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*/fees/*/mpbs/verifications")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*/fees/*", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(
                        new StudentMonitorMatcher(
                            GET, "/students/*/fees/*", "students", userService))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(DELETE, "/students/*/fees/*")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(GET, "/students/*/fees/*")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*/fees", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(
                        new StudentMonitorMatcher(GET, "/students/*/fees", "students", userService))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(
                        new SelfMatcher(GET, "/students/*/fees/*/payments", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(
                        new StudentMonitorMatcher(
                            GET, "/students/*/fees/*/payments", "students", userService))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(DELETE, "/students/*/fees/*/payments/*")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(
                        new SelfMatcher(
                            GET, "/students/*/fees/*/payments/*/receipt/raw", "students"))
                    .hasRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*/fees/*/payments/*/receipt/raw")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*/fees/*", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*/fees/*")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*/fees", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(
                        new SelfMatcher(GET, "/students/*/fees/*/payments", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*/fees/*/mpbs", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*/fees/*/mpbs")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(PUT, "/students/*/fees/*/mpbs", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(
                        new SelfMatcher(GET, "/students/*/fees/*/mpbs/verifications", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*/fees/*/mpbs_verifications")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/students/*/fees/*/payments")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(POST, "/students/*/fees/*/payments")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/students/*/fees")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(POST, "/students/*/fees", "students"))
                    .hasRole(STUDENT.getRole())
                    .requestMatchers(POST, "/students/*/fees")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(PUT, "/students/*/fees")
                    //
                    // Payments resources
                    //
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(
                        new SelfMatcher(GET, "/students/*/fees/*/payments", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*/fees/*/payments")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(POST, "/students/*/fees/*/payments")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    // TODO: clarify PUT STUDENTS/** FOR MANAGERS
                    .requestMatchers(PUT, "/students/**")
                    .hasAnyRole(MANAGER.getRole())
                    //
                    // Grades resources
                    //
                    .requestMatchers(new SelfMatcher(GET, "/students/*/grades", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*/grades")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/teachers")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/teachers/*", "teachers"))
                    .hasAnyRole(TEACHER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/monitors/*", "monitors"))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(GET, "/monitors/*")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(PUT, "/monitors/*")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(
                        new SelfMatcher(GET, "/students/*/fees/*/payments", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*/fees/*/payments")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(POST, "/students/*/fees/*/payments")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())

                    // scholarship security conf
                    .requestMatchers(
                        new SelfMatcher(GET, "/students/*/scholarship_certificate/raw", "students"))
                    .hasRole(STUDENT.getRole())
                    .requestMatchers(
                        new StudentMonitorMatcher(
                            GET,
                            "/students/*/scholarship_certificate/raw",
                            "students",
                            userService))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(GET, "/students/*/scholarship_certificate/raw")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(PUT, "/students/**")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*/grades", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/students/*/grades")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/exams/*/students/*/grade")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(PUT, "/exams/*/students/*/grade")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/fees")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/teachers")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/teachers/*", "teachers"))
                    .hasAnyRole(TEACHER.getRole())
                    .requestMatchers(GET, "/teachers/*")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(PUT, "/teachers/*", "teachers"))
                    .hasRole(TEACHER.getRole())
                    .requestMatchers(PUT, "/teachers/**")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(PUT, "/managers/*", "managers"))
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(GET, "/monitors")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(PUT, "/monitors")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers("/managers/**")
                    .hasRole(MANAGER.getRole())
                    .requestMatchers(GET, "/groups")
                    .authenticated()
                    .requestMatchers(GET, "/groups/*")
                    .authenticated()
                    .requestMatchers(GET, "/groups/*/awarded_courses")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/groups/*/awarded_courses/*")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(PUT, "/groups/*/awarded_courses")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/teachers/*/awarded_courses")
                    .hasAnyRole(STUDENT.getRole(), TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(PUT, "/teachers/*/awarded_courses")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(
                        new AwardedCourseOfTeacherMatcher(
                            awardedCourseService, PUT, "/groups/*/awarded_courses/*/exams"))
                    .hasAnyRole(TEACHER.getRole())
                    .requestMatchers(GET, "/groups/*/awarded_courses/*/exams")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/groups/*/awarded_courses/*/exams/*")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/groups/*/awarded_courses/*/exams/*/grades")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(
                        new SelfMatcher(
                            GET,
                            "/groups/*/awarded_courses/*/exams/*/students/*/grade",
                            "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/groups/*/awarded_courses/*/exams/*/students/*/grade")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/awarded_courses")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/awarded_courses/*/exams")
                    .authenticated()
                    .requestMatchers(PUT, "/awarded_courses/*/exams")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/exams")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/exams/*")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(PUT, "/exams")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(
                        new AwardedCourseOfTeacherMatcher(
                            awardedCourseService, PUT, "/groups/*" + "/awarded_courses/*/exams"))
                    .hasAnyRole(TEACHER.getRole())
                    .requestMatchers(GET, "/groups/*/awarded_courses/*/exams")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/groups/*/awarded_courses/*/exams/*")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/groups/*/awarded_courses/*/exams/*/grades")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(
                        new SelfMatcher(
                            GET,
                            "/groups/*/awarded_courses/*" + "/exams/*/students/*/grade",
                            "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/groups/*/awarded_courses/*/exams/*/students/*/grade")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/groups/*/students")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/groups/*/students/raw")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/groups/**")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(PUT, "/groups/**")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/courses")
                    .authenticated()
                    .requestMatchers(PUT, "/courses")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(PUT, "/courses/**")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/courses/*")
                    .authenticated()
                    .requestMatchers(GET, "/courses/*/exams")
                    .authenticated()
                    .requestMatchers(GET, "/courses/*/exams/*")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/courses/*/exams/*/details")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(
                        new SelfMatcher(GET, "/courses/*/exams/*/participants/*", "participants"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/courses/*/exams/*/participants/*")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, STUDENT_COURSE, "students"))
                    .hasAnyRole(STUDENT.getRole())
                    //
                    // Comments resources
                    //
                    .requestMatchers(GET, "/comments")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/students/*/comments", "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(
                        new StudentMonitorMatcher(
                            GET, "/students/*/comments", "students", userService))
                    .hasRole(MONITOR.getRole())
                    .requestMatchers(GET, "/students/*/comments")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole())
                    .requestMatchers(POST, "/students/*/comments")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole())
                    //
                    // Letter resources
                    //
                    .requestMatchers(GET, "/letters")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole())
                    .requestMatchers(PUT, "/letters")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/letters/*")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole())
                    .requestMatchers(new SelfMatcher(POST, "/users/*/letters", "users"))
                    .hasAnyRole(STUDENT.getRole(), TEACHER.getRole())
                    .requestMatchers(new SelfMatcher(GET, "/users/*/letters", "users"))
                    .hasAnyRole(STUDENT.getRole(), TEACHER.getRole())
                    .requestMatchers(GET, "/users/*/letters")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(POST, "/users/*/letters")
                    .hasAnyRole(MANAGER.getRole())
                    //
                    // Event resources
                    //

                    .requestMatchers(GET, "/events")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole(), STUDENT.getRole())
                    .requestMatchers(PUT, "/events")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(GET, "/events/*")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole(), STUDENT.getRole())
                    .requestMatchers(GET, "/events/*/participants")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole(), STUDENT.getRole())
                    .requestMatchers(PUT, "/events/*/participants")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole())
                    .requestMatchers(GET, "/events/*/stats")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole())
                    .requestMatchers(GET, "/promotions")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole(), STUDENT.getRole())
                    .requestMatchers(PUT, "/promotions")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers("/promotions/*")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole(), STUDENT.getRole())
                    .requestMatchers(GET, "/promotions/*/students")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole())
                    .requestMatchers(PUT, "/promotions/*/groups")
                    .hasAnyRole(MANAGER.getRole())
                    //
                    // Attendances resources
                    //
                    .requestMatchers(GET, "/attendance")
                    .hasAnyRole(MANAGER.getRole(), TEACHER.getRole())
                    // .requestMatchers(new SelfMatcher(GET, "/attendance", "students"))
                    // .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(POST, "/attendance/movement")
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, STUDENT_COURSE, "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/courses/*")
                    .authenticated()
                    .requestMatchers(GET, "/courses/*/exams")
                    .authenticated()
                    .requestMatchers(GET, "/courses/*/exams/*")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(GET, "/courses/*" + "/exams/*/details")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(
                        new SelfMatcher(
                            GET, "/courses/*" + "/exams/*/participants/*", "participants"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, "/courses/*" + "/exams/*/participants/*")
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(new SelfMatcher(GET, STUDENT_COURSE, "students"))
                    .hasAnyRole(STUDENT.getRole())
                    .requestMatchers(GET, STUDENT_COURSE)
                    .hasAnyRole(TEACHER.getRole(), MANAGER.getRole())
                    .requestMatchers(PUT, STUDENT_COURSE)
                    .hasAnyRole(MANAGER.getRole())
                    .requestMatchers(nonAccessibleBySuspendedUserPath)
                    .authenticated()
                    .requestMatchers("/**")
                    .denyAll())

        // disable superfluous protections
        // Eg if all clients are non-browser then no csrf
        // https://docs.spring.io/spring-security/site/docs/3.2.0.CI-SNAPSHOT/reference/html/csrf.html,
        // Sec 13.3
        .cors(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);
    // formatter:on
    return httpSecurity.build();
  }

  private Exception forbiddenWithRemoteInfo(HttpServletRequest req) {
    log.info(
        String.format(
            "Access is denied for remote caller: address=%s, host=%s, port=%s",
            req.getRemoteAddr(), req.getRemoteHost(), req.getRemotePort()));
    return new ForbiddenException("Access is denied");
  }

  private BearerAuthFilter bearerFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
    BearerAuthFilter bearerFilter =
        new BearerAuthFilter(requiresAuthenticationRequestMatcher, AUTHORIZATION_HEADER);
    bearerFilter.setAuthenticationManager(authenticationManager());
    bearerFilter.setAuthenticationSuccessHandler(
        (httpServletRequest, httpServletResponse, authentication) -> {});
    bearerFilter.setAuthenticationFailureHandler(
        (req, res, e) ->
            // note(spring-exception)
            // issues like when a user is not found(i.e. UsernameNotFoundException)
            // or other exceptions thrown inside authentication provider.
            // In fact, this handles other authentication exceptions that are
            // not handled by AccessDeniedException and AuthenticationEntryPoint
            exceptionResolver.resolveException(req, res, null, forbiddenWithRemoteInfo(req)));
    return bearerFilter;
  }
}
