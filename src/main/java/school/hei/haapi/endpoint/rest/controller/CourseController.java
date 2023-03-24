package school.hei.haapi.endpoint.rest.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.CourseMapper;
import school.hei.haapi.endpoint.rest.model.CodeOrder;
import school.hei.haapi.endpoint.rest.model.Course;
import school.hei.haapi.endpoint.rest.model.CreditsOrder;
import school.hei.haapi.endpoint.rest.model.CrupdateCourse;
import school.hei.haapi.model.BoundedPageSize;

import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.service.CourseService;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static java.util.stream.Collectors.toUnmodifiableList;

@RestController
@AllArgsConstructor
public class CourseController {
    private final CourseService courseService;
    private final CourseMapper courseMapper;

    @GetMapping("/courses")
    public List<Course> getAllCourse (
            @RequestParam(required = false, defaultValue="1") PageFromOne page,
            @RequestParam(required = false,name = "page_size",defaultValue = "15") BoundedPageSize pageSize,
            @RequestParam(required = false, name = "code_order") CodeOrder codeOrder,
            @RequestParam(required = false,name = "name") String name,
            @RequestParam(required = false, name = "credits_order") CreditsOrder creditsOrder,
            @RequestParam(required = false, name = "teacher_first_name") String teacherFirstName,
            @RequestParam(required = false, name = "teacher_last_name") String teacherLastName
            ) {
        if (codeOrder == null && name == null && creditsOrder == null && teacherFirstName == null
                && teacherLastName == null) {
            return courseService.getAll(page,pageSize)
                    .stream().map(courseMapper::toRestCourse)
                    .collect(Collectors.toList());
        } else{
        return courseService.getByFilter(page, pageSize, codeOrder, name,creditsOrder,
                        teacherFirstName,teacherLastName)
                .stream().map(courseMapper::toRestCourse)
                .collect(Collectors.toList()); }
    }

  @PutMapping(value = "/courses")
  public List<Course> createOrUpdateCourses(@RequestBody List<CrupdateCourse> toWrite) {
    var saved = courseService.saveAll(toWrite.stream()
        .map(courseMapper::toDomain)
        .collect(toUnmodifiableList()));
    return saved.stream()
        .map(courseMapper::toRest)
        .collect(toUnmodifiableList());
  }


}
