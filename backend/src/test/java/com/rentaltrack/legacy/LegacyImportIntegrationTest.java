package com.rentaltrack.legacy;

import static org.assertj.core.api.Assertions.*;

import com.rentaltrack.auth.api.LoginRequest;
import com.rentaltrack.auth.application.AuthService;
import com.rentaltrack.auth.application.JwtTokenProvider;
import com.rentaltrack.auth.domain.AuthenticatedMember;
import com.rentaltrack.core.WorkflowService;
import com.rentaltrack.member.domain.*;
import com.rentaltrack.member.repository.MemberRepository;
import com.rentaltrack.storage.application.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LegacyImportIntegrationTest {

  @Autowired
  LegacyImportService importer;

  @Autowired
  WorkflowService workflow;

  @Autowired
  MemberRepository members;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  AuthService auth;

  @Autowired
  JwtTokenProvider jwt;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  ImageStorage storage;

  @LocalServerPort
  int port;

  static final AtomicLong seq = new AtomicLong(
    (System.currentTimeMillis() % 7_000_000_000L) + 2_000_000_000L
  );
  Member admin, outsider;
  Long business, legacyMember, legacyOperator, product, contract, before, after, beforeDamage, afterDamage, inspection;
  String legacyEmail, hash, beforeKey, afterKey;
  final List<String> files = new ArrayList<>();

  @BeforeEach
  void seed() throws Exception {
    admin = account(MemberRole.ADMIN);
    outsider = account(MemberRole.CUSTOMER);
    as(admin);
    business = id(
      workflow.addBusiness(
        new com.rentaltrack.core.Requests.BusinessInput(
          String.valueOf(seq.incrementAndGet()),
          "Migration fixture owner",
          null
        )
      )
    );
    hash = encoder.encode("legacy-password123!");
    legacyEmail = "legacy" + seq.incrementAndGet() + "@example.com";
    legacyMember = insert(
      "INSERT INTO member_account(email,name,status,role,password_hash) VALUES (?,'Old customer','ACTIVE','USER',?)",
      legacyEmail,
      hash
    );
    legacyOperator = insert(
      "INSERT INTO member_account(email,name,status,role,password_hash) VALUES (?,'Old inspector','ACTIVE','OPERATOR',?)",
      "oldinspector" + seq.incrementAndGet() + "@example.com",
      hash
    );
    Long location = insert(
      "INSERT INTO rental_location(code,name,address,latitude,longitude) VALUES (?,'Old location','Original address',37,127)",
      "L" + seq.incrementAndGet()
    );
    Long category = insert(
      "INSERT INTO product_category(code,name) VALUES (?,'Original category')",
      "C" + seq.incrementAndGet()
    );
    product = insert(
      "INSERT INTO rental_product(asset_code,display_name,category_id,current_location_id,status) VALUES (?,'Legacy camera',?,?,'AVAILABLE')",
      "ASSET" + seq.incrementAndGet(),
      category,
      location
    );
    contract = insert(
      "INSERT INTO legacy_rental_contract(contract_code,member_id,rental_product_id,pickup_location_id,status,requested_at,approved_at,picked_up_at,rented_at,due_at,returned_at,completed_at) VALUES (?,?,?,?,'COMPLETED','2026-08-01 10:00:00','2026-08-01 10:01:00','2026-08-01 10:05:00','2026-08-01 10:10:00','2026-08-01 18:00:00','2026-08-01 17:00:00','2026-08-01 17:20:00')",
      "OLD" + seq.incrementAndGet(),
      legacyMember,
      product,
      location
    );
    before = insert(
      "INSERT INTO legacy_condition_record(rental_contract_id,record_type,summary,recorded_at) VALUES (?,'BEFORE_RENTAL','Original intake notes','2026-08-01 10:06:00')",
      contract
    );
    after = insert(
      "INSERT INTO legacy_condition_record(rental_contract_id,record_type,summary,recorded_at) VALUES (?,'AFTER_RENTAL','Original return notes','2026-08-01 17:05:00')",
      contract
    );
    beforeKey = photo(before);
    afterKey = photo(after);
    beforeDamage = insert(
      "INSERT INTO damage_record(condition_record_id,damage_type,damage_location,severity,description,comparison_result) VALUES (?,'SCRATCH','FRONT','MINOR','Original scratch','BASELINE')",
      before
    );
    afterDamage = insert(
      "INSERT INTO damage_record(condition_record_id,damage_type,damage_location,severity,description,comparison_result,matched_before_damage_id) VALUES (?,'SCRATCH','FRONT','MINOR','Same scratch','EXISTING',?)",
      after,
      beforeDamage
    );
    inspection = insert(
      "INSERT INTO legacy_return_inspection(rental_contract_id,result,note,inspected_at) VALUES (?,'NO_NEW_DAMAGE','Original inspection note','2026-08-01 17:20:00')",
      contract
    );
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    for (String key : files) storage.delete(key);
  }

  Member account(MemberRole role) {
    String name = "new" + seq.incrementAndGet();
    Member m = new Member(
      name,
      name + "@example.com",
      name,
      encoder.encode("current-password123!")
    );
    m.changeRole(role);
    return members.saveAndFlush(m);
  }

  void as(Member m) {
    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(
        new AuthenticatedMember(
          m.getId(),
          m.getEmail(),
          m.getName(),
          m.getRole()
        ),
        null,
        List.of()
      )
    );
  }

  static Long id(Map<String, Object> view) {
    return ((Number) view.get("id")).longValue();
  }

  Long insert(String sql, Object... args) {
    var key = new GeneratedKeyHolder();
    jdbc.update(c -> {
      var s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
      return s;
    }, key);
    return key.getKey().longValue();
  }

  String photo(Long condition) throws Exception {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(
      new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB),
      "png",
      bytes
    );
    StoredImage file = storage.store(
      new ImageUpload(
        "legacy-test/" + UUID.randomUUID(),
        "original.png",
        "image/png",
        bytes.toByteArray()
      )
    );
    files.add(file.storageKey());
    insert(
      "INSERT INTO condition_image(condition_record_id,storage_key,capture_order,viewpoint) VALUES (?,?,1,'FRONT')",
      condition,
      file.storageKey()
    );
    return file.storageKey();
  }

  LegacyManifest manifest() {
    return new LegacyManifest(
      List.of(),
      List.of(
        new LegacyManifest.ProductMapping(product, business, "CAMERA", null)
      ),
      List.of(
        new LegacyManifest.InspectorMapping(inspection, null, legacyOperator)
      ),
      List.of()
    );
  }

  String token(LegacyManifest m) {
    var plan = importer.preview(m);
    assertThat(plan.get("issues")).as(plan.toString()).isEqualTo(List.of());
    return (String) plan.get("planToken");
  }

  Map<String, Object> apply(LegacyManifest m) {
    return importer.apply(new LegacyManifest.ApplyInput(m, token(m)));
  }

  Long mapped(String table, Long id) {
    return jdbc.queryForObject(
      "SELECT target_id FROM legacy_entity_map WHERE source_table=? AND source_id=?",
      Long.class,
      table,
      id
    );
  }

  @SuppressWarnings("unchecked")
  boolean hasIssue(Map<String, Object> report, String code) {
    return ((List<Map<String, Object>>) report.get("issues"))
      .stream()
      .anyMatch(i -> code.equals(i.get("code")));
  }

  @Test
  void completedHistoryBecomesVisibleToItsMigratedMemberWithOriginalPasswordAndPhotos() {
    var result = apply(manifest());
    assertThat(result.get("status")).isEqualTo("IMPORTED");
    Long member = mapped("member_account", legacyMember),
      newContract = mapped("legacy_rental_contract", contract),
      newBefore = mapped("legacy_condition_record", before),
      newAfter = mapped("legacy_condition_record", after);
    assertThat(
      jdbc.queryForObject(
        "SELECT password_hash FROM member_credential WHERE member_id=?",
        String.class,
        member
      )
    ).isEqualTo(hash);
    assertThat(
      auth
        .login(
          new LoginRequest("legacy-" + legacyMember, "legacy-password123!")
        )
        .memberId()
    ).isEqualTo(member);
    as(members.findById(member).orElseThrow());
    assertThat(workflow.myRentals())
      .extracting(r -> r.get("id"))
      .contains(newContract);
    Map<String, Object> detail = workflow.detail(newContract);
    assertThat(
      ((Map<?, ?>) detail.get("rental")).get("contractStatusCd")
    ).isEqualTo("INSPECTED");
    assertThat(
      ((Map<?, ?>) ((Map<?, ?>) detail.get("rental")).get("legacySource")).get(
        "originalStatus"
      )
    ).isEqualTo("COMPLETED");
    assertThat(
      jdbc.queryForObject(
        "SELECT confirmed_yn FROM condition_record WHERE condition_id=?",
        String.class,
        newBefore
      )
    ).isEqualTo("Y");
    assertThat(
      jdbc.queryForObject(
        "SELECT return_condition_id FROM return_inspection WHERE inspection_id=?",
        Long.class,
        mapped("legacy_return_inspection", inspection)
      )
    ).isEqualTo(newAfter);
    assertThat(
      jdbc.queryForObject(
        "SELECT inspector_member_id FROM return_inspection WHERE inspection_id=?",
        Long.class,
        mapped("legacy_return_inspection", inspection)
      )
    ).isEqualTo(mapped("member_account", legacyOperator));
    assertThat(
      jdbc.queryForObject(
        "SELECT intake_damage_id FROM damage_assessment WHERE return_damage_id=?",
        Long.class,
        mapped("damage_record", afterDamage)
      )
    ).isEqualTo(mapped("damage_record", beforeDamage));
    Long photo = jdbc.queryForObject(
      "SELECT photo_id FROM condition_photo WHERE condition_id=?",
      Long.class,
      newBefore
    );
    assertThat(workflow.photoContent(photo).bytes()).isEqualTo(
      storage.load(beforeKey)
    );
    assertThat(
      jdbc.queryForObject(
        "SELECT summary FROM legacy_condition_record WHERE id=?",
        String.class,
        before
      )
    ).isEqualTo("Original intake notes");
    as(outsider);
    assertThatThrownBy(() -> workflow.detail(newContract)).isInstanceOf(
      com.rentaltrack.global.error.AccessDeniedException.class
    );
  }

  @Test
  void exactRetryDoesNotDuplicateRowsOrOverwriteLaterEdits() {
    LegacyManifest m = manifest();
    String token = token(m);
    var first = importer.apply(new LegacyManifest.ApplyInput(m, token));
    Long target = mapped("rental_product", product);
    jdbc.update(
      "UPDATE rental_item SET item_status_cd='INACTIVE' WHERE item_id=?",
      target
    );
    var second = importer.apply(new LegacyManifest.ApplyInput(m, token));
    assertThat(second.get("status")).isEqualTo("ALREADY_IMPORTED");
    assertThat(second.get("runId")).isEqualTo(first.get("runId"));
    assertThat(
      jdbc.queryForObject(
        "SELECT item_status_cd FROM rental_item WHERE item_id=?",
        String.class,
        target
      )
    ).isEqualTo("INACTIVE");
  }

  @Test
  void ownerCategoryAndHistoricalInspectorMustBeExplicit() {
    var m = new LegacyManifest(
      List.of(),
      List.of(new LegacyManifest.ProductMapping(product, null, null, null)),
      List.of(),
      List.of()
    );
    var p = importer.preview(m);
    assertThat(hasIssue(p, "BUSINESS_REQUIRED")).isTrue();
    assertThat(hasIssue(p, "CATEGORY_REQUIRED")).isTrue();
    assertThat(hasIssue(p, "INSPECTOR_REQUIRED")).isTrue();
    assertThatThrownBy(() ->
      importer.apply(
        new LegacyManifest.ApplyInput(m, (String) p.get("planToken"))
      )
    ).isInstanceOf(IllegalStateException.class);
    assertThat(
      jdbc.queryForObject(
        "SELECT COUNT(*) FROM legacy_entity_map WHERE source_table='rental_product' AND source_id=?",
        Integer.class,
        product
      )
    ).isZero();
  }

  @Test
  void sourceOrImageChangesAfterPreviewRequireANewPreview() {
    LegacyManifest m = manifest();
    String token = token(m);
    jdbc.update(
      "UPDATE legacy_condition_record SET summary='Changed after preview' WHERE id=?",
      before
    );
    assertThatThrownBy(() ->
      importer.apply(new LegacyManifest.ApplyInput(m, token))
    ).hasMessageContaining("changed");
    storage.delete(beforeKey);
    assertThat(hasIssue(importer.preview(m), "PHOTO_UNAVAILABLE")).isTrue();
  }

  @Test
  void nonstandardDamageLocationBlocksUntilAnExplicitMappingIsProvided() {
    jdbc.update(
      "UPDATE damage_record SET damage_location='UPPER_LEFT_CORNER' WHERE id=?",
      afterDamage
    );
    var m = manifest();
    assertThat(
      hasIssue(importer.preview(m), "DAMAGE_LOCATION_REQUIRED")
    ).isTrue();
    var mapped = new LegacyManifest(
      m.members(),
      m.products(),
      m.inspections(),
      List.of(new LegacyManifest.LocationMapping("UPPER_LEFT_CORNER", "EDGE"))
    );
    apply(mapped);
    assertThat(
      jdbc.queryForObject(
        "SELECT damage_location_cd FROM damage_observation WHERE damage_id=?",
        String.class,
        mapped("damage_record", afterDamage)
      )
    ).isEqualTo("EDGE");
  }

  @Test
  void finalResultCannotContradictTheRecordedDamageComparisons() {
    jdbc.update(
      "UPDATE legacy_return_inspection SET result='NEW_DAMAGE_CONFIRMED' WHERE id=?",
      inspection
    );
    assertThat(
      hasIssue(importer.preview(manifest()), "INSPECTION_RESULT_CONFLICT")
    ).isTrue();
  }

  @Test
  void reviewRequiredStaysOpenAndDoesNotInventAnInspectionCompletion() {
    jdbc.update(
      "UPDATE legacy_rental_contract SET status='INSPECTING',completed_at=NULL WHERE id=?",
      contract
    );
    jdbc.update(
      "UPDATE rental_product SET status='INSPECTION' WHERE id=?",
      product
    );
    jdbc.update(
      "UPDATE legacy_return_inspection SET result='REVIEW_REQUIRED' WHERE id=?",
      inspection
    );
    jdbc.update(
      "UPDATE damage_record SET comparison_result='UNRESOLVED',matched_before_damage_id=NULL WHERE id=?",
      afterDamage
    );
    apply(manifest());
    Long i = mapped("legacy_return_inspection", inspection);
    assertThat(
      jdbc.queryForObject(
        "SELECT inspection_status_cd FROM return_inspection WHERE inspection_id=?",
        String.class,
        i
      )
    ).isEqualTo("IN_PROGRESS");
    assertThat(
      jdbc.queryForObject(
        "SELECT inspected_at FROM return_inspection WHERE inspection_id=?",
        java.sql.Timestamp.class,
        i
      )
    ).isNull();
    assertThat(
      jdbc.queryForObject(
        "SELECT assessment_result_cd FROM damage_assessment WHERE inspection_id=?",
        String.class,
        i
      )
    ).isEqualTo("HOLD");
    assertThatThrownBy(() ->
      workflow.completeInspection(i)
    ).hasMessageContaining("Unresolved");
  }

  @Test
  void explicitlyLinkedAccountKeepsItsCurrentPasswordRoleAndStatus() {
    Member current = new Member(
      "existing" + seq.incrementAndGet(),
      legacyEmail,
      "Current profile",
      encoder.encode("current-password123!")
    );
    current = members.saveAndFlush(current);
    String savedHash = current.getPasswordHash();
    var m = manifest();
    m = new LegacyManifest(
      List.of(
        new LegacyManifest.MemberMapping(legacyMember, null, current.getId())
      ),
      m.products(),
      m.inspections(),
      m.damageLocations()
    );
    apply(m);
    assertThat(mapped("member_account", legacyMember)).isEqualTo(
      current.getId()
    );
    assertThat(
      jdbc.queryForObject(
        "SELECT password_hash FROM member_credential WHERE member_id=?",
        String.class,
        current.getId()
      )
    ).isEqualTo(savedHash);
    assertThat(
      jdbc.queryForObject(
        "SELECT member_role_cd FROM member WHERE member_id=?",
        String.class,
        current.getId()
      )
    ).isEqualTo("CUSTOMER");
  }

  @Test
  void missingPasswordIsNotReplacedByAGeneratedCredential() {
    jdbc.update(
      "UPDATE member_account SET password_hash=NULL,status='SUSPENDED' WHERE id=?",
      legacyMember
    );
    var p = importer.preview(manifest());
    assertThat(p.get("ready")).isEqualTo(true);
    apply(manifest());
    Long id = mapped("member_account", legacyMember);
    assertThat(
      jdbc.queryForObject(
        "SELECT password_hash FROM member_credential WHERE member_id=?",
        String.class,
        id
      )
    ).isNull();
    assertThat(
      jdbc.queryForObject(
        "SELECT status FROM member_credential WHERE member_id=?",
        String.class,
        id
      )
    ).isEqualTo("SUSPENDED");
    assertThatThrownBy(() ->
      auth.login(new LoginRequest("legacy-" + legacyMember, "anything"))
    ).isInstanceOf(
      com.rentaltrack.global.error.AuthenticationRequiredException.class
    );
  }

  @Test
  void databaseFailureRollsBackRunAccountAndMappingTogether() {
    LegacyManifest m = manifest();
    String token = token(m),
      trigger = "reject_import_" + seq.incrementAndGet();
    jdbc.execute(
      "CREATE TRIGGER " +
        trigger +
        " BEFORE INSERT ON member_credential FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test rollback'"
    );
    try {
      assertThatThrownBy(() ->
        importer.apply(new LegacyManifest.ApplyInput(m, token))
      ).isInstanceOf(org.springframework.dao.DataAccessException.class);
      assertThat(
        jdbc.queryForObject(
          "SELECT COUNT(*) FROM legacy_import_run WHERE plan_token=?",
          Integer.class,
          token
        )
      ).isZero();
      assertThat(
        jdbc.queryForObject(
          "SELECT COUNT(*) FROM member WHERE login_id=?",
          Integer.class,
          "legacy-" + legacyMember
        )
      ).isZero();
      assertThat(
        jdbc.queryForObject(
          "SELECT COUNT(*) FROM legacy_entity_map WHERE source_table='member_account' AND source_id=?",
          Integer.class,
          legacyMember
        )
      ).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER " + trigger);
    }
  }

  @Test
  void sourceReadsAndImportRoutesRequireAdministratorPrivileges() {
    as(outsider);
    assertThatThrownBy(importer::source).isInstanceOf(
      com.rentaltrack.global.error.AccessDeniedException.class
    );
    var client = RestClient.builder()
      .baseUrl("http://localhost:" + port)
      .defaultHeader("Authorization", "Bearer " + jwt.issue(outsider.getId()))
      .build();
    assertThatThrownBy(() ->
      client
        .get()
        .uri("/api/v2/admin/legacy-import/source")
        .retrieve()
        .toBodilessEntity()
    ).isInstanceOf(HttpClientErrorException.Forbidden.class);
    assertThatThrownBy(() ->
      client
        .post()
        .uri("/api/v2/admin/legacy-import/preview")
        .body(manifest())
        .retrieve()
        .toBodilessEntity()
    ).isInstanceOf(HttpClientErrorException.Forbidden.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "REQUESTED,RESERVED,REQUESTED,N",
    "APPROVED,RESERVED,REQUESTED,N",
    "PICKED_UP,RESERVED,REQUESTED,N",
    "IN_USE,RENTED,RENTED,Y",
    "RETURNED,INSPECTION,RETURNED,Y",
    "INSPECTING,INSPECTION,RETURNED,Y",
    "REJECTED,AVAILABLE,CANCELED,N",
    "CANCELED,AVAILABLE,CANCELED,N",
  })
  void legacyStatesKeepTheirOriginalStageWhileMappingToTheNewWorkflow(
    String old,
    String itemState,
    String expected,
    String intakeConfirmed
  ) {
    jdbc.update("DELETE FROM legacy_return_inspection WHERE id=?", inspection);
    jdbc.update(
      "UPDATE legacy_rental_contract SET status=?,completed_at=NULL WHERE id=?",
      old,
      contract
    );
    if (
      Set.of(
        "REQUESTED",
        "APPROVED",
        "PICKED_UP",
        "REJECTED",
        "CANCELED"
      ).contains(old)
    ) jdbc.update(
      "UPDATE legacy_rental_contract SET rented_at=NULL,due_at=NULL,returned_at=NULL WHERE id=?",
      contract
    );
    if ("IN_USE".equals(old)) jdbc.update(
      "UPDATE legacy_rental_contract SET returned_at=NULL WHERE id=?",
      contract
    );
    jdbc.update(
      "UPDATE rental_product SET status=? WHERE id=?",
      itemState,
      product
    );
    var base = manifest();
    var m = new LegacyManifest(
      base.members(),
      base.products(),
      List.of(),
      base.damageLocations()
    );
    apply(m);
    assertThat(
      jdbc.queryForObject(
        "SELECT contract_status_cd FROM rental_contract WHERE contract_id=?",
        String.class,
        mapped("legacy_rental_contract", contract)
      )
    ).isEqualTo(expected);
    assertThat(
      jdbc.queryForObject(
        "SELECT confirmed_yn FROM condition_record WHERE condition_id=?",
        String.class,
        mapped("legacy_condition_record", before)
      )
    ).isEqualTo(intakeConfirmed);
    Map<?, ?> view = (Map<?, ?>) workflow
      .detail(mapped("legacy_rental_contract", contract))
      .get("rental");
    assertThat(
      ((Map<?, ?>) view.get("legacySource")).get("originalStatus")
    ).isEqualTo(old);
  }
}
