package com.rentaltrack.core;

import static org.assertj.core.api.Assertions.*;

import com.rentaltrack.auth.application.JwtTokenProvider;
import com.rentaltrack.auth.domain.AuthenticatedMember;
import com.rentaltrack.global.error.AccessDeniedException;
import com.rentaltrack.member.domain.*;
import com.rentaltrack.member.repository.MemberRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkflowIntegrationTest {

  @Autowired
  WorkflowService flow;

  @Autowired
  MemberRepository members;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtTokenProvider jwt;

  @Autowired
  JdbcTemplate jdbc;

  @LocalServerPort
  int port;

  static final AtomicLong sequence = new AtomicLong(
    (System.currentTimeMillis() % 8_000_000_000L) + 1_000_000_000L
  );
  Member admin, customer, other;
  Long rental, item;

  @BeforeEach
  void fixture() {
    admin = member(MemberRole.ADMIN);
    customer = member(MemberRole.CUSTOMER);
    other = member(MemberRole.CUSTOMER);
    as(admin);
    Long business = id(
      flow.addBusiness(
        new Requests.BusinessInput(
          String.valueOf(sequence.incrementAndGet()),
          "Test business",
          null
        )
      )
    );
    item = id(
      flow.addItem(
        new Requests.ItemInput(
          business,
          "ASSET-1",
          "Test camera",
          "CAMERA",
          null
        )
      )
    );
    as(customer);
    rental = id(flow.requestRental(new Requests.RentalInput(item)));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  Member member(MemberRole role) {
    String key = "u" + sequence.incrementAndGet();
    Member m = new Member(
      key,
      key + "@example.com",
      key,
      encoder.encode("password123!")
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

  @SuppressWarnings("unchecked")
  static List<Map<String, Object>> list(Map<String, Object> view, String key) {
    return (List<Map<String, Object>>) view.get(key);
  }

  MockMultipartFile image() {
    try {
      var bytes = new ByteArrayOutputStream();
      ImageIO.write(
        new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB),
        "png",
        bytes
      );
      return new MockMultipartFile(
        "file",
        "test.png",
        "image/png",
        bytes.toByteArray()
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  Long record(String stage) {
    return id(
      flow.addCondition(
        rental,
        new Requests.ConditionInput(stage, stage + " notes")
      )
    );
  }

  Long photo(Long condition) {
    return id(flow.addPhoto(condition, image(), "FRONT", LocalDateTime.now()));
  }

  Long damage(Long condition, String type) {
    return id(
      flow.addDamage(
        condition,
        new Requests.DamageInput("FRONT", type, "MINOR", "Observed mark")
      )
    );
  }

  Long intake() {
    Long before = record("INTAKE");
    photo(before);
    flow.confirm(before);
    return before;
  }

  void start() {
    LocalDateTime now = LocalDateTime.now().plusSeconds(1);
    flow.start(rental, new Requests.StartInput(now, now.plusHours(2)));
  }

  Long returned() {
    start();
    flow.returnRental(
      rental,
      new Requests.ReturnInput(LocalDateTime.now().plusHours(1))
    );
    Long after = record("RETURN");
    photo(after);
    return after;
  }

  Long inspection(Long before, Long after) {
    flow.confirm(after);
    as(admin);
    return id(
      flow.createInspection(
        rental,
        new Requests.InspectionInput(before, after, "Inspection")
      )
    );
  }

  RestClient client(Member m) {
    return RestClient.builder()
      .baseUrl("http://localhost:" + port)
      .defaultHeader("Authorization", "Bearer " + jwt.issue(m.getId()))
      .build();
  }

  @SuppressWarnings("unchecked")
  Map<String, Object> rentalView() {
    return (Map<String, Object>) flow.detail(rental).get("rental");
  }

  @Test
  void flywayAndJpaValidateTheNewSchema() {
    assertThat(
      jdbc.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE version='11' AND success=1",
        Integer.class
      )
    ).isEqualTo(1);
    assertThat(
      jdbc.queryForObject(
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('business','member','rental_item','rental_contract','condition_record','condition_photo','damage_observation','damage_photo','image_analysis_result','return_inspection','damage_assessment','common_code_group','common_code')",
        Integer.class
      )
    ).isEqualTo(13);
    assertThat(
      jdbc.queryForObject(
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('legacy_rental_contract','legacy_condition_record','legacy_return_inspection')",
        Integer.class
      )
    ).isEqualTo(3);
  }

  @Test
  void fullFlowSeparatesExistingAndNewDamageAndDerivesRepair() {
    Long before = record("INTAKE"),
      p = photo(before),
      old = damage(before, "SCRATCH");
    flow.addEvidence(old, new Requests.EvidenceInput(p, null));
    flow.confirm(before);
    Long after = returned(),
      existing = damage(after, "SCRATCH"),
      added = damage(after, "CRACK"),
      inspection = inspection(before, after);
    flow.beginInspection(inspection);
    flow.assess(
      inspection,
      new Requests.AssessmentInput(
        existing,
        old,
        "EXISTING",
        "Same intake scratch"
      )
    );
    flow.assess(
      inspection,
      new Requests.AssessmentInput(added, null, "NEW", "No intake counterpart")
    );
    assertThat(
      flow.completeInspection(inspection).get("overallResultCd")
    ).isEqualTo("NEW_DAMAGE");
    assertThat(rentalView().get("contractStatusCd")).isEqualTo("INSPECTED");
    assertThat(
      jdbc.queryForObject(
        "SELECT item_status_cd FROM rental_item WHERE item_id=?",
        String.class,
        item
      )
    ).isEqualTo("REPAIR");
    assertThatThrownBy(() ->
      flow.assess(
        inspection,
        new Requests.AssessmentInput(added, null, "NEW", "Changed")
      )
    ).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void noNewDamageReleasesTheItemForAnotherRental() {
    Long before = intake(),
      after = returned(),
      inspection = inspection(before, after);
    flow.beginInspection(inspection);
    assertThat(
      flow.completeInspection(inspection).get("overallResultCd")
    ).isEqualTo("NO_NEW_DAMAGE");
    as(other);
    assertThat(
      flow.requestRental(new Requests.RentalInput(item)).get("contractStatusCd")
    ).isEqualTo("REQUESTED");
  }

  @Test
  void revisionCopiesPhotosDamageAndEvidenceWithoutChangingConfirmedSource() {
    Long before = record("INTAKE"),
      p = photo(before),
      d = damage(before, "SCRATCH");
    flow.addEvidence(d, new Requests.EvidenceInput(p, null));
    flow.confirm(before);
    assertThatThrownBy(() ->
      flow.editCondition(before, new Requests.NoteInput("overwrite"))
    ).hasMessageContaining("immutable");
    assertThatThrownBy(() -> damage(before, "CRACK")).hasMessageContaining(
      "immutable"
    );
    Map<String, Object> next = flow.revise(
      before,
      new Requests.NoteInput("Revision 2")
    );
    Long revised = id(next);
    assertThat(next.get("recordRevisionNo")).isEqualTo(2);
    assertThat(next.get("confirmed")).isEqualTo(false);
    Map<String, Object> copyPhoto = list(next, "photos").getFirst(),
      copyDamage = list(next, "damages").getFirst();
    assertThat(id(copyPhoto)).isNotEqualTo(p);
    assertThat(
      list(copyDamage, "evidence").getFirst().get("photoId")
    ).isEqualTo(id(copyPhoto));
    flow.editDamage(
      id(copyDamage),
      new Requests.DamageInput(
        "FRONT",
        "CRACK",
        "SEVERE",
        "Corrected observation"
      )
    );
    assertThat(
      jdbc.queryForObject(
        "SELECT damage_type_cd FROM damage_observation WHERE damage_id=?",
        String.class,
        d
      )
    ).isEqualTo("SCRATCH");
    assertThatThrownBy(() ->
      flow.revise(before, new Requests.NoteInput("Duplicate"))
    ).hasMessageContaining("latest");
    assertThatThrownBy(this::start).hasMessageContaining("Latest intake");
    flow.confirm(revised);
    start();
    assertThatThrownBy(() ->
      flow.revise(revised, new Requests.NoteInput("Retroactive intake"))
    ).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void cancelMakesPendingItemRequestableAgain() {
    flow.cancel(rental);
    as(other);
    assertThat(
      id(flow.requestRental(new Requests.RentalInput(item)))
    ).isNotEqualTo(rental);
  }

  @Test
  void startRequiresConfirmedPhotoAndValidDates() {
    assertThatThrownBy(this::start).hasMessageContaining("intake");
    Long before = record("INTAKE");
    assertThatThrownBy(() -> flow.confirm(before)).hasMessageContaining(
      "photo"
    );
    photo(before);
    assertThatThrownBy(this::start).hasMessageContaining("confirmed");
    flow.confirm(before);
    assertThatThrownBy(() ->
      flow.start(
        rental,
        new Requests.StartInput(
          LocalDateTime.now(),
          LocalDateTime.now().minusHours(1)
        )
      )
    ).hasMessageContaining("period");
  }

  @Test
  void evidenceCannotUseAnotherConditionsPhoto() {
    Long a = record("INTAKE"),
      p = photo(a),
      d = damage(a, "SCRATCH");
    flow.confirm(a);
    Long b = id(flow.revise(a, new Requests.NoteInput("New"))),
      newDamage = damage(b, "CRACK");
    assertThatThrownBy(() ->
      flow.addEvidence(newDamage, new Requests.EvidenceInput(p, null))
    ).hasMessageContaining("same condition");
    assertThat(d).isNotNull();
  }

  @Test
  void inspectionRejectsUnconfirmedWrongStageAndOtherContractRecords() {
    Long before = intake(),
      after = returned();
    as(admin);
    assertThatThrownBy(() ->
      flow.createInspection(
        rental,
        new Requests.InspectionInput(before, after, null)
      )
    ).hasMessageContaining("confirmed");
    as(customer);
    flow.confirm(after);
    as(admin);
    assertThatThrownBy(() ->
      flow.createInspection(
        rental,
        new Requests.InspectionInput(after, before, null)
      )
    ).hasMessageContaining("stages");
    // Another valid confirmed record still cannot be used for this contract.
    Long original = rental;
    as(customer);
    Long business;
    as(admin);
    business = id(
      flow.addBusiness(
        new Requests.BusinessInput(
          String.valueOf(sequence.incrementAndGet()),
          "Other business",
          null
        )
      )
    );
    Long otherItem = id(
      flow.addItem(
        new Requests.ItemInput(business, "OTHER", "Other item", "CAMERA", null)
      )
    );
    as(customer);
    rental = id(flow.requestRental(new Requests.RentalInput(otherItem)));
    Long foreign = intake();
    rental = original;
    as(admin);
    assertThatThrownBy(() ->
      flow.createInspection(
        rental,
        new Requests.InspectionInput(foreign, after, null)
      )
    ).hasMessageContaining("this contract");
  }

  @Test
  void inspectionPinsSelectedRecordsAndBlocksFurtherRevisions() {
    Long before = intake(),
      after = returned(),
      inspection = inspection(before, after);
    assertThatThrownBy(() ->
      flow.revise(after, new Requests.NoteInput("Changed"))
    ).hasMessageContaining("fixed");
    assertThat(flow.detail(rental).get("inspection")).isNotNull();
    assertThat(inspection).isPositive();
  }

  @Test
  void missingAndHeldAssessmentsBlockCompletion() {
    Long before = intake(),
      after = returned(),
      d = damage(after, "DENT"),
      inspection = inspection(before, after);
    flow.beginInspection(inspection);
    assertThatThrownBy(() ->
      flow.completeInspection(inspection)
    ).hasMessageContaining("Every return damage");
    flow.assess(
      inspection,
      new Requests.AssessmentInput(d, null, "HOLD", "Need more information")
    );
    assertThatThrownBy(() ->
      flow.completeInspection(inspection)
    ).hasMessageContaining("Unresolved");
    flow.assess(
      inspection,
      new Requests.AssessmentInput(d, null, "NEW", "Confirmed after review")
    );
    assertThat(
      flow.completeInspection(inspection).get("overallResultCd")
    ).isEqualTo("NEW_DAMAGE");
  }

  @Test
  void existingAssessmentRequiresAnIntakeDamageFromPinnedRecord() {
    Long before = record("INTAKE"),
      d = damage(before, "SCRATCH");
    photo(before);
    flow.confirm(before);
    Long after = returned(),
      afterDamage = damage(after, "DENT"),
      inspection = inspection(before, after);
    flow.beginInspection(inspection);
    assertThatThrownBy(() ->
      flow.assess(
        inspection,
        new Requests.AssessmentInput(
          afterDamage,
          null,
          "EXISTING",
          "Missing reference"
        )
      )
    ).hasMessageContaining("requires");
    assertThatThrownBy(() ->
      flow.assess(
        inspection,
        new Requests.AssessmentInput(
          afterDamage,
          afterDamage,
          "EXISTING",
          "Wrong stage"
        )
      )
    ).hasMessageContaining("fixed");
    assertThatThrownBy(() ->
      flow.assess(
        inspection,
        new Requests.AssessmentInput(afterDamage, d, "NEW", "Wrong reference")
      )
    ).hasMessageContaining("Only EXISTING");
  }

  @Test
  void photoHashIsStoredAndNonImageBytesRejected() {
    Long before = record("INTAKE");
    Map<String, Object> photo = flow.addPhoto(before, image(), "FRONT", null);
    assertThat((String) photo.get("fileHash")).matches("[0-9a-f]{64}");
    assertThatThrownBy(() ->
      flow.addPhoto(
        before,
        new MockMultipartFile(
          "file",
          "fake.png",
          "image/png",
          "not an image".getBytes()
        ),
        "FRONT",
        null
      )
    ).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void analysisIsACandidateWithUniqueRunSequenceAndValidBounds() {
    Long before = record("INTAKE"),
      p = photo(before);
    as(admin);
    String run = UUID.randomUUID().toString();
    var box = new Requests.Box(
      new BigDecimal("0.1"),
      new BigDecimal("0.2"),
      new BigDecimal("0.3"),
      new BigDecimal("0.4")
    );
    var input = new Requests.AnalysisInput(
      run,
      1,
      "supplied-test-model",
      "test-v1",
      "SCRATCH",
      new BigDecimal("0.9"),
      box,
      LocalDateTime.now()
    );
    flow.addAnalysis(p, input);
    assertThatThrownBy(() -> flow.addAnalysis(p, input)).isInstanceOf(
      org.springframework.dao.DataIntegrityViolationException.class
    );
    assertThatThrownBy(() ->
      new Requests.Box(
        BigDecimal.ONE,
        BigDecimal.ZERO,
        BigDecimal.ONE,
        BigDecimal.ONE
      ).check()
    ).isInstanceOf(IllegalArgumentException.class);
    assertThat(rentalView().get("contractStatusCd")).isEqualTo("REQUESTED");
  }

  @Test
  void ownerIsolationAndStaffAuthorizationAreEnforcedOverHttp() {
    Long before = record("INTAKE"),
      p = photo(before);
    assertThatThrownBy(() ->
      client(other)
        .get()
        .uri("/api/v2/rentals/" + rental)
        .retrieve()
        .toBodilessEntity()
    ).isInstanceOf(HttpClientErrorException.Forbidden.class);
    assertThatThrownBy(() ->
      client(other)
        .get()
        .uri("/api/v2/photos/" + p + "/content")
        .retrieve()
        .toBodilessEntity()
    ).isInstanceOf(HttpClientErrorException.Forbidden.class);
    assertThatThrownBy(() ->
      client(customer)
        .post()
        .uri("/api/v2/businesses")
        .body(new Requests.BusinessInput("1234567890", "Denied", null))
        .retrieve()
        .toBodilessEntity()
    ).isInstanceOf(HttpClientErrorException.Forbidden.class);
    assertThat(
      client(customer)
        .get()
        .uri("/api/v2/photos/" + p + "/content")
        .retrieve()
        .toEntity(byte[].class)
        .getBody()
    ).isNotEmpty();
  }

  @Test
  void apiKeepsUnauthorizedBadRequestAndConflictStatusesDistinct() {
    RestClient anonymous = RestClient.create("http://localhost:" + port);
    assertThatThrownBy(() ->
      anonymous.get().uri("/api/v2/rentals").retrieve().toBodilessEntity()
    ).isInstanceOf(HttpClientErrorException.Unauthorized.class);
    assertThatThrownBy(() ->
      client(customer)
        .post()
        .uri("/api/v2/rentals/" + rental + "/conditions")
        .body(new Requests.ConditionInput("BAD_CODE", null))
        .retrieve()
        .toBodilessEntity()
    ).isInstanceOf(HttpClientErrorException.BadRequest.class);
    assertThatThrownBy(() ->
      client(customer)
        .post()
        .uri("/api/v2/rentals")
        .body(new Requests.RentalInput(item))
        .retrieve()
        .toBodilessEntity()
    ).isInstanceOf(HttpClientErrorException.Conflict.class);
  }

  @Test
  void registrationHashesPasswordAndCannotChooseRole() {
    String key = "new" + sequence.incrementAndGet();
    RestClient anonymous = RestClient.create("http://localhost:" + port);
    Map<?, ?> registered = anonymous
      .post()
      .uri("/api/v1/auth/register")
      .body(
        Map.of(
          "loginId",
          key,
          "email",
          key + "@example.com",
          "name",
          "New member",
          "password",
          "password123!",
          "role",
          "ADMIN"
        )
      )
      .retrieve()
      .body(Map.class);
    assertThat(registered.get("role")).isEqualTo("CUSTOMER");
    Member saved = members.findByLoginIdIgnoreCase(key).orElseThrow();
    assertThat(saved.getPasswordHash()).isNotEqualTo("password123!");
    assertThat(
      encoder.matches("password123!", saved.getPasswordHash())
    ).isTrue();
    assertThat(
      anonymous
        .post()
        .uri("/api/v1/auth/login")
        .body(
          Map.of(
            "loginId",
            key.toUpperCase(Locale.ROOT),
            "password",
            "password123!"
          )
        )
        .retrieve()
        .body(Map.class)
        .get("memberId")
    ).isEqualTo(((Number) registered.get("memberId")).intValue());
    assertThatThrownBy(() ->
      anonymous
        .post()
        .uri("/api/v1/auth/login")
        .body(Map.of("loginId", key, "password", "wrong-password"))
        .retrieve()
        .toBodilessEntity()
    ).isInstanceOf(HttpClientErrorException.Unauthorized.class);
  }

  @Test
  void databaseRejectsPartialBoundingBox() {
    Long before = record("INTAKE"),
      p = photo(before),
      d = damage(before, "SCRATCH");
    Throwable failure = catchThrowable(() ->
      jdbc.update(
        "INSERT INTO damage_photo(damage_id,photo_id,bbox_x_ratio,bbox_y_ratio) VALUES (?,?,0.1,0.1)",
        d,
        p
      )
    );
    assertThat(failure)
      .isInstanceOf(org.springframework.dao.DataAccessException.class)
      .hasRootCauseInstanceOf(java.sql.SQLException.class);
    Throwable root = failure;
    while (root.getCause() != null) root = root.getCause();
    assertThat(((java.sql.SQLException) root).getErrorCode()).isEqualTo(3819);
  }

  @Test
  void onlyOneConcurrentRequestCanReserveAnAvailableItem() throws Exception {
    flow.cancel(rental);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var ready = new CountDownLatch(2);
      var go = new CountDownLatch(1);
      List<Future<Boolean>> futures = new ArrayList<>();
      for (Member m : List.of(customer, other))
        futures.add(
          pool.submit(() -> {
            as(m);
            ready.countDown();
            go.await();
            try {
              flow.requestRental(new Requests.RentalInput(item));
              return true;
            } catch (IllegalStateException e) {
              return false;
            } finally {
              SecurityContextHolder.clearContext();
            }
          })
        );
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      go.countDown();
      int successes = 0;
      for (var f : futures) if (f.get(20, TimeUnit.SECONDS)) successes++;
      assertThat(successes).isEqualTo(1);
    }
  }

  @Test
  void onlyOneConcurrentRevisionCanBranchFromTheSameConfirmedRecord()
    throws Exception {
    Long before = intake();
    try (var pool = Executors.newFixedThreadPool(2)) {
      var ready = new CountDownLatch(2);
      var go = new CountDownLatch(1);
      List<Future<Boolean>> futures = new ArrayList<>();
      for (int i = 0; i < 2; i++) futures.add(
        pool.submit(() -> {
          as(customer);
          ready.countDown();
          go.await();
          try {
            flow.revise(before, new Requests.NoteInput("Next revision"));
            return true;
          } catch (IllegalStateException e) {
            return false;
          } finally {
            SecurityContextHolder.clearContext();
          }
        })
      );
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      go.countDown();
      int successes = 0;
      for (var future : futures)
        if (future.get(20, TimeUnit.SECONDS)) successes++;
      assertThat(successes).isEqualTo(1);
      assertThat(
        jdbc.queryForObject(
          "SELECT COUNT(*) FROM condition_record WHERE contract_id=? AND record_stage_cd='INTAKE'",
          Integer.class,
          rental
        )
      ).isEqualTo(2);
    }
  }

  @Test
  void jwtFromTheLegacyMemberNamespaceIsRejected() {
    String secret = "UmVudGFsVHJhY2stRGV2LU9ubHktSldULVNlY3JldC0zMi1CeXRlcyE=";
    var provider = new JwtTokenProvider(secret, 1800);
    String old = io.jsonwebtoken.Jwts.builder()
      .subject(customer.getId().toString())
      .signWith(
        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
          io.jsonwebtoken.io.Decoders.BASE64.decode(secret)
        )
      )
      .compact();
    assertThatThrownBy(() -> provider.parseMemberId(old)).isInstanceOf(
      IllegalArgumentException.class
    );
    assertThat(
      provider.parseMemberId(provider.issue(customer.getId()))
    ).isEqualTo(customer.getId());
  }
}
