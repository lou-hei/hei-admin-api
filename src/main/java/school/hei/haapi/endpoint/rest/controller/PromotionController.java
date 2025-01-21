package school.hei.haapi.endpoint.rest.controller;

import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.PromotionMapper;
import school.hei.haapi.endpoint.rest.model.CrupdatePromotion;
import school.hei.haapi.endpoint.rest.model.Promotion;
import school.hei.haapi.endpoint.rest.model.UpdatePromotionSGroup;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.service.PromotionService;
import school.hei.haapi.service.UserService;

@AllArgsConstructor
@RestController
public class PromotionController {
  private final UserService userService;
  private final PromotionService promotionService;
  private final PromotionMapper promotionMapper;

  @GetMapping("/promotions")
  public List<Promotion> getPromotions(
      @RequestParam(name = "page") PageFromOne page,
      @RequestParam(name = "page_size") BoundedPageSize pageSize,
      @RequestParam(required = false) String ref,
      @RequestParam(name = "group_ref", required = false) String groupRef,
      @RequestParam(required = false) String name) {
    return promotionService.getPromotions(name, ref, groupRef, page, pageSize).stream()
        .map(promotionMapper::toRest)
        .toList();
  }

  @PutMapping("/promotions")
  public Promotion crupdatePromotion(@RequestBody CrupdatePromotion rest) {
    return promotionMapper.toRest(
        promotionService.crupdatePromotion(promotionMapper.toDomain(rest)));
  }

  @GetMapping("/promotions/{id}")
  public Promotion getPromotionById(@PathVariable String id) {
    return promotionMapper.toRest(promotionService.getPromotionById(id));
  }

  @GetMapping(value = "/promotions/{id}/students", produces = "application/vnd.ms-excel")
  public byte[] getStudentsByPromotion(@PathVariable(name = "id") String promotionId)
      throws IOException {
    return promotionService.getStudentsPromotionInXlsx(promotionId);
  }

  @PutMapping("/promotions/{id}/groups")
  public Promotion updatePromotionSGroup(
      @PathVariable String id, @RequestBody UpdatePromotionSGroup updatePromotionSGroup) {
    return promotionMapper.toRest(promotionService.updateGroups(id, updatePromotionSGroup));
  }

  @GetMapping(
          value = "/promotion/{promotion_id}/students/raw/xlsx",
          produces = "application/vnd.ms-excel")
  public byte[] generateStudentsPromotionInXlsx(
          @PathVariable(name = "promotion_id") String promotionId) {
    return userService.generateStudentsInPromotionXlsx(promotionId);
  }
}
