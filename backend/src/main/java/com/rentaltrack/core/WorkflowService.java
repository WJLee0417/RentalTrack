package com.rentaltrack.core;

import com.rentaltrack.auth.domain.AuthenticatedMember;
import com.rentaltrack.global.error.AccessDeniedException;
import com.rentaltrack.global.error.ResourceNotFoundException;
import com.rentaltrack.member.domain.MemberRole;
import com.rentaltrack.storage.application.*;
import java.io.*;
import java.math.BigDecimal;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class WorkflowService {

  private final BusinessRepository businesses;
  private final RentalItemRepository items;
  private final RentalAgreementRepository rentals;
  private final ConditionSnapshotRepository conditions;
  private final ConditionPhotoRepository photos;
  private final DamageObservationRepository damages;
  private final DamagePhotoRepository evidence;
  private final ImageAnalysisResultRepository analyses;
  private final ReturnInspectionRepository inspections;
  private final DamageAssessmentRepository assessments;
  private final JdbcTemplate jdbc;
  private final ImageStorage storage;

  public WorkflowService(
    BusinessRepository businesses,
    RentalItemRepository items,
    RentalAgreementRepository rentals,
    ConditionSnapshotRepository conditions,
    ConditionPhotoRepository photos,
    DamageObservationRepository damages,
    DamagePhotoRepository evidence,
    ImageAnalysisResultRepository analyses,
    ReturnInspectionRepository inspections,
    DamageAssessmentRepository assessments,
    JdbcTemplate jdbc,
    ImageStorage storage
  ) {
    this.businesses = businesses;
    this.items = items;
    this.rentals = rentals;
    this.conditions = conditions;
    this.photos = photos;
    this.damages = damages;
    this.evidence = evidence;
    this.analyses = analyses;
    this.inspections = inspections;
    this.assessments = assessments;
    this.jdbc = jdbc;
    this.storage = storage;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> catalog() {
    return items.findAllByOrderByIdDesc().stream().map(this::itemView).toList();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> businessList() {
    return businesses
      .findAllByBusinessStatusCdOrderByIdAsc("ACTIVE")
      .stream()
      .map(this::businessView)
      .toList();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> codes() {
    return jdbc.queryForList(
      "SELECT code_group_id AS codeGroupId, code_value AS codeValue, code_name AS codeName FROM common_code WHERE use_yn='Y' ORDER BY code_group_id,display_order"
    );
  }

  public Map<String, Object> addBusiness(Requests.BusinessInput input) {
    admin();
    Business b = new Business();
    b.businessNo = input.businessNo();
    b.businessName = input.businessName().trim();
    b.businessStatusCd = "ACTIVE";
    b.contactEmail = input.contactEmail();
    return businessView(businesses.saveAndFlush(b));
  }

  public Map<String, Object> addItem(Requests.ItemInput input) {
    admin();
    Business b = businesses
      .findById(input.businessId())
      .orElseThrow(() -> missing("Business"));
    require("ACTIVE".equals(b.businessStatusCd), "Business is not active");
    code("CATEGORY", input.categoryCd());
    RentalItem item = new RentalItem();
    item.businessId = b.id;
    item.assetCode = input.assetCode().trim();
    item.itemName = input.itemName().trim();
    item.categoryCd = input.categoryCd();
    item.description = input.description();
    item.itemStatusCd = "AVAILABLE";
    return itemView(items.saveAndFlush(item));
  }

  public Map<String, Object> requestRental(Requests.RentalInput input) {
    var actor = actor();
    RentalItem item = items
      .lock(input.itemId())
      .orElseThrow(() -> missing("Item"));
    Business business = businesses
      .findById(item.businessId)
      .orElseThrow(() -> missing("Business"));
    require(
      "ACTIVE".equals(business.businessStatusCd),
      "Business is not active"
    );
    require(
      "AVAILABLE".equals(item.itemStatusCd) && !reserved(item.id),
      "Item is already reserved or unavailable"
    );
    RentalAgreement rental = new RentalAgreement();
    rental.contractNo = UUID.randomUUID().toString();
    rental.memberId = actor.memberId();
    rental.itemId = item.id;
    rental.requestedAt = LocalDateTime.now();
    rental.contractStatusCd = "REQUESTED";
    return rentalView(rentals.saveAndFlush(rental));
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> myRentals() {
    return rentals
      .findAllByMemberIdOrderByIdDesc(actor().memberId())
      .stream()
      .map(this::rentalView)
      .toList();
  }

  public Map<String, Object> start(Long id, Requests.StartInput input) {
    RentalAgreement rental = contract(id, true);
    requireState(rental, "REQUESTED");
    ConditionSnapshot before = conditions
      .findTopByContractIdAndRecordStageCdOrderByRecordRevisionNoDesc(
        id,
        "INTAKE"
      )
      .orElseThrow(() ->
        new IllegalStateException("Confirmed intake record is required")
      );
    require(
      "Y".equals(before.confirmedYn),
      "Latest intake record must be confirmed"
    );
    require(
      !input.rentalStartAt().isBefore(rental.requestedAt) &&
        input.rentalEndAt().isAfter(input.rentalStartAt()),
      "Invalid rental period"
    );
    RentalItem item = items
      .lock(rental.itemId)
      .orElseThrow(() -> missing("Item"));
    require("AVAILABLE".equals(item.itemStatusCd), "Item is unavailable");
    rental.rentalStartAt = input.rentalStartAt();
    rental.rentalEndAt = input.rentalEndAt();
    rental.contractStatusCd = "RENTED";
    item.itemStatusCd = "RENTED";
    return rentalView(rental);
  }

  public Map<String, Object> cancel(Long id) {
    RentalAgreement rental = contract(id, true);
    requireState(rental, "REQUESTED");
    rental.contractStatusCd = "CANCELED";
    return rentalView(rental);
  }

  public Map<String, Object> returnRental(Long id, Requests.ReturnInput input) {
    RentalAgreement rental = contract(id, true);
    requireState(rental, "RENTED");
    require(
      !input.actualReturnAt().isBefore(rental.rentalStartAt),
      "Return precedes rental start"
    );
    RentalItem item = items
      .lock(rental.itemId)
      .orElseThrow(() -> missing("Item"));
    rental.actualReturnAt = input.actualReturnAt();
    rental.contractStatusCd = "RETURNED";
    item.itemStatusCd = "INSPECTION";
    return rentalView(rental);
  }

  public Map<String, Object> addCondition(
    Long id,
    Requests.ConditionInput input
  ) {
    RentalAgreement rental = contract(id, true);
    code("RECORD_STAGE", input.recordStageCd());
    editableStage(rental, input.recordStageCd());
    require(
      conditions
        .findTopByContractIdAndRecordStageCdOrderByRecordRevisionNoDesc(
          id,
          input.recordStageCd()
        )
        .isEmpty(),
      "Use a new revision for an existing stage"
    );
    ConditionSnapshot record = new ConditionSnapshot();
    record.contractId = id;
    record.recordStageCd = input.recordStageCd();
    record.recordRevisionNo = 1;
    record.recordNote = input.recordNote();
    record.confirmedYn = "N";
    record.recordedAt = LocalDateTime.now();
    return conditionView(conditions.saveAndFlush(record));
  }

  public Map<String, Object> editCondition(Long id, Requests.NoteInput input) {
    ConditionSnapshot record = draft(id);
    record.recordNote = input.recordNote();
    return conditionView(record);
  }

  public Map<String, Object> confirm(Long id) {
    ConditionSnapshot record = draft(id);
    require(
      !photos.findAllByConditionIdOrderByPhotoSeqAsc(id).isEmpty(),
      "At least one photo is required before confirmation"
    );
    record.confirmedYn = "Y";
    return conditionView(record);
  }

  public Map<String, Object> revise(Long id, Requests.NoteInput input) {
    Long contractId = conditions
      .contractIdFor(id)
      .orElseThrow(() -> missing("Condition"));
    RentalAgreement rental = contract(contractId, true);
    ConditionSnapshot previous = condition(id);
    editableStage(rental, previous.recordStageCd);
    require(
      "Y".equals(previous.confirmedYn),
      "Only confirmed records can be revised"
    );
    ConditionSnapshot latest = conditions
      .findTopByContractIdAndRecordStageCdOrderByRecordRevisionNoDesc(
        previous.contractId,
        previous.recordStageCd
      )
      .orElseThrow();
    require(latest.id.equals(id), "Revise the latest record");
    ConditionSnapshot next = new ConditionSnapshot();
    next.contractId = previous.contractId;
    next.recordStageCd = previous.recordStageCd;
    next.recordRevisionNo = previous.recordRevisionNo + 1;
    next.confirmedYn = "N";
    next.recordedAt = LocalDateTime.now();
    next.recordNote = input.recordNote();
    conditions.saveAndFlush(next);
    Map<Long, Long> copiedPhotos = new HashMap<>();
    for (ConditionPhoto old : photos.findAllByConditionIdOrderByPhotoSeqAsc(
      id
    )) {
      ConditionPhoto copy = new ConditionPhoto();
      copy.conditionId = next.id;
      copy.photoSeq = old.photoSeq;
      copy.storageKey = old.storageKey;
      copy.originalFileName = old.originalFileName;
      copy.mimeType = old.mimeType;
      copy.fileSize = old.fileSize;
      copy.fileHash = old.fileHash;
      copy.captureAngleCd = old.captureAngleCd;
      copy.capturedAt = old.capturedAt;
      photos.saveAndFlush(copy);
      copiedPhotos.put(old.id, copy.id);
    }
    for (DamageObservation old : damages.findAllByConditionIdOrderByIdAsc(id)) {
      DamageObservation copy = new DamageObservation();
      copy.conditionId = next.id;
      copy.damageLocationCd = old.damageLocationCd;
      copy.damageTypeCd = old.damageTypeCd;
      copy.severityCd = old.severityCd;
      copy.damageDescription = old.damageDescription;
      damages.saveAndFlush(copy);
      for (DamagePhoto oldLink : evidence.findAllByDamageIdOrderByIdAsc(
        old.id
      )) {
        DamagePhoto link = new DamagePhoto();
        link.damageId = copy.id;
        link.photoId = copiedPhotos.get(oldLink.photoId);
        require(link.photoId != null, "Invalid source evidence");
        link.bboxXRatio = oldLink.bboxXRatio;
        link.bboxYRatio = oldLink.bboxYRatio;
        link.bboxWidthRatio = oldLink.bboxWidthRatio;
        link.bboxHeightRatio = oldLink.bboxHeightRatio;
        evidence.save(link);
      }
    }
    return conditionView(next);
  }

  public Map<String, Object> addPhoto(
    Long id,
    MultipartFile file,
    String angle,
    LocalDateTime capturedAt
  ) {
    ConditionSnapshot record = draft(id);
    code("CAPTURE_ANGLE", angle);
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot read image", e);
    }
    if (
      bytes.length == 0 || bytes.length > 10 * 1024 * 1024
    ) throw new IllegalArgumentException(
      "Image must be between 1 byte and 10 MiB"
    );
    String mime = validateImage(bytes);
    String filename = Optional.ofNullable(file.getOriginalFilename()).orElse(
      "image"
    );
    if (filename.length() > 255) throw new IllegalArgumentException(
      "Filename is too long"
    );
    StoredImage stored = storage.store(
      new ImageUpload("conditions/" + id, filename, mime, bytes)
    );
    TransactionSynchronizationManager.registerSynchronization(
      new TransactionSynchronization() {
        @Override
        public void afterCompletion(int status) {
          if (status != STATUS_COMMITTED) storage.delete(stored.storageKey());
        }
      }
    );
    ConditionPhoto photo = new ConditionPhoto();
    photo.conditionId = record.id;
    photo.photoSeq =
      photos
        .findAllByConditionIdOrderByPhotoSeqAsc(id)
        .stream()
        .mapToInt(p -> p.photoSeq)
        .max()
        .orElse(0) + 1;
    photo.storageKey = stored.storageKey();
    photo.originalFileName = filename;
    photo.mimeType = mime;
    photo.fileSize = (long) bytes.length;
    try {
      photo.fileHash = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(bytes)
      );
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    photo.captureAngleCd = angle;
    photo.capturedAt = capturedAt;
    return photoView(photos.saveAndFlush(photo));
  }

  public Map<String, Object> addDamage(Long id, Requests.DamageInput input) {
    ConditionSnapshot record = draft(id);
    code("DAMAGE_LOCATION", input.damageLocationCd());
    code("DAMAGE_TYPE", input.damageTypeCd());
    code("SEVERITY", input.severityCd());
    DamageObservation damage = new DamageObservation();
    damage.conditionId = record.id;
    damage.damageLocationCd = input.damageLocationCd();
    damage.damageTypeCd = input.damageTypeCd();
    damage.damageDescription = input.damageDescription();
    damage.severityCd = input.severityCd();
    return damageView(damages.saveAndFlush(damage));
  }

  public Map<String, Object> addEvidence(
    Long id,
    Requests.EvidenceInput input
  ) {
    DamageObservation damage = damages
      .findById(id)
      .orElseThrow(() -> missing("Damage"));
    draft(damage.conditionId);
    ConditionPhoto photo = photos
      .findById(input.photoId())
      .orElseThrow(() -> missing("Photo"));
    require(
      photo.conditionId.equals(damage.conditionId),
      "Damage and photo must belong to the same condition record"
    );
    require(
      !evidence.existsByDamageIdAndPhotoId(id, photo.id),
      "Photo is already linked"
    );
    DamagePhoto link = new DamagePhoto();
    link.damageId = id;
    link.photoId = photo.id;
    if (input.box() != null) {
      input.box().check();
      link.bboxXRatio = input.box().x();
      link.bboxYRatio = input.box().y();
      link.bboxWidthRatio = input.box().width();
      link.bboxHeightRatio = input.box().height();
    }
    return evidenceView(evidence.saveAndFlush(link));
  }

  public Map<String, Object> editDamage(Long id, Requests.DamageInput input) {
    DamageObservation damage = damages
      .findById(id)
      .orElseThrow(() -> missing("Damage"));
    draft(damage.conditionId);
    code("DAMAGE_LOCATION", input.damageLocationCd());
    code("DAMAGE_TYPE", input.damageTypeCd());
    code("SEVERITY", input.severityCd());
    damage.damageLocationCd = input.damageLocationCd();
    damage.damageTypeCd = input.damageTypeCd();
    damage.severityCd = input.severityCd();
    damage.damageDescription = input.damageDescription();
    return damageView(damage);
  }

  public void deleteDamage(Long id) {
    DamageObservation damage = damages
      .findById(id)
      .orElseThrow(() -> missing("Damage"));
    draft(damage.conditionId);
    evidence.deleteAll(evidence.findAllByDamageIdOrderByIdAsc(id));
    evidence.flush();
    damages.delete(damage);
  }

  public Map<String, Object> addAnalysis(
    Long photoId,
    Requests.AnalysisInput input
  ) {
    staff();
    ConditionPhoto photo = photos
      .findById(photoId)
      .orElseThrow(() -> missing("Photo"));
    contract(condition(photo.conditionId).contractId, true);
    code("DAMAGE_TYPE", input.predictedDamageTypeCd());
    input.box().check();
    require(
      input.confidenceScore().signum() >= 0 &&
        input.confidenceScore().compareTo(BigDecimal.ONE) <= 0,
      "Invalid confidence"
    );
    ImageAnalysisResult result = new ImageAnalysisResult();
    result.photoId = photoId;
    result.analysisRunId = input.analysisRunId();
    result.detectionSeq = input.detectionSeq();
    result.modelName = input.modelName();
    result.modelVersion = input.modelVersion();
    result.predictedDamageTypeCd = input.predictedDamageTypeCd();
    result.confidenceScore = input.confidenceScore();
    result.bboxXRatio = input.box().x();
    result.bboxYRatio = input.box().y();
    result.bboxWidthRatio = input.box().width();
    result.bboxHeightRatio = input.box().height();
    result.analyzedAt = input.analyzedAt();
    return analysisView(analyses.saveAndFlush(result));
  }

  public Map<String, Object> createInspection(
    Long contractId,
    Requests.InspectionInput input
  ) {
    staff();
    RentalAgreement rental = contract(contractId, true);
    requireState(rental, "RETURNED");
    require(
      inspections.findByContractId(contractId).isEmpty(),
      "Inspection already exists"
    );
    ConditionSnapshot intake = condition(input.intakeConditionId()),
      returned = condition(input.returnConditionId());
    require(
      intake.contractId.equals(contractId) &&
        returned.contractId.equals(contractId),
      "Inspection records must belong to this contract"
    );
    require(
      "INTAKE".equals(intake.recordStageCd) &&
        "RETURN".equals(returned.recordStageCd),
      "Incorrect inspection stages"
    );
    require(
      "Y".equals(intake.confirmedYn) && "Y".equals(returned.confirmedYn),
      "Inspection records must be confirmed"
    );
    ReturnInspection inspection = new ReturnInspection();
    inspection.contractId = contractId;
    inspection.intakeConditionId = intake.id;
    inspection.returnConditionId = returned.id;
    inspection.inspectorMemberId = actor().memberId();
    inspection.inspectionStatusCd = "DRAFT";
    inspection.inspectionNote = input.inspectionNote();
    return inspectionView(inspections.saveAndFlush(inspection));
  }

  public Map<String, Object> beginInspection(Long id) {
    ReturnInspection inspection = inspectionForWrite(id);
    require(
      "DRAFT".equals(inspection.inspectionStatusCd),
      "Inspection is not a draft"
    );
    inspection.inspectionStatusCd = "IN_PROGRESS";
    return inspectionView(inspection);
  }

  public Map<String, Object> assess(Long id, Requests.AssessmentInput input) {
    ReturnInspection inspection = inspectionForWrite(id);
    require(
      "IN_PROGRESS".equals(inspection.inspectionStatusCd),
      "Inspection must be in progress"
    );
    code("ASSESSMENT_RESULT", input.assessmentResultCd());
    DamageObservation returned = damages
      .findById(input.returnDamageId())
      .orElseThrow(() -> missing("Return damage"));
    require(
      returned.conditionId.equals(inspection.returnConditionId),
      "Return damage is outside the fixed inspection record"
    );
    if ("EXISTING".equals(input.assessmentResultCd())) {
      require(
        input.intakeDamageId() != null,
        "Existing damage requires an intake damage reference"
      );
      DamageObservation intake = damages
        .findById(input.intakeDamageId())
        .orElseThrow(() -> missing("Intake damage"));
      require(
        intake.conditionId.equals(inspection.intakeConditionId),
        "Intake damage is outside the fixed inspection record"
      );
    } else require(
      input.intakeDamageId() == null,
      "Only EXISTING accepts an intake damage reference"
    );
    DamageAssessment result = assessments
      .findByInspectionIdAndReturnDamageId(id, returned.id)
      .orElseGet(DamageAssessment::new);
    result.inspectionId = id;
    result.returnDamageId = returned.id;
    result.intakeDamageId = input.intakeDamageId();
    result.assessmentResultCd = input.assessmentResultCd();
    result.assessmentReason = input.assessmentReason();
    return assessmentView(assessments.saveAndFlush(result));
  }

  public Map<String, Object> completeInspection(Long id) {
    ReturnInspection inspection = inspectionForWrite(id);
    require(
      "IN_PROGRESS".equals(inspection.inspectionStatusCd),
      "Inspection must be in progress"
    );
    List<DamageObservation> observed = damages.findAllByConditionIdOrderByIdAsc(
      inspection.returnConditionId
    );
    List<DamageAssessment> results =
      assessments.findAllByInspectionIdOrderByIdAsc(id);
    Set<Long> assessed = new HashSet<>();
    results.forEach(result -> assessed.add(result.returnDamageId));
    require(
      observed.stream().allMatch(damage -> assessed.contains(damage.id)),
      "Every return damage requires an assessment"
    );
    require(
      results
        .stream()
        .noneMatch(result -> "HOLD".equals(result.assessmentResultCd)),
      "Unresolved assessments block completion"
    );
    boolean newDamage = results
      .stream()
      .anyMatch(result -> "NEW".equals(result.assessmentResultCd));
    inspection.overallResultCd = newDamage ? "NEW_DAMAGE" : "NO_NEW_DAMAGE";
    inspection.inspectionStatusCd = "COMPLETED";
    inspection.inspectedAt = LocalDateTime.now();
    RentalAgreement rental = contract(inspection.contractId, true);
    rental.contractStatusCd = "INSPECTED";
    RentalItem item = items
      .lock(rental.itemId)
      .orElseThrow(() -> missing("Item"));
    item.itemStatusCd = newDamage ? "REPAIR" : "AVAILABLE";
    return inspectionView(inspection);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> detail(Long id) {
    RentalAgreement rental = contract(id, false);
    return map(
      "rental",
      rentalView(rental),
      "conditions",
      conditions
        .findAllByContractIdOrderByRecordStageCdAscRecordRevisionNoAsc(id)
        .stream()
        .map(this::conditionView)
        .toList(),
      "inspection",
      inspections.findByContractId(id).map(this::inspectionView).orElse(null)
    );
  }

  @Transactional(readOnly = true)
  public PhotoContent photoContent(Long id) {
    ConditionPhoto photo = photos
      .findById(id)
      .orElseThrow(() -> missing("Photo"));
    contract(condition(photo.conditionId).contractId, false);
    return new PhotoContent(storage.load(photo.storageKey), photo.mimeType);
  }

  public record PhotoContent(byte[] bytes, String mimeType) {}

  private ReturnInspection inspectionForWrite(Long id) {
    staff();
    Long contractId = inspections
      .contractIdFor(id)
      .orElseThrow(() -> missing("Inspection"));
    RentalAgreement rental = contract(contractId, true);
    requireState(rental, "RETURNED");
    return inspections.lock(id).orElseThrow();
  }

  private ConditionSnapshot draft(Long id) {
    Long contractId = conditions
      .contractIdFor(id)
      .orElseThrow(() -> missing("Condition"));
    RentalAgreement rental = contract(contractId, true);
    ConditionSnapshot record = conditions
      .lock(id)
      .orElseThrow(() -> missing("Condition"));
    editableStage(rental, record.recordStageCd);
    require(
      "N".equals(record.confirmedYn),
      "Confirmed records are immutable; create a new revision"
    );
    return record;
  }

  private void editableStage(RentalAgreement rental, String stage) {
    requireState(rental, "INTAKE".equals(stage) ? "REQUESTED" : "RETURNED");
    require(
      inspections.findByContractId(rental.id).isEmpty(),
      "Inspection records are fixed; further edits are blocked"
    );
  }

  private ConditionSnapshot condition(Long id) {
    return conditions.findById(id).orElseThrow(() -> missing("Condition"));
  }

  private RentalAgreement contract(Long id, boolean lock) {
    RentalAgreement rental = (
      lock ? rentals.lock(id) : rentals.findById(id)
    ).orElseThrow(() -> missing("Rental"));
    var user = actor();
    if (
      user.role() == MemberRole.CUSTOMER &&
      !rental.memberId.equals(user.memberId())
    ) throw new AccessDeniedException("This rental belongs to another member");
    return rental;
  }

  private boolean reserved(Long itemId) {
    return rentals.existsByItemIdAndContractStatusCdIn(
      itemId,
      List.of("REQUESTED", "RENTED", "RETURNED")
    );
  }

  private void code(String group, String value) {
    Integer count = jdbc.queryForObject(
      "SELECT COUNT(*) FROM common_code c JOIN common_code_group g ON g.code_group_id=c.code_group_id WHERE c.code_group_id=? AND c.code_value=? AND c.use_yn='Y' AND g.use_yn='Y'",
      Integer.class,
      group,
      value
    );
    if (count == null || count != 1) throw new IllegalArgumentException(
      "Invalid " + group + " code"
    );
  }

  private AuthenticatedMember actor() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (
      auth == null ||
      !(auth.getPrincipal() instanceof AuthenticatedMember member)
    ) throw new AccessDeniedException("Authentication required");
    return member;
  }

  private void staff() {
    if (actor().role() == MemberRole.CUSTOMER) throw new AccessDeniedException(
      "Inspector role required"
    );
  }

  private void admin() {
    if (actor().role() != MemberRole.ADMIN) throw new AccessDeniedException(
      "Administrator role required"
    );
  }

  private void requireState(RentalAgreement rental, String state) {
    require(
      state.equals(rental.contractStatusCd),
      "Expected contract state " + state
    );
  }

  private static void require(boolean valid, String message) {
    if (!valid) throw new IllegalStateException(message);
  }

  private static ResourceNotFoundException missing(String name) {
    return new ResourceNotFoundException(name + " not found");
  }

  private String validateImage(byte[] bytes) {
    try (
      var input = ImageIO.createImageInputStream(
        new ByteArrayInputStream(bytes)
      )
    ) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw new IllegalArgumentException(
        "Only valid PNG/JPEG images are accepted"
      );
      var reader = readers.next();
      try {
        reader.setInput(input);
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (
          !List.of("png", "jpeg", "jpg").contains(format)
        ) throw new IllegalArgumentException(
          "Only PNG/JPEG images are accepted"
        );
        if (
          (long) reader.getWidth(0) * reader.getHeight(0) > 20_000_000
        ) throw new IllegalArgumentException(
          "Image dimensions exceed 20 megapixels"
        );
        reader.read(0);
        return format.equals("png") ? "image/png" : "image/jpeg";
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid image", e);
    }
  }

  private Map<String, Object> businessView(Business b) {
    return map(
      "id",
      b.id,
      "businessNo",
      b.businessNo,
      "businessName",
      b.businessName,
      "businessStatusCd",
      b.businessStatusCd,
      "contactEmail",
      b.contactEmail
    );
  }

  private Map<String, Object> itemView(RentalItem i) {
    return map(
      "id",
      i.id,
      "businessId",
      i.businessId,
      "assetCode",
      i.assetCode,
      "itemName",
      i.itemName,
      "categoryCd",
      i.categoryCd,
      "itemStatusCd",
      i.itemStatusCd,
      "description",
      i.description,
      "requestable",
      "AVAILABLE".equals(i.itemStatusCd) && !reserved(i.id)
    );
  }

  private Map<String, Object> rentalView(RentalAgreement r) {
    Map<String, Object> view = map(
      "id",
      r.id,
      "contractNo",
      r.contractNo,
      "memberId",
      r.memberId,
      "itemId",
      r.itemId,
      "contractStatusCd",
      r.contractStatusCd,
      "requestedAt",
      r.requestedAt,
      "rentalStartAt",
      r.rentalStartAt,
      "rentalEndAt",
      r.rentalEndAt,
      "actualReturnAt",
      r.actualReturnAt
    );
    view.put("legacySource", legacyView("rental_contract", r.id));
    return view;
  }

  private Map<String, Object> conditionView(ConditionSnapshot c) {
    Map<String, Object> view = map(
      "id",
      c.id,
      "contractId",
      c.contractId,
      "recordStageCd",
      c.recordStageCd,
      "recordRevisionNo",
      c.recordRevisionNo,
      "recordNote",
      c.recordNote,
      "confirmed",
      "Y".equals(c.confirmedYn),
      "recordedAt",
      c.recordedAt,
      "photos",
      photos
        .findAllByConditionIdOrderByPhotoSeqAsc(c.id)
        .stream()
        .map(this::photoView)
        .toList(),
      "damages",
      damages
        .findAllByConditionIdOrderByIdAsc(c.id)
        .stream()
        .map(this::damageView)
        .toList()
    );
    view.put("legacySource", legacyView("condition_record", c.id));
    return view;
  }

  private Map<String, Object> photoView(ConditionPhoto p) {
    Map<String, Object> view = map(
      "id",
      p.id,
      "conditionId",
      p.conditionId,
      "photoSeq",
      p.photoSeq,
      "originalFileName",
      p.originalFileName,
      "mimeType",
      p.mimeType,
      "fileSize",
      p.fileSize,
      "fileHash",
      p.fileHash,
      "captureAngleCd",
      p.captureAngleCd,
      "capturedAt",
      p.capturedAt,
      "contentUrl",
      "/api/v2/photos/" + p.id + "/content",
      "analyses",
      analyses
        .findAllByPhotoIdOrderByIdAsc(p.id)
        .stream()
        .map(this::analysisView)
        .toList()
    );
    view.put("legacySource", legacyView("condition_photo", p.id));
    return view;
  }

  private Map<String, Object> damageView(DamageObservation d) {
    Map<String, Object> view = map(
      "id",
      d.id,
      "conditionId",
      d.conditionId,
      "damageLocationCd",
      d.damageLocationCd,
      "damageTypeCd",
      d.damageTypeCd,
      "severityCd",
      d.severityCd,
      "damageDescription",
      d.damageDescription,
      "evidence",
      evidence
        .findAllByDamageIdOrderByIdAsc(d.id)
        .stream()
        .map(this::evidenceView)
        .toList()
    );
    view.put("legacySource", legacyView("damage_observation", d.id));
    return view;
  }

  private Map<String, Object> evidenceView(DamagePhoto e) {
    return map(
      "id",
      e.id,
      "photoId",
      e.photoId,
      "box",
      box(e.bboxXRatio, e.bboxYRatio, e.bboxWidthRatio, e.bboxHeightRatio)
    );
  }

  private Map<String, Object> analysisView(ImageAnalysisResult a) {
    return map(
      "id",
      a.id,
      "analysisRunId",
      a.analysisRunId,
      "detectionSeq",
      a.detectionSeq,
      "modelName",
      a.modelName,
      "modelVersion",
      a.modelVersion,
      "predictedDamageTypeCd",
      a.predictedDamageTypeCd,
      "confidenceScore",
      a.confidenceScore,
      "box",
      box(a.bboxXRatio, a.bboxYRatio, a.bboxWidthRatio, a.bboxHeightRatio),
      "analyzedAt",
      a.analyzedAt
    );
  }

  private Map<String, Object> inspectionView(ReturnInspection i) {
    Map<String, Object> view = map(
      "id",
      i.id,
      "contractId",
      i.contractId,
      "intakeConditionId",
      i.intakeConditionId,
      "returnConditionId",
      i.returnConditionId,
      "inspectorMemberId",
      i.inspectorMemberId,
      "inspectionStatusCd",
      i.inspectionStatusCd,
      "overallResultCd",
      i.overallResultCd,
      "inspectionNote",
      i.inspectionNote,
      "inspectedAt",
      i.inspectedAt,
      "assessments",
      assessments
        .findAllByInspectionIdOrderByIdAsc(i.id)
        .stream()
        .map(this::assessmentView)
        .toList()
    );
    view.put("legacySource", legacyView("return_inspection", i.id));
    return view;
  }

  private Map<String, Object> assessmentView(DamageAssessment a) {
    return map(
      "id",
      a.id,
      "returnDamageId",
      a.returnDamageId,
      "intakeDamageId",
      a.intakeDamageId,
      "assessmentResultCd",
      a.assessmentResultCd,
      "assessmentReason",
      a.assessmentReason
    );
  }

  private Map<String, Object> box(
    BigDecimal x,
    BigDecimal y,
    BigDecimal width,
    BigDecimal height
  ) {
    return x == null
      ? null
      : map("x", x, "y", y, "width", width, "height", height);
  }

  private Map<String, Object> legacyView(String table, Long id) {
    String source;
    String columns;
    switch (table) {
      case "rental_contract" -> {
        source = "legacy_rental_contract";
        columns =
          "s.id AS legacyId,s.contract_code AS originalContractNo,s.status AS originalStatus,s.approved_at AS approvedAt,s.picked_up_at AS pickedUpAt,s.completed_at AS completedAt";
      }
      case "condition_record" -> {
        source = "legacy_condition_record";
        columns =
          "s.id AS legacyId,s.record_type AS originalStage,s.recorded_by_member_id AS recordedByLegacyMemberId";
      }
      case "condition_photo" -> {
        source = "condition_image";
        columns =
          "s.id AS legacyId,s.viewpoint AS originalViewpoint,s.original_filename AS originalFilename";
      }
      case "damage_observation" -> {
        source = "damage_record";
        columns =
          "s.id AS legacyId,s.damage_type AS originalType,s.damage_location AS originalLocation,s.comparison_result AS originalComparison";
      }
      default -> {
        source = "legacy_return_inspection";
        columns =
          "s.id AS legacyId,s.result AS originalResult,s.inspected_at AS originalInspectedAt";
      }
    }
    var found = jdbc.queryForList(
      "SELECT " +
        columns +
        " FROM legacy_entity_map m JOIN " +
        source +
        " s ON s.id=m.source_id WHERE m.source_table=? AND m.target_table=? AND m.target_id=?",
      source,
      table,
      id
    );
    return found.isEmpty() ? null : found.getFirst();
  }

  private static Map<String, Object> map(Object... values) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) result.put(
      (String) values[i],
      values[i + 1]
    );
    return result;
  }
}
