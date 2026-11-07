package com.rentaltrack.legacy;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/admin/legacy-import")
public class LegacyImportController {

  private final LegacyImportService service;

  public LegacyImportController(LegacyImportService service) {
    this.service = service;
  }

  @GetMapping("/source")
  public Map<String, Object> source() {
    return service.source();
  }

  @PostMapping("/preview")
  public Map<String, Object> preview(
    @Valid @RequestBody LegacyManifest manifest
  ) {
    return service.preview(manifest);
  }

  @PostMapping("/apply")
  public Map<String, Object> apply(
    @Valid @RequestBody LegacyManifest.ApplyInput input
  ) {
    return service.apply(input);
  }
}
