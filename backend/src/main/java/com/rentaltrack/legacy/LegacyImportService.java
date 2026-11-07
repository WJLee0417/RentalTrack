package com.rentaltrack.legacy;

import com.rentaltrack.auth.domain.AuthenticatedMember;
import com.rentaltrack.global.error.AccessDeniedException;
import com.rentaltrack.member.domain.MemberRole;
import com.rentaltrack.storage.application.ImageStorage;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;

@Service
public class LegacyImportService {

  private final JdbcTemplate jdbc;
  private final ImageStorage storage;
  private final ObjectMapper json;
  private static final String MEMBER = "member_account",
    PRODUCT = "rental_product",
    CONTRACT = "legacy_rental_contract",
    CONDITION = "legacy_condition_record",
    PHOTO = "condition_image",
    DAMAGE = "damage_record",
    INSPECTION = "legacy_return_inspection";
  private static final List<String> SOURCES = List.of(
    MEMBER,
    PRODUCT,
    CONTRACT,
    CONDITION,
    PHOTO,
    DAMAGE,
    INSPECTION
  );
  private static final Map<String, String> TARGETS = Map.of(
    MEMBER,
    "member",
    PRODUCT,
    "rental_item",
    CONTRACT,
    "rental_contract",
    CONDITION,
    "condition_record",
    PHOTO,
    "condition_photo",
    DAMAGE,
    "damage_observation",
    INSPECTION,
    "return_inspection"
  );
  private static final Map<String, String> CONTRACT_STATES = Map.ofEntries(
    Map.entry("REQUESTED", "REQUESTED"),
    Map.entry("APPROVED", "REQUESTED"),
    Map.entry("PICKED_UP", "REQUESTED"),
    Map.entry("IN_USE", "RENTED"),
    Map.entry("RETURNED", "RETURNED"),
    Map.entry("INSPECTING", "RETURNED"),
    Map.entry("COMPLETED", "INSPECTED"),
    Map.entry("REJECTED", "CANCELED"),
    Map.entry("CANCELED", "CANCELED")
  );
  private static final Map<String, String> ITEM_STATES = Map.of(
    "AVAILABLE",
    "AVAILABLE",
    "RESERVED",
    "AVAILABLE",
    "RENTED",
    "RENTED",
    "INSPECTION",
    "INSPECTION",
    "MAINTENANCE",
    "REPAIR",
    "RETIRED",
    "INACTIVE"
  );
  private static final Set<String> CATEGORIES = Set.of(
      "CAMERA",
      "COMPUTER",
      "HOME_APPLIANCE",
      "SPORTS"
    ),
    LOCATIONS = Set.of(
      "FRONT",
      "BACK",
      "LEFT",
      "RIGHT",
      "TOP",
      "BOTTOM",
      "EDGE",
      "SCREEN",
      "LENS",
      "PORT",
      "OTHER"
    ),
    ANGLES = Set.of(
      "FRONT",
      "BACK",
      "LEFT",
      "RIGHT",
      "TOP",
      "BOTTOM",
      "DETAIL",
      "OTHER"
    ),
    POST_START = Set.of("IN_USE", "RETURNED", "INSPECTING", "COMPLETED");

  public LegacyImportService(
    JdbcTemplate jdbc,
    ImageStorage storage,
    ObjectMapper json
  ) {
    this.jdbc = jdbc;
    this.storage = storage;
    this.json = json;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> source() {
    admin();
    return map(
      "members",
      jdbc.queryForList(
        "SELECT a.id,a.email,a.name,a.role,a.status,m.target_id AS importedMemberId FROM member_account a LEFT JOIN legacy_entity_map m ON m.source_table='member_account' AND m.source_id=a.id ORDER BY a.id"
      ),
      "products",
      jdbc.queryForList(
        "SELECT p.id,p.asset_code AS assetCode,p.display_name AS itemName,p.status,c.code AS categoryCode,c.name AS categoryName,m.target_id AS importedItemId FROM rental_product p JOIN product_category c ON c.id=p.category_id LEFT JOIN legacy_entity_map m ON m.source_table='rental_product' AND m.source_id=p.id ORDER BY p.id"
      ),
      "inspections",
      jdbc.queryForList(
        "SELECT i.id,i.rental_contract_id AS contractId,i.result,m.target_id AS importedInspectionId FROM legacy_return_inspection i LEFT JOIN legacy_entity_map m ON m.source_table='legacy_return_inspection' AND m.source_id=i.id ORDER BY i.id"
      ),
      "damageLocations",
      jdbc.queryForList(
        "SELECT DISTINCT damage_location AS source FROM damage_record ORDER BY damage_location"
      ),
      "businesses",
      jdbc.queryForList(
        "SELECT business_id AS id,business_no AS businessNo,business_name AS businessName FROM business ORDER BY business_id"
      ),
      "inspectors",
      jdbc.queryForList(
        "SELECT member_id AS id,member_name AS name,member_role_cd AS role FROM `member` WHERE member_role_cd IN ('ADMIN','INSPECTOR') ORDER BY member_id"
      ),
      "contractStates",
      new TreeMap<>(CONTRACT_STATES),
      "itemStates",
      new TreeMap<>(ITEM_STATES)
    );
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> preview(LegacyManifest manifest) {
    admin();
    return prepare(manifest, false).report();
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> apply(LegacyManifest.ApplyInput input) {
    AuthenticatedMember actor = admin();
    jdbc.queryForObject(
      "SELECT id FROM legacy_import_mutex WHERE id=1 FOR UPDATE",
      Integer.class
    );
    var completed = jdbc.queryForList(
      "SELECT id,result_json,manifest_json FROM legacy_import_run WHERE plan_token=?",
      input.planToken()
    );
    if (!completed.isEmpty()) {
      if (
        !completed
          .getFirst()
          .get("manifest_json")
          .equals(json.writeValueAsString(input.manifest()))
      ) throw new IllegalStateException(
        "Preview does not match the supplied mapping"
      );
      return map(
        "status",
        "ALREADY_IMPORTED",
        "runId",
        num(completed.getFirst(), "id"),
        "counts",
        json.readValue(
          (String) completed.getFirst().get("result_json"),
          Map.class
        )
      );
    }
    Batch b = prepare(input.manifest(), true);
    if (!b.issues.isEmpty()) throw new IllegalStateException(
      "Import blocked: " +
        b.issues
          .stream()
          .map(
            i -> i.get("code") + " " + i.get("entity") + "#" + i.get("legacyId")
          )
          .collect(Collectors.joining(", "))
    );
    if (!b.token.equals(input.planToken())) throw new IllegalStateException(
      "Source, files or destination changed; preview the mapping again"
    );
    Long run = insert(
      "legacy_import_run",
      "id",
      map(
        "plan_token",
        b.token,
        "source_digest",
        b.sourceDigest,
        "manifest_json",
        json.writeValueAsString(input.manifest()),
        "result_json",
        "{}",
        "imported_by",
        actor.memberId()
      )
    );
    Map<String, Integer> counts = new TreeMap<>();
    for (var row : b.rows.get(MEMBER))
      if (!b.isMapped(MEMBER, id(row))) {
        var choice = b.members.get(id(row));
        Long target;
        if (choice != null && choice.existingMemberId() != null) target =
          choice.existingMemberId();
        else {
          target = insert(
            "member",
            "member_id",
            map(
              "login_id",
              login(choice, id(row)),
              "member_name",
              row.get("name"),
              "email",
              row.get("email"),
              "member_role_cd",
              role(row),
              "created_at",
              row.get("created_at")
            )
          );
          jdbc.update(
            "INSERT INTO member_credential(member_id,password_hash,status) VALUES (?,?,?)",
            target,
            row.get("password_hash"),
            row.get("status")
          );
        }
        b.remember(MEMBER, row, target, run, counts);
      }
    for (var row : b.rows.get(PRODUCT))
      if (!b.isMapped(PRODUCT, id(row))) {
        var choice = b.products.get(id(row));
        Long target = insert(
          "rental_item",
          "item_id",
          map(
            "business_id",
            choice.businessId(),
            "asset_code",
            asset(choice, row),
            "item_name",
            row.get("display_name"),
            "category_cd",
            choice.categoryCd(),
            "item_status_cd",
            ITEM_STATES.get(str(row, "status")),
            "created_at",
            row.get("created_at")
          )
        );
        b.remember(PRODUCT, row, target, run, counts);
      }
    for (var row : b.rows.get(CONTRACT))
      if (!b.isMapped(CONTRACT, id(row))) {
        Long target = insert(
          "rental_contract",
          "contract_id",
          map(
            "contract_no",
            row.get("contract_code"),
            "member_id",
            b.target(MEMBER, num(row, "member_id")),
            "item_id",
            b.target(PRODUCT, num(row, "rental_product_id")),
            "requested_at",
            row.get("requested_at"),
            "rental_start_at",
            row.get("rented_at"),
            "rental_end_at",
            row.get("due_at"),
            "actual_return_at",
            row.get("returned_at"),
            "contract_status_cd",
            CONTRACT_STATES.get(str(row, "status")),
            "created_at",
            row.get("created_at")
          )
        );
        b.remember(CONTRACT, row, target, run, counts);
      }
    for (var row : b.rows.get(CONDITION))
      if (!b.isMapped(CONDITION, id(row))) {
        Long target = insert(
          "condition_record",
          "condition_id",
          map(
            "contract_id",
            b.target(CONTRACT, num(row, "rental_contract_id")),
            "record_stage_cd",
            stage(row),
            "record_revision_no",
            1,
            "recorded_at",
            row.get("recorded_at"),
            "record_note",
            row.get("summary"),
            "confirmed_yn",
            confirmed(b, row) ? "Y" : "N",
            "created_at",
            row.get("created_at")
          )
        );
        b.remember(CONDITION, row, target, run, counts);
      }
    for (var row : b.rows.get(PHOTO))
      if (!b.isMapped(PHOTO, id(row))) {
        PhotoFile file = b.files.get(id(row));
        Long target = insert(
          "condition_photo",
          "photo_id",
          map(
            "condition_id",
            b.target(CONDITION, num(row, "condition_record_id")),
            "photo_seq",
            row.get("capture_order"),
            "storage_key",
            row.get("storage_key"),
            "original_file_name",
            file.filename,
            "mime_type",
            file.mime,
            "file_size",
            file.bytes,
            "file_hash",
            file.hash,
            "capture_angle_cd",
            angle(row),
            "captured_at",
            row.get("captured_at"),
            "created_at",
            row.get("created_at")
          )
        );
        b.remember(PHOTO, row, target, run, counts);
      }
    for (var row : b.rows.get(DAMAGE))
      if (!b.isMapped(DAMAGE, id(row))) {
        Long target = insert(
          "damage_observation",
          "damage_id",
          map(
            "condition_id",
            b.target(CONDITION, num(row, "condition_record_id")),
            "damage_location_cd",
            location(b, row),
            "damage_type_cd",
            damageType(row),
            "severity_cd",
            row.get("severity"),
            "damage_description",
            row.get("description"),
            "created_at",
            row.get("created_at")
          )
        );
        b.remember(DAMAGE, row, target, run, counts);
      }
    for (var row : b.rows.get(INSPECTION))
      if (!b.isMapped(INSPECTION, id(row))) {
        var contract = b.find(CONTRACT, num(row, "rental_contract_id"));
        var before = b.condition(id(contract), "BEFORE_RENTAL");
        var after = b.condition(id(contract), "AFTER_RENTAL");
        var choice = b.inspectors.get(id(row));
        Long inspector =
          choice.inspectorMemberId() != null
            ? choice.inspectorMemberId()
            : b.target(MEMBER, choice.legacyInspectorMemberId());
        boolean complete = "COMPLETED".equals(str(contract, "status"));
        String result = "REVIEW_REQUIRED".equals(str(row, "result"))
          ? "REVIEW_REQUIRED"
          : "NEW_DAMAGE_CONFIRMED".equals(str(row, "result"))
            ? "NEW_DAMAGE"
            : "NO_NEW_DAMAGE";
        Long target = insert(
          "return_inspection",
          "inspection_id",
          map(
            "contract_id",
            b.target(CONTRACT, id(contract)),
            "intake_condition_id",
            b.target(CONDITION, id(before)),
            "return_condition_id",
            b.target(CONDITION, id(after)),
            "inspector_member_id",
            inspector,
            "inspection_status_cd",
            complete ? "COMPLETED" : "IN_PROGRESS",
            "overall_result_cd",
            result,
            "inspected_at",
            complete ? row.get("inspected_at") : null,
            "inspection_note",
            row.get("note"),
            "created_at",
            row.get("created_at")
          )
        );
        for (var damage : b.children(
          DAMAGE,
          "condition_record_id",
          id(after)
        )) {
          String comparison = str(damage, "comparison_result"),
            assessment = "UNRESOLVED".equals(comparison) ? "HOLD" : comparison;
          insert(
            "damage_assessment",
            "assessment_id",
            map(
              "inspection_id",
              target,
              "return_damage_id",
              b.target(DAMAGE, id(damage)),
              "intake_damage_id",
              "EXISTING".equals(comparison)
                ? b.target(DAMAGE, num(damage, "matched_before_damage_id"))
                : null,
              "assessment_result_cd",
              assessment,
              "assessment_reason",
              null,
              "created_at",
              LocalDateTime.now()
            )
          );
          counts.merge("damage_assessment", 1, Integer::sum);
        }
        b.remember(INSPECTION, row, target, run, counts);
      }
    jdbc.update(
      "UPDATE legacy_import_run SET result_json=? WHERE id=?",
      json.writeValueAsString(counts),
      run
    );
    return map("status", "IMPORTED", "runId", run, "counts", counts);
  }

  private Batch prepare(LegacyManifest manifest, boolean lock) {
    Batch b = new Batch(manifest);
    if (b.products.isEmpty() && b.members.isEmpty()) b.issue(
      "NO_SELECTION",
      "manifest",
      null,
      "Select legacy members or products to import"
    );
    b.rows.put(PRODUCT, select(PRODUCT, "id", b.products.keySet(), lock));
    b.rows.put(
      CONTRACT,
      select(CONTRACT, "rental_product_id", ids(b.rows.get(PRODUCT)), lock)
    );
    b.rows.put(
      CONDITION,
      select(CONDITION, "rental_contract_id", ids(b.rows.get(CONTRACT)), lock)
    );
    b.rows.put(
      PHOTO,
      select(PHOTO, "condition_record_id", ids(b.rows.get(CONDITION)), lock)
    );
    b.rows.put(
      DAMAGE,
      select(DAMAGE, "condition_record_id", ids(b.rows.get(CONDITION)), lock)
    );
    b.rows.put(
      INSPECTION,
      select(INSPECTION, "rental_contract_id", ids(b.rows.get(CONTRACT)), lock)
    );
    Set<Long> memberIds = new TreeSet<>(b.members.keySet());
    b.rows.get(CONTRACT).forEach(r -> memberIds.add(num(r, "member_id")));
    b.inspectors
      .values()
      .stream()
      .map(LegacyManifest.InspectorMapping::legacyInspectorMemberId)
      .filter(Objects::nonNull)
      .forEach(memberIds::add);
    b.rows.put(MEMBER, select(MEMBER, "id", memberIds, lock));
    b.targets.put(
      "members",
      jdbc.queryForList(
        "SELECT m.*,c.status FROM `member` m JOIN member_credential c ON c.member_id=m.member_id ORDER BY m.member_id" +
          (lock ? " FOR SHARE" : "")
      )
    );
    b.targets.put(
      "items",
      jdbc.queryForList(
        "SELECT * FROM rental_item ORDER BY item_id" +
          (lock ? " FOR SHARE" : "")
      )
    );
    b.targets.put(
      "businesses",
      jdbc.queryForList(
        "SELECT * FROM business ORDER BY business_id" +
          (lock ? " FOR SHARE" : "")
      )
    );
    for (var row : jdbc.queryForList(
      "SELECT * FROM legacy_entity_map ORDER BY source_table,source_id" +
        (lock ? " FOR SHARE" : "")
    ))
      b.mapped.put(key(str(row, "source_table"), num(row, "source_id")), row);
    for (var entry : b.rows.entrySet())
      for (var row : entry.getValue()) {
        var mapped = b.mapping(entry.getKey(), id(row));
        if (mapped != null) {
          if (!digest(row).equals(mapped.get("source_digest"))) b.issue(
            "SOURCE_CHANGED",
            entry.getKey(),
            id(row),
            "Legacy row changed after it was imported; reconcile it explicitly"
          );
          String targetTable = TARGETS.get(entry.getKey());
          String targetKey = switch (targetTable) {
            case "member" -> "member_id";
            case "rental_item" -> "item_id";
            case "rental_contract" -> "contract_id";
            case "condition_record" -> "condition_id";
            case "condition_photo" -> "photo_id";
            case "damage_observation" -> "damage_id";
            default -> "inspection_id";
          };
          if (
            jdbc.queryForObject(
              "SELECT COUNT(*) FROM `" +
                targetTable +
                "` WHERE " +
                targetKey +
                "=?",
              Integer.class,
              num(mapped, "target_id")
            ) != 1
          ) b.issue(
            "TARGET_MISSING",
            entry.getKey(),
            id(row),
            "Previously imported destination no longer exists"
          );
        }
      }
    for (Long id : b.products.keySet())
      if (b.find(PRODUCT, id) == null) b.issue(
        "NOT_FOUND",
        PRODUCT,
        id,
        "Legacy product does not exist"
      );
    for (Long id : memberIds)
      if (b.find(MEMBER, id) == null) b.issue(
        "NOT_FOUND",
        MEMBER,
        id,
        "Legacy member does not exist"
      );
    for (Long id : b.inspectors.keySet())
      if (b.find(INSPECTION, id) == null) b.issue(
        "NOT_SELECTED",
        INSPECTION,
        id,
        "Inspector mapping is outside the selected product history"
      );
    validateMembers(b);
    validateProducts(b);
    validateHistory(b);
    validateInspections(b);
    b.sourceDigest = digest(b.rows);
    Map<Long, String> hashes = new TreeMap<>();
    b.files.forEach((id, file) -> hashes.put(id, file.hash));
    b.token = digest(
      map(
        "manifest",
        json.writeValueAsString(manifest),
        "source",
        b.sourceDigest,
        "files",
        hashes,
        "destination",
        b.targets,
        "mappings",
        b.mapped
      )
    );
    return b;
  }

  private void validateMembers(Batch b) {
    Set<String> chosenLogins = new HashSet<>();
    Set<Long> existingTargets = new HashSet<>();
    for (var row : b.rows.get(MEMBER)) {
      Long id = id(row);
      var choice = b.members.get(id);
      String login = login(choice, id),
        email = str(row, "email");
      if (
        !login.matches("[a-zA-Z0-9._-]{3,50}") ||
        !chosenLogins.add(login.toLowerCase(Locale.ROOT))
      ) b.issue(
        "LOGIN_CONFLICT",
        MEMBER,
        id,
        "Login IDs must be valid and unique within the batch"
      );
      if (
        email == null || email.length() > 254 || !email.contains("@")
      ) b.issue(
        "EMAIL_INVALID",
        MEMBER,
        id,
        "Legacy email requires an explicit correction before import"
      );
      if (role(row) == null) b.issue(
        "ROLE_INVALID",
        MEMBER,
        id,
        "Unknown legacy role"
      );
      if (b.isMapped(MEMBER, id)) {
        Long target = b.target(MEMBER, id);
        if (
          choice != null &&
          choice.existingMemberId() != null &&
          !target.equals(choice.existingMemberId())
        ) b.issue(
          "MAPPING_CHANGED",
          MEMBER,
          id,
          "This legacy member already has another destination"
        );
        continue;
      }
      if (choice != null && choice.existingMemberId() != null) {
        var target = b.targetRow(
          "members",
          "member_id",
          choice.existingMemberId()
        );
        if (
          target == null ||
          email == null ||
          !email.equalsIgnoreCase(str(target, "email"))
        ) b.issue(
          "MEMBER_LINK_INVALID",
          MEMBER,
          id,
          "Explicit existing account link must have the same email"
        );
        if (
          !existingTargets.add(choice.existingMemberId()) ||
          b.mapped
            .values()
            .stream()
            .anyMatch(
              m ->
                "member".equals(m.get("target_table")) &&
                choice.existingMemberId().equals(num(m, "target_id"))
            )
        ) b.issue(
          "MEMBER_LINK_CONFLICT",
          MEMBER,
          id,
          "Destination account already belongs to another legacy member"
        );
      } else {
        if (
          b.targets
            .get("members")
            .stream()
            .anyMatch(
              m ->
                login.equalsIgnoreCase(str(m, "login_id")) ||
                Objects.toString(email, "").equalsIgnoreCase(str(m, "email"))
            )
        ) b.issue(
          "MEMBER_CONFLICT",
          MEMBER,
          id,
          "Choose another login ID or explicitly link the matching existing account"
        );
        String hash = str(row, "password_hash");
        if (hash == null) b.warn(
          "PASSWORD_REQUIRED",
          MEMBER,
          id,
          "No legacy password hash; password enrollment is required before login"
        );
        else if (!hash.matches("\\$2[aby]\\$[0-9]{2}\\$.{53}")) b.issue(
          "PASSWORD_FORMAT",
          MEMBER,
          id,
          "Unsupported legacy password hash; do not substitute a generated password"
        );
      }
    }
  }

  private void validateProducts(Batch b) {
    Set<String> assets = new HashSet<>();
    for (var row : b.rows.get(PRODUCT)) {
      Long id = id(row);
      var choice = b.products.get(id);
      String asset = asset(choice, row);
      if (
        choice.businessId() == null ||
        b.targetRow("businesses", "business_id", choice.businessId()) == null
      ) b.issue(
        "BUSINESS_REQUIRED",
        PRODUCT,
        id,
        "Select the actual owner business; locations are not businesses"
      );
      if (
        !CATEGORIES.contains(Objects.toString(choice.categoryCd(), ""))
      ) b.issue(
        "CATEGORY_REQUIRED",
        PRODUCT,
        id,
        "Choose the target category explicitly"
      );
      if (asset == null || asset.isBlank() || asset.length() > 40) b.issue(
        "ASSET_CODE_REQUIRED",
        PRODUCT,
        id,
        "Asset code must fit 40 characters; provide an explicit replacement if necessary"
      );
      if (
        !assets.add(
          choice.businessId() +
            ":" +
            Objects.toString(asset, "").toLowerCase(Locale.ROOT)
        )
      ) b.issue(
        "ASSET_CONFLICT",
        PRODUCT,
        id,
        "Two legacy products have the same target asset identity"
      );
      if (ITEM_STATES.get(str(row, "status")) == null) b.issue(
        "ITEM_STATE_INVALID",
        PRODUCT,
        id,
        "Unknown legacy item state"
      );
      if (b.isMapped(PRODUCT, id)) {
        var target = b.targetRow("items", "item_id", b.target(PRODUCT, id));
        if (
          target == null ||
          !Objects.equals(choice.businessId(), num(target, "business_id")) ||
          !Objects.equals(choice.categoryCd(), str(target, "category_cd")) ||
          !Objects.equals(asset, str(target, "asset_code"))
        ) b.issue(
          "MAPPING_CHANGED",
          PRODUCT,
          id,
          "Existing imported product does not match the supplied owner/category/asset mapping"
        );
        for (var contract : b.children(CONTRACT, "rental_product_id", id))
          if (!b.isMapped(CONTRACT, id(contract))) b.issue(
            "LATE_LEGACY_WRITE",
            CONTRACT,
            id(contract),
            "New legacy history appeared after the product was imported"
          );
      } else if (
        b.targets
          .get("items")
          .stream()
          .anyMatch(
            i ->
              Objects.equals(choice.businessId(), num(i, "business_id")) &&
              Objects.toString(asset, "").equalsIgnoreCase(str(i, "asset_code"))
          )
      ) b.issue(
        "ASSET_CONFLICT",
        PRODUCT,
        id,
        "Target business already has this asset code"
      );
      var active = b
        .children(CONTRACT, "rental_product_id", id)
        .stream()
        .filter(c ->
          Set.of("REQUESTED", "RENTED", "RETURNED").contains(
            CONTRACT_STATES.getOrDefault(str(c, "status"), "INVALID")
          )
        )
        .toList();
      if (active.size() > 1) b.issue(
        "MULTIPLE_ACTIVE_RENTALS",
        PRODUCT,
        id,
        "An item has multiple active legacy contracts"
      );
      if (active.size() == 1) {
        String expected = switch (
          CONTRACT_STATES.get(str(active.getFirst(), "status"))
        ) {
          case "REQUESTED" -> "AVAILABLE";
          case "RENTED" -> "RENTED";
          default -> "INSPECTION";
        };
        if (!expected.equals(ITEM_STATES.get(str(row, "status")))) b.issue(
          "ITEM_STATE_CONFLICT",
          PRODUCT,
          id,
          "Item state disagrees with its active contract"
        );
      }
      if (
        active.isEmpty() &&
        Set.of("RESERVED", "RENTED").contains(str(row, "status"))
      ) b.issue(
        "ITEM_STATE_CONFLICT",
        PRODUCT,
        id,
        "Reserved/rented item has no active legacy contract"
      );
    }
  }

  private void validateHistory(Batch b) {
    for (var row : b.rows.get(CONTRACT)) {
      Long id = id(row);
      String state = str(row, "status");
      if (!CONTRACT_STATES.containsKey(state)) b.issue(
        "CONTRACT_STATE_INVALID",
        CONTRACT,
        id,
        "Unknown legacy contract state"
      );
      if (POST_START.contains(state)) {
        if (
          row.get("rented_at") == null ||
          row.get("due_at") == null ||
          compare(row.get("due_at"), row.get("rented_at")) <= 0
        ) b.issue(
          "RENTAL_TIME_INVALID",
          CONTRACT,
          id,
          "Rental period is missing or inconsistent"
        );
        requireRecord(b, id, "BEFORE_RENTAL");
      }
      if (
        Set.of("RETURNED", "INSPECTING", "COMPLETED").contains(state) &&
        (row.get("returned_at") == null ||
          compare(row.get("returned_at"), row.get("rented_at")) < 0)
      ) b.issue(
        "RETURN_TIME_INVALID",
        CONTRACT,
        id,
        "Return time is missing or inconsistent"
      );
      if (Set.of("INSPECTING", "COMPLETED").contains(state)) requireRecord(
        b,
        id,
        "AFTER_RENTAL"
      );
      if (
        "COMPLETED".equals(state) &&
        b.children(INSPECTION, "rental_contract_id", id).isEmpty()
      ) b.issue(
        "INSPECTION_MISSING",
        CONTRACT,
        id,
        "Completed contract has no final inspection"
      );
    }
    for (var row : b.rows.get(CONDITION))
      if (
        !Set.of("BEFORE_RENTAL", "AFTER_RENTAL").contains(
          str(row, "record_type")
        )
      ) b.issue(
        "RECORD_STAGE_INVALID",
        CONDITION,
        id(row),
        "Unknown condition stage"
      );
    for (var row : b.rows.get(PHOTO)) {
      try {
        PhotoFile file = inspectPhoto(row);
        b.files.put(id(row), file);
        if (b.isMapped(PHOTO, id(row))) {
          var target = jdbc.queryForList(
            "SELECT file_hash FROM condition_photo WHERE photo_id=?",
            b.target(PHOTO, id(row))
          );
          if (
            !target.isEmpty() &&
            !file.hash.equals(target.getFirst().get("file_hash"))
          ) b.issue(
            "PHOTO_CHANGED",
            PHOTO,
            id(row),
            "Original photo bytes changed after import"
          );
        }
        if (row.get("original_filename") == null) b.warn(
          "FILENAME_FROM_KEY",
          PHOTO,
          id(row),
          "Filename is derived from the preserved storage key"
        );
        if (!ANGLES.contains(normalize(str(row, "viewpoint")))) b.warn(
          "ANGLE_OTHER",
          PHOTO,
          id(row),
          "Unmapped photo direction becomes OTHER; original direction remains in legacy metadata"
        );
      } catch (RuntimeException e) {
        b.issue(
          "PHOTO_UNAVAILABLE",
          PHOTO,
          id(row),
          "Original photo is missing, unsupported or unreadable under the configured image storage root"
        );
      }
    }
    for (var row : b.rows.get(DAMAGE)) {
      var condition = b.find(CONDITION, num(row, "condition_record_id"));
      String comparison = str(row, "comparison_result");
      if (!LOCATIONS.contains(Objects.toString(location(b, row), ""))) b.issue(
        "DAMAGE_LOCATION_REQUIRED",
        DAMAGE,
        id(row),
        "Map the original damage location explicitly"
      );
      if (
        !Set.of(
          "SCRATCH",
          "DENT",
          "CRACK",
          "BROKEN",
          "STAIN",
          "OTHER"
        ).contains(damageType(row))
      ) b.issue("DAMAGE_TYPE_INVALID", DAMAGE, id(row), "Unknown damage type");
      if (
        "BEFORE_RENTAL".equals(str(condition, "record_type")) &&
        !"BASELINE".equals(comparison)
      ) b.issue(
        "DAMAGE_COMPARISON_INVALID",
        DAMAGE,
        id(row),
        "Intake observation is not a baseline"
      );
      if (
        "AFTER_RENTAL".equals(str(condition, "record_type")) &&
        !Set.of("UNRESOLVED", "EXISTING", "NEW").contains(comparison)
      ) b.issue(
        "DAMAGE_COMPARISON_INVALID",
        DAMAGE,
        id(row),
        "Unknown return comparison result"
      );
      if ("EXISTING".equals(comparison)) {
        var before = b.find(DAMAGE, num(row, "matched_before_damage_id"));
        var beforeCondition =
          before == null
            ? null
            : b.find(CONDITION, num(before, "condition_record_id"));
        if (
          beforeCondition == null ||
          !"BEFORE_RENTAL".equals(str(beforeCondition, "record_type")) ||
          !Objects.equals(
            num(condition, "rental_contract_id"),
            num(beforeCondition, "rental_contract_id")
          )
        ) b.issue(
          "DAMAGE_REFERENCE_INVALID",
          DAMAGE,
          id(row),
          "Existing damage must reference this contract's intake observation"
        );
      } else if (row.get("matched_before_damage_id") != null) b.issue(
        "DAMAGE_REFERENCE_INVALID",
        DAMAGE,
        id(row),
        "Only EXISTING may have a matching intake damage"
      );
    }
    // A selected, previously imported parent cannot acquire untracked children later.
    for (var row : b.rows.get(CONDITION))
      if (b.isMapped(CONDITION, id(row))) for (String child : List.of(
        PHOTO,
        DAMAGE
      ))
        for (var r : b.children(child, "condition_record_id", id(row)))
          if (!b.isMapped(child, id(r))) b.issue(
            "LATE_LEGACY_WRITE",
            child,
            id(r),
            "Legacy record changed after cutover"
          );
    for (var row : b.rows.get(CONTRACT))
      if (b.isMapped(CONTRACT, id(row))) for (String child : List.of(
        CONDITION,
        INSPECTION
      ))
        for (var r : b.children(child, "rental_contract_id", id(row)))
          if (!b.isMapped(child, id(r))) b.issue(
            "LATE_LEGACY_WRITE",
            child,
            id(r),
            "Legacy history changed after cutover"
          );
  }

  private void validateInspections(Batch b) {
    for (var row : b.rows.get(INSPECTION)) {
      Long id = id(row),
        contractId = num(row, "rental_contract_id");
      var contract = b.find(CONTRACT, contractId);
      var choice = b.inspectors.get(id);
      if (!b.isMapped(INSPECTION, id)) {
        if (
          choice == null ||
          (choice.inspectorMemberId() == null) ==
            (choice.legacyInspectorMemberId() == null)
        ) b.issue(
          "INSPECTOR_REQUIRED",
          INSPECTION,
          id,
          "Specify exactly one actual existing or legacy inspector; the import operator is not assumed to be the inspector"
        );
        else {
          Map<String, Object> inspector =
            choice.inspectorMemberId() != null
              ? b.targetRow("members", "member_id", choice.inspectorMemberId())
              : b.find(MEMBER, choice.legacyInspectorMemberId());
          String inspectorRole =
            choice.inspectorMemberId() != null
              ? str(inspector, "member_role_cd")
              : role(inspector);
          if (choice.legacyInspectorMemberId() != null) {
            var memberChoice = b.members.get(choice.legacyInspectorMemberId());
            if (
              memberChoice != null && memberChoice.existingMemberId() != null
            ) inspectorRole = str(
              b.targetRow(
                "members",
                "member_id",
                memberChoice.existingMemberId()
              ),
              "member_role_cd"
            );
          }
          if (
            !Set.of("ADMIN", "INSPECTOR").contains(
              Objects.toString(inspectorRole, "")
            )
          ) b.issue(
            "INSPECTOR_ROLE_INVALID",
            INSPECTION,
            id,
            "Mapped inspector must have an inspector role"
          );
        }
      }
      requireRecord(b, contractId, "BEFORE_RENTAL");
      requireRecord(b, contractId, "AFTER_RENTAL");
      if (
        !Set.of("INSPECTING", "COMPLETED").contains(str(contract, "status"))
      ) b.issue(
        "INSPECTION_STATE_INVALID",
        INSPECTION,
        id,
        "Inspection does not match the legacy contract stage"
      );
      if ("COMPLETED".equals(str(contract, "status"))) {
        var after = b.condition(contractId, "AFTER_RENTAL");
        var observed =
          after == null
            ? List.<Map<String, Object>>of()
            : b.children(DAMAGE, "condition_record_id", id(after));
        if (
          observed
            .stream()
            .anyMatch(
              d ->
                !Set.of("EXISTING", "NEW").contains(str(d, "comparison_result"))
            )
        ) b.issue(
          "UNRESOLVED_FINAL_INSPECTION",
          INSPECTION,
          id,
          "Completed inspection contains unresolved observations"
        );
        String expected = observed
          .stream()
          .anyMatch(d -> "NEW".equals(str(d, "comparison_result")))
          ? "NEW_DAMAGE_CONFIRMED"
          : "NO_NEW_DAMAGE";
        if (!expected.equals(str(row, "result"))) b.issue(
          "INSPECTION_RESULT_CONFLICT",
          INSPECTION,
          id,
          "Final inspection disagrees with stored damage comparisons"
        );
      }
    }
  }

  private void requireRecord(Batch b, Long contract, String type) {
    var record = b.condition(contract, type);
    if (
      record == null ||
      b.children(PHOTO, "condition_record_id", id(record)).isEmpty()
    ) b.issue(
      "CONDITION_PHOTO_REQUIRED",
      CONTRACT,
      contract,
      "Missing " + type + " record or original photo"
    );
  }

  private boolean confirmed(Batch b, Map<String, Object> row) {
    var contract = b.find(CONTRACT, num(row, "rental_contract_id"));
    return "BEFORE_RENTAL".equals(str(row, "record_type"))
      ? POST_START.contains(str(contract, "status"))
      : Set.of("INSPECTING", "COMPLETED").contains(str(contract, "status")) ||
          !b.children(INSPECTION, "rental_contract_id", id(contract)).isEmpty();
  }

  private PhotoFile inspectPhoto(Map<String, Object> row) {
    byte[] bytes = storage.load(str(row, "storage_key"));
    String mime;
    if (
      bytes.length >= 8 &&
      bytes[0] == (byte) 0x89 &&
      bytes[1] == 'P' &&
      bytes[2] == 'N' &&
      bytes[3] == 'G' &&
      bytes[4] == 13 &&
      bytes[5] == 10 &&
      bytes[6] == 26 &&
      bytes[7] == 10
    ) mime = "image/png";
    else if (
      bytes.length >= 3 &&
      bytes[0] == (byte) 0xff &&
      bytes[1] == (byte) 0xd8 &&
      bytes[2] == (byte) 0xff
    ) mime = "image/jpeg";
    else if (
      bytes.length >= 12 &&
      new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF") &&
      new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")
    ) mime = "image/webp";
    else throw new IllegalArgumentException("Unsupported source image");
    String filename = str(row, "original_filename");
    if (filename == null || filename.isBlank()) {
      String key = str(row, "storage_key").replace('\\', '/');
      filename = key.substring(key.lastIndexOf('/') + 1);
    }
    if (
      filename.isBlank() || filename.length() > 255
    ) throw new IllegalArgumentException("Invalid source filename");
    return new PhotoFile(filename, mime, (long) bytes.length, hash(bytes));
  }

  private List<Map<String, Object>> select(
    String table,
    String column,
    Collection<Long> ids,
    boolean lock
  ) {
    if (ids.isEmpty()) return new ArrayList<>();
    String marks = String.join(",", Collections.nCopies(ids.size(), "?"));
    return jdbc.queryForList(
      "SELECT * FROM " +
        table +
        " WHERE " +
        column +
        " IN (" +
        marks +
        ") ORDER BY id" +
        (lock ? " FOR SHARE" : ""),
      ids.toArray()
    );
  }

  private Long insert(
    String table,
    String keyColumn,
    Map<String, Object> values
  ) {
    String columns = values
        .keySet()
        .stream()
        .map(c -> "`" + c + "`")
        .collect(Collectors.joining(",")),
      marks = String.join(",", Collections.nCopies(values.size(), "?"));
    var key = new GeneratedKeyHolder();
    jdbc.update(connection -> {
      var statement = connection.prepareStatement(
        "INSERT INTO `" + table + "` (" + columns + ") VALUES (" + marks + ")",
        Statement.RETURN_GENERATED_KEYS
      );
      int i = 1;
      for (Object value : values.values()) statement.setObject(i++, value);
      return statement;
    }, key);
    return Objects.requireNonNull(key.getKey()).longValue();
  }

  private AuthenticatedMember admin() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (
      auth == null ||
      !(auth.getPrincipal() instanceof AuthenticatedMember actor) ||
      actor.role() != MemberRole.ADMIN
    ) throw new AccessDeniedException("Administrator role required");
    return actor;
  }

  private static String role(Map<String, Object> row) {
    if (row == null) return null;
    return switch (Objects.toString(row.get("role"), "")) {
      case "USER" -> "CUSTOMER";
      case "OPERATOR" -> "INSPECTOR";
      default -> null;
    };
  }

  private static String login(LegacyManifest.MemberMapping choice, Long id) {
    return choice == null || choice.loginId() == null
      ? "legacy-" + id
      : choice.loginId().toLowerCase(Locale.ROOT);
  }

  private static String asset(
    LegacyManifest.ProductMapping choice,
    Map<String, Object> row
  ) {
    return choice.assetCode() == null
      ? str(row, "asset_code")
      : choice.assetCode();
  }

  private static String stage(Map<String, Object> row) {
    return "BEFORE_RENTAL".equals(str(row, "record_type"))
      ? "INTAKE"
      : "RETURN";
  }

  private static String damageType(Map<String, Object> row) {
    return "MISSING_PART".equals(str(row, "damage_type"))
      ? "OTHER"
      : str(row, "damage_type");
  }

  private static String angle(Map<String, Object> row) {
    String angle = normalize(str(row, "viewpoint"));
    return ANGLES.contains(angle) ? angle : "OTHER";
  }

  private static String location(Batch b, Map<String, Object> row) {
    String original = normalize(str(row, "damage_location"));
    return b.locations.getOrDefault(
      original,
      LOCATIONS.contains(original) ? original : null
    );
  }

  private static int compare(Object a, Object b) {
    if (a == null || b == null) return -1;
    return a.toString().compareTo(b.toString());
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  private static String str(Map<String, Object> row, String key) {
    return row == null || row.get(key) == null ? null : row.get(key).toString();
  }

  private static Long num(Map<String, Object> row, String key) {
    return row == null || row.get(key) == null
      ? null
      : ((Number) row.get(key)).longValue();
  }

  private static Long id(Map<String, Object> row) {
    return num(row, "id");
  }

  private static Set<Long> ids(List<Map<String, Object>> rows) {
    return rows
      .stream()
      .map(LegacyImportService::id)
      .collect(Collectors.toCollection(TreeSet::new));
  }

  private static String key(String table, Long id) {
    return table + ":" + id;
  }

  private static Map<String, Object> map(Object... pairs) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) map.put(
      (String) pairs[i],
      pairs[i + 1]
    );
    return map;
  }

  private static String hash(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(bytes)
      );
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String digest(Object object) {
    return hash(canonical(object).getBytes(StandardCharsets.UTF_8));
  }

  private static String canonical(Object value) {
    if (value instanceof Map<?, ?> m) {
      Map<String, String> sorted = new TreeMap<>();
      m.forEach((k, v) -> sorted.put(k.toString(), canonical(v)));
      return sorted
        .entrySet()
        .stream()
        .map(e -> e.getKey().length() + ":" + e.getKey() + e.getValue())
        .collect(Collectors.joining("", "M" + sorted.size() + "{", "}"));
    }
    if (value instanceof Collection<?> c) return c
      .stream()
      .map(LegacyImportService::canonical)
      .collect(Collectors.joining("", "L" + c.size() + "[", "]"));
    if (value == null) return "Z";
    String scalar = value.getClass().getName() + ":" + value;
    return "V" + scalar.length() + ":" + scalar;
  }

  private record PhotoFile(
    String filename,
    String mime,
    Long bytes,
    String hash
  ) {}

  private final class Batch {

    final Map<String, List<Map<String, Object>>> rows = new TreeMap<>(),
      targets = new TreeMap<>();
    final Map<String, Map<String, Object>> mapped = new TreeMap<>();
    final Map<Long, LegacyManifest.MemberMapping> members;
    final Map<Long, LegacyManifest.ProductMapping> products;
    final Map<Long, LegacyManifest.InspectorMapping> inspectors;
    final Map<String, String> locations = new TreeMap<>();
    final Map<Long, PhotoFile> files = new TreeMap<>();
    final List<Map<String, Object>> issues = new ArrayList<>(),
      warnings = new ArrayList<>();
    String sourceDigest, token;

    Batch(LegacyManifest manifest) {
      members = index(
        manifest.members(),
        LegacyManifest.MemberMapping::legacyMemberId
      );
      products = index(
        manifest.products(),
        LegacyManifest.ProductMapping::legacyProductId
      );
      inspectors = index(
        manifest.inspections(),
        LegacyManifest.InspectorMapping::legacyInspectionId
      );
      for (var entry : manifest.damageLocations()) {
        if (
          !LOCATIONS.contains(entry.target()) ||
          locations.putIfAbsent(normalize(entry.source()), entry.target()) !=
            null
        ) throw new IllegalArgumentException(
          "Invalid or duplicate damage location mapping"
        );
      }
    }

    Map<String, Object> find(String table, Long id) {
      return rows
        .get(table)
        .stream()
        .filter(row -> Objects.equals(id, id(row)))
        .findFirst()
        .orElse(null);
    }

    Map<String, Object> targetRow(String table, String key, Long id) {
      return targets
        .get(table)
        .stream()
        .filter(row -> Objects.equals(id, num(row, key)))
        .findFirst()
        .orElse(null);
    }

    List<Map<String, Object>> children(String table, String field, Long id) {
      return rows
        .get(table)
        .stream()
        .filter(row -> Objects.equals(id, num(row, field)))
        .toList();
    }

    Map<String, Object> condition(Long contract, String type) {
      return children(CONDITION, "rental_contract_id", contract)
        .stream()
        .filter(row -> type.equals(str(row, "record_type")))
        .findFirst()
        .orElse(null);
    }

    Map<String, Object> mapping(String table, Long id) {
      return mapped.get(key(table, id));
    }

    boolean isMapped(String table, Long id) {
      return mapping(table, id) != null;
    }

    Long target(String table, Long id) {
      var found = mapping(table, id);
      if (found == null) throw new IllegalStateException(
        "Missing target mapping for " + table + "#" + id
      );
      return num(found, "target_id");
    }

    void issue(String code, String table, Long id, String message) {
      issues.add(
        map("code", code, "entity", table, "legacyId", id, "message", message)
      );
    }

    void warn(String code, String table, Long id, String message) {
      warnings.add(
        map("code", code, "entity", table, "legacyId", id, "message", message)
      );
    }

    void remember(
      String table,
      Map<String, Object> row,
      Long target,
      Long run,
      Map<String, Integer> counts
    ) {
      String fingerprint = digest(row);
      jdbc.update(
        "INSERT INTO legacy_entity_map(source_table,source_id,target_table,target_id,source_digest,import_run_id) VALUES (?,?,?,?,?,?)",
        table,
        id(row),
        TARGETS.get(table),
        target,
        fingerprint,
        run
      );
      mapped.put(
        key(table, id(row)),
        map("target_id", target, "source_digest", fingerprint)
      );
      counts.merge(TARGETS.get(table), 1, Integer::sum);
    }

    Map<String, Object> report() {
      Map<String, Integer> sourceCounts = new TreeMap<>(),
        pending = new TreeMap<>(),
        existing = new TreeMap<>();
      for (String table : SOURCES) {
        sourceCounts.put(table, rows.get(table).size());
        int done = (int) rows
          .get(table)
          .stream()
          .filter(r -> isMapped(table, id(r)))
          .count();
        pending.put(TARGETS.get(table), rows.get(table).size() - done);
        existing.put(TARGETS.get(table), done);
      }
      return map(
        "ready",
        issues.isEmpty(),
        "planToken",
        token,
        "sourceCounts",
        sourceCounts,
        "pendingCounts",
        pending,
        "alreadyMappedCounts",
        existing,
        "issues",
        issues,
        "warnings",
        warnings,
        "members",
        rows
          .get(MEMBER)
          .stream()
          .map(r ->
            map(
              "legacyId",
              id(r),
              "loginId",
              login(members.get(id(r)), id(r)),
              "role",
              role(r),
              "status",
              r.get("status")
            )
          )
          .toList()
      );
    }
  }

  private static <T> Map<Long, T> index(List<T> values, Function<T, Long> id) {
    Map<Long, T> result = new TreeMap<>();
    for (T value : values)
      if (
        result.putIfAbsent(id.apply(value), value) != null
      ) throw new IllegalArgumentException("Duplicate legacy mapping ID");
    return result;
  }
}
