package com.rentaltrack.core;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v2")
public class WorkflowController {

  private final WorkflowService workflow;

  public WorkflowController(WorkflowService workflow) {
    this.workflow = workflow;
  }

  @GetMapping("/catalog/items")
  public List<Map<String, Object>> catalog() {
    return workflow.catalog();
  }

  @GetMapping("/catalog/businesses")
  public List<Map<String, Object>> businesses() {
    return workflow.businessList();
  }

  @GetMapping("/catalog/codes")
  public List<Map<String, Object>> codes() {
    return workflow.codes();
  }

  @PostMapping("/businesses")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> business(
    @Valid @RequestBody Requests.BusinessInput input
  ) {
    return workflow.addBusiness(input);
  }

  @PostMapping("/items")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> item(
    @Valid @RequestBody Requests.ItemInput input
  ) {
    return workflow.addItem(input);
  }

  @PostMapping("/rentals")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> request(
    @Valid @RequestBody Requests.RentalInput input
  ) {
    return workflow.requestRental(input);
  }

  @GetMapping("/rentals")
  public List<Map<String, Object>> history() {
    return workflow.myRentals();
  }

  @GetMapping("/rentals/{id}")
  public Map<String, Object> rental(@PathVariable Long id) {
    return workflow.detail(id);
  }

  @PostMapping("/rentals/{id}/start")
  public Map<String, Object> start(
    @PathVariable Long id,
    @Valid @RequestBody Requests.StartInput input
  ) {
    return workflow.start(id, input);
  }

  @PostMapping("/rentals/{id}/return")
  public Map<String, Object> returned(
    @PathVariable Long id,
    @Valid @RequestBody Requests.ReturnInput input
  ) {
    return workflow.returnRental(id, input);
  }

  @PostMapping("/rentals/{id}/cancel")
  public Map<String, Object> cancel(@PathVariable Long id) {
    return workflow.cancel(id);
  }

  @PostMapping("/rentals/{id}/conditions")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> condition(
    @PathVariable Long id,
    @Valid @RequestBody Requests.ConditionInput input
  ) {
    return workflow.addCondition(id, input);
  }

  @PatchMapping("/conditions/{id}")
  public Map<String, Object> edit(
    @PathVariable Long id,
    @Valid @RequestBody Requests.NoteInput input
  ) {
    return workflow.editCondition(id, input);
  }

  @PostMapping("/conditions/{id}/confirm")
  public Map<String, Object> confirm(@PathVariable Long id) {
    return workflow.confirm(id);
  }

  @PostMapping("/conditions/{id}/revisions")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> revise(
    @PathVariable Long id,
    @Valid @RequestBody Requests.NoteInput input
  ) {
    return workflow.revise(id, input);
  }

  @PostMapping(
    value = "/conditions/{id}/photos",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> photo(
    @PathVariable Long id,
    @RequestParam MultipartFile file,
    @RequestParam String captureAngleCd,
    @RequestParam(required = false) LocalDateTime capturedAt
  ) {
    return workflow.addPhoto(id, file, captureAngleCd, capturedAt);
  }

  @GetMapping("/photos/{id}/content")
  public ResponseEntity<byte[]> image(@PathVariable Long id) {
    var content = workflow.photoContent(id);
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(content.mimeType()))
      .header("X-Content-Type-Options", "nosniff")
      .cacheControl(CacheControl.noStore())
      .body(content.bytes());
  }

  @PostMapping("/conditions/{id}/damages")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> damage(
    @PathVariable Long id,
    @Valid @RequestBody Requests.DamageInput input
  ) {
    return workflow.addDamage(id, input);
  }

  @PostMapping("/damages/{id}/photos")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> evidence(
    @PathVariable Long id,
    @Valid @RequestBody Requests.EvidenceInput input
  ) {
    return workflow.addEvidence(id, input);
  }

  @PatchMapping("/damages/{id}")
  public Map<String, Object> editDamage(
    @PathVariable Long id,
    @Valid @RequestBody Requests.DamageInput input
  ) {
    return workflow.editDamage(id, input);
  }

  @DeleteMapping("/damages/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDamage(@PathVariable Long id) {
    workflow.deleteDamage(id);
  }

  @PostMapping("/photos/{id}/analyses")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> analysis(
    @PathVariable Long id,
    @Valid @RequestBody Requests.AnalysisInput input
  ) {
    return workflow.addAnalysis(id, input);
  }

  @PostMapping("/rentals/{id}/inspection")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> inspection(
    @PathVariable Long id,
    @Valid @RequestBody Requests.InspectionInput input
  ) {
    return workflow.createInspection(id, input);
  }

  @PostMapping("/inspections/{id}/begin")
  public Map<String, Object> begin(@PathVariable Long id) {
    return workflow.beginInspection(id);
  }

  @PutMapping("/inspections/{id}/assessments")
  public Map<String, Object> assessment(
    @PathVariable Long id,
    @Valid @RequestBody Requests.AssessmentInput input
  ) {
    return workflow.assess(id, input);
  }

  @PostMapping("/inspections/{id}/complete")
  public Map<String, Object> complete(@PathVariable Long id) {
    return workflow.completeInspection(id);
  }
}
