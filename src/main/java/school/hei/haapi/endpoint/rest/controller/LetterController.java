package school.hei.haapi.endpoint.rest.controller;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import school.hei.haapi.endpoint.rest.mapper.LetterMapper;
import school.hei.haapi.endpoint.rest.model.*;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.exception.BadRequestException;
import school.hei.haapi.service.LetterService;

@RestController
@AllArgsConstructor
public class LetterController {

  private final LetterService letterService;
  private final LetterMapper letterMapper;

  @GetMapping(value = "/letters")
  public List<Letter> getLetters(
      @RequestParam(name = "page") PageFromOne page,
      @RequestParam(name = "page_size") BoundedPageSize pageSize,
      @RequestParam(name = "ref", required = false) String ref,
      @RequestParam(name = "name", required = false) String name,
      @RequestParam(name = "status", required = false) LetterStatus status,
      @RequestParam(name = "student_ref", required = false) String studentRef,
      @RequestParam(name = "fee_id", required = false) String feeId,
      @RequestParam(name = "is_linked_with_fee", required = false) Boolean isLinkedWithFee) {
    return letterService
        .getLetters(ref, studentRef, status, name, feeId, isLinkedWithFee, page, pageSize)
        .stream()
        .map(letterMapper::toRest)
        .toList();
  }

  @GetMapping(value = "/letters/stats")
  public LetterStats getStats() {
    return letterService.getStats();
  }

  @PutMapping(value = "/letters")
  public List<Letter> updateLetter(@RequestBody List<UpdateLettersStatus> letters) {
    return letterService.updateLetter(letters).stream().map(letterMapper::toRest).toList();
  }

  @GetMapping(value = "/letters/{id}")
  public Letter getLetter(@PathVariable String id) {
    return letterMapper.toRest(letterService.getLetterById(id));
  }

  @GetMapping(value = "/students/{student_id}/letters")
  public List<Letter> getStudentLetters(
      @PathVariable(name = "student_id") String studentId,
      @RequestParam(name = "status", required = false) LetterStatus status,
      @RequestParam(name = "page") PageFromOne page,
      @RequestParam(name = "page_size") BoundedPageSize pageSize) {
    return letterService.getLettersByStudentId(studentId, status, page, pageSize).stream()
        .map(letterMapper::toRest)
        .toList();
  }

  @PostMapping(value = "/students/{student_id}/letters", consumes = MULTIPART_FORM_DATA_VALUE)
  public Letter createLetter(
      @PathVariable(name = "student_id") String studentId,
      @RequestParam(name = "amount", required = false) Integer amount,
      @RequestParam(name = "fee_id", required = false) String feeId,
      @RequestParam(name = "event_participant_id", required = false) String eventParticipantId,
      @RequestParam(name = "description") String description,
      @RequestParam(name = "filename") String filename,
      @RequestPart(name = "file_to_upload") MultipartFile file) {

    if (Objects.nonNull(feeId) && Objects.nonNull(eventParticipantId)) {
      throw new BadRequestException("Cannot link letter with both fee and event participant");
    }

    if (Objects.nonNull(feeId) && Objects.isNull(amount)) {
      throw new BadRequestException("Cannot create a letter for a fee without a given amount");
    }

    return letterMapper.toRest(
        letterService.createLetter(
            studentId, description, filename, file, feeId, amount, eventParticipantId));
  }
}
