package com.reliefcoordination.relief_coordination.repository;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.model.Victim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReliefRequestRepository extends JpaRepository<ReliefRequest, Long> {
    List<ReliefRequest> findByStatus(RequestStatus status);
    List<ReliefRequest> findByVictim(Victim victim);
    List<ReliefRequest> findByRescuer(Rescuer rescuer);
    long countByStatus(RequestStatus status);

    // Kiểm tra chống spam SOS: cùng SĐT đã có yêu cầu chưa kết thúc (chưa CONFIRMED/CANCELLED)
    boolean existsByGuestPhoneAndStatusNotIn(String guestPhone, java.util.Collection<RequestStatus> statuses);

    // Thống kê số yêu cầu theo từng trạng thái của 1 Victim (GROUP BY)
    @Query("SELECT r.status, COUNT(r) FROM ReliefRequest r WHERE r.victim.id = :victimId GROUP BY r.status")
    List<Object[]> countRequestsByStatusForVictim(@Param("victimId") Long victimId);

    // Phân trang danh sách yêu cầu của 1 Victim
    Page<ReliefRequest> findByVictimId(Long victimId, Pageable pageable);

    // Phân trang danh sách yêu cầu của 1 Victim có lọc theo trạng thái
    Page<ReliefRequest> findByVictimIdAndStatus(Long victimId, RequestStatus status, Pageable pageable);

    // Thống kê toàn hệ thống theo trạng thái (GROUP BY một câu truy vấn)
    @Query("SELECT r.status, COUNT(r), SUM(COALESCE(r.numberOfPeople, 0)) FROM ReliefRequest r GROUP BY r.status")
    List<Object[]> countRequestsAndPeopleByStatus();

    // Query lọc động và phân trang cho Admin:
    // Sắp xếp: Yêu cầu chưa xong (SENT, ACCEPTED, PROCESSING) lên trước, trong đó CRITICAL > HIGH > MEDIUM > LOW, cùng mức thì mới nhất trước.
    // Yêu cầu đã xong hoặc hủy xuống dưới, mới nhất trước.
    @Query(
        value = "SELECT r FROM ReliefRequest r " +
                "LEFT JOIN r.victim v " +
                "WHERE (:status IS NULL OR r.status = :status) " +
                "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                "  AND (:keyword IS NULL OR :keyword = '' OR " +
                "       LOWER(r.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                "       (r.guestName IS NOT NULL AND LOWER(r.guestName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
                "       (r.guestPhone IS NOT NULL AND r.guestPhone LIKE CONCAT('%', :keyword, '%')) OR " +
                "       (v IS NOT NULL AND (" +
                "           LOWER(v.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                "           v.phoneNumber LIKE CONCAT('%', :keyword, '%')" +
                "       ))) " +
                "ORDER BY " +
                "  CASE WHEN r.status IN (com.reliefcoordination.relief_coordination.model.RequestStatus.SENT, " +
                "                         com.reliefcoordination.relief_coordination.model.RequestStatus.ACCEPTED, " +
                "                         com.reliefcoordination.relief_coordination.model.RequestStatus.PROCESSING) " +
                "       THEN 0 ELSE 1 END ASC, " +
                "  CASE WHEN r.status IN (com.reliefcoordination.relief_coordination.model.RequestStatus.SENT, " +
                "                         com.reliefcoordination.relief_coordination.model.RequestStatus.ACCEPTED, " +
                "                         com.reliefcoordination.relief_coordination.model.RequestStatus.PROCESSING) " +
                "       THEN (CASE r.urgencyLevel " +
                "               WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.CRITICAL THEN 0 " +
                "               WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.HIGH THEN 1 " +
                "               WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.MEDIUM THEN 2 " +
                "               WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.LOW THEN 3 " +
                "               ELSE 4 END) " +
                "       ELSE 0 END ASC, " +
                "  r.createdAt DESC, " +
                "  r.id DESC",
        countQuery = "SELECT COUNT(r) FROM ReliefRequest r " +
                     "LEFT JOIN r.victim v " +
                     "WHERE (:status IS NULL OR r.status = :status) " +
                     "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                     "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                     "  AND (:keyword IS NULL OR :keyword = '' OR " +
                     "       LOWER(r.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "       (r.guestName IS NOT NULL AND LOWER(r.guestName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
                     "       (r.guestPhone IS NOT NULL AND r.guestPhone LIKE CONCAT('%', :keyword, '%')) OR " +
                     "       (v IS NOT NULL AND (" +
                     "           LOWER(v.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "           v.phoneNumber LIKE CONCAT('%', :keyword, '%')" +
                     "       )))"
    )
    Page<ReliefRequest> timKiemYeuCauAdmin(
            @Param("status") RequestStatus status,
            @Param("urgencyLevel") com.reliefcoordination.relief_coordination.model.UrgencyLevel urgencyLevel,
            @Param("needType") com.reliefcoordination.relief_coordination.model.NeedType needType,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // =======================================================================
    // Yêu cầu chờ (Rescuer) - status = SENT
    // =======================================================================

    @Query(
        value = "SELECT r FROM ReliefRequest r " +
                "LEFT JOIN r.victim v " +
                "WHERE r.status = com.reliefcoordination.relief_coordination.model.RequestStatus.SENT " +
                "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                "ORDER BY " +
                "  CASE r.urgencyLevel " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.CRITICAL THEN 0 " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.HIGH THEN 1 " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.MEDIUM THEN 2 " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.LOW THEN 3 " +
                "    ELSE 4 END ASC, " +
                "  r.createdAt ASC, " +
                "  r.id ASC",
        countQuery = "SELECT COUNT(r) FROM ReliefRequest r " +
                     "WHERE r.status = com.reliefcoordination.relief_coordination.model.RequestStatus.SENT " +
                     "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                     "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                     "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel)"
    )
    Page<ReliefRequest> timKiemYeuCauChoUrgencyDesc(
            @Param("location") String location,
            @Param("needType") NeedType needType,
            @Param("urgencyLevel") UrgencyLevel urgencyLevel,
            Pageable pageable
    );

    @Query(
        value = "SELECT r FROM ReliefRequest r " +
                "LEFT JOIN r.victim v " +
                "WHERE r.status = com.reliefcoordination.relief_coordination.model.RequestStatus.SENT " +
                "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                "ORDER BY " +
                "  CASE r.urgencyLevel " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.LOW THEN 0 " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.MEDIUM THEN 1 " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.HIGH THEN 2 " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.CRITICAL THEN 3 " +
                "    ELSE 4 END ASC, " +
                "  r.createdAt ASC, " +
                "  r.id ASC",
        countQuery = "SELECT COUNT(r) FROM ReliefRequest r " +
                     "WHERE r.status = com.reliefcoordination.relief_coordination.model.RequestStatus.SENT " +
                     "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                     "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                     "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel)"
    )
    Page<ReliefRequest> timKiemYeuCauChoUrgencyAsc(
            @Param("location") String location,
            @Param("needType") NeedType needType,
            @Param("urgencyLevel") UrgencyLevel urgencyLevel,
            Pageable pageable
    );

    @Query(
        value = "SELECT r FROM ReliefRequest r " +
                "LEFT JOIN r.victim v " +
                "WHERE r.status = com.reliefcoordination.relief_coordination.model.RequestStatus.SENT " +
                "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                "ORDER BY r.createdAt DESC, r.id DESC",
        countQuery = "SELECT COUNT(r) FROM ReliefRequest r " +
                     "WHERE r.status = com.reliefcoordination.relief_coordination.model.RequestStatus.SENT " +
                     "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                     "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                     "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel)"
    )
    Page<ReliefRequest> timKiemYeuCauChoCreatedDesc(
            @Param("location") String location,
            @Param("needType") NeedType needType,
            @Param("urgencyLevel") UrgencyLevel urgencyLevel,
            Pageable pageable
    );

    @Query(
        value = "SELECT r FROM ReliefRequest r " +
                "LEFT JOIN r.victim v " +
                "WHERE r.status = com.reliefcoordination.relief_coordination.model.RequestStatus.SENT " +
                "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                "ORDER BY r.createdAt ASC, r.id ASC",
        countQuery = "SELECT COUNT(r) FROM ReliefRequest r " +
                     "WHERE r.status = com.reliefcoordination.relief_coordination.model.RequestStatus.SENT " +
                     "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                     "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                     "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel)"
    )
    Page<ReliefRequest> timKiemYeuCauChoCreatedAsc(
            @Param("location") String location,
            @Param("needType") NeedType needType,
            @Param("urgencyLevel") UrgencyLevel urgencyLevel,
            Pageable pageable
    );

    // =======================================================================
    // Nhiệm vụ của tôi (Rescuer) - r.rescuer = :rescuer
    // =======================================================================

    @Query(
        value = "SELECT r FROM ReliefRequest r " +
                "LEFT JOIN r.victim v " +
                "WHERE r.rescuer = :rescuer " +
                "  AND (:status IS NULL OR r.status = :status) " +
                "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                "ORDER BY " +
                "  CASE WHEN r.status IN (com.reliefcoordination.relief_coordination.model.RequestStatus.ACCEPTED, " +
                "                         com.reliefcoordination.relief_coordination.model.RequestStatus.PROCESSING) " +
                "       THEN 0 ELSE 1 END ASC, " +
                "  CASE WHEN r.status IN (com.reliefcoordination.relief_coordination.model.RequestStatus.ACCEPTED, " +
                "                         com.reliefcoordination.relief_coordination.model.RequestStatus.PROCESSING) " +
                "       THEN (CASE r.urgencyLevel " +
                "               WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.CRITICAL THEN 0 " +
                "               WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.HIGH THEN 1 " +
                "               WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.MEDIUM THEN 2 " +
                "               WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.LOW THEN 3 " +
                "               ELSE 4 END) " +
                "       ELSE 0 END ASC, " +
                "  CASE WHEN r.status IN (com.reliefcoordination.relief_coordination.model.RequestStatus.ACCEPTED, " +
                "                         com.reliefcoordination.relief_coordination.model.RequestStatus.PROCESSING) " +
                "       THEN r.updatedAt ELSE NULL END ASC, " +
                "  CASE WHEN r.status IN (com.reliefcoordination.relief_coordination.model.RequestStatus.ACCEPTED, " +
                "                         com.reliefcoordination.relief_coordination.model.RequestStatus.PROCESSING) " +
                "       THEN r.id ELSE NULL END ASC, " +
                "  r.updatedAt DESC, " +
                "  r.id DESC",
        countQuery = "SELECT COUNT(r) FROM ReliefRequest r " +
                     "WHERE r.rescuer = :rescuer " +
                     "  AND (:status IS NULL OR r.status = :status) " +
                     "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                     "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                     "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%')))"
    )
    Page<ReliefRequest> timKiemNhiemVuDefault(
            @Param("rescuer") Rescuer rescuer,
            @Param("status") RequestStatus status,
            @Param("needType") NeedType needType,
            @Param("urgencyLevel") UrgencyLevel urgencyLevel,
            @Param("location") String location,
            Pageable pageable
    );

    @Query(
        value = "SELECT r FROM ReliefRequest r " +
                "LEFT JOIN r.victim v " +
                "WHERE r.rescuer = :rescuer " +
                "  AND (:status IS NULL OR r.status = :status) " +
                "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                "ORDER BY " +
                "  CASE r.urgencyLevel " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.LOW THEN 0 " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.MEDIUM THEN 1 " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.HIGH THEN 2 " +
                "    WHEN com.reliefcoordination.relief_coordination.model.UrgencyLevel.CRITICAL THEN 3 " +
                "    ELSE 4 END ASC, " +
                "  r.updatedAt ASC, " +
                "  r.id ASC",
        countQuery = "SELECT COUNT(r) FROM ReliefRequest r " +
                     "WHERE r.rescuer = :rescuer " +
                     "  AND (:status IS NULL OR r.status = :status) " +
                     "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                     "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                     "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%')))"
    )
    Page<ReliefRequest> timKiemNhiemVuUrgencyAsc(
            @Param("rescuer") Rescuer rescuer,
            @Param("status") RequestStatus status,
            @Param("needType") NeedType needType,
            @Param("urgencyLevel") UrgencyLevel urgencyLevel,
            @Param("location") String location,
            Pageable pageable
    );

    @Query(
        value = "SELECT r FROM ReliefRequest r " +
                "LEFT JOIN r.victim v " +
                "WHERE r.rescuer = :rescuer " +
                "  AND (:status IS NULL OR r.status = :status) " +
                "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                "ORDER BY r.updatedAt DESC, r.id DESC",
        countQuery = "SELECT COUNT(r) FROM ReliefRequest r " +
                     "WHERE r.rescuer = :rescuer " +
                     "  AND (:status IS NULL OR r.status = :status) " +
                     "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                     "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                     "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%')))"
    )
    Page<ReliefRequest> timKiemNhiemVuAcceptedDesc(
            @Param("rescuer") Rescuer rescuer,
            @Param("status") RequestStatus status,
            @Param("needType") NeedType needType,
            @Param("urgencyLevel") UrgencyLevel urgencyLevel,
            @Param("location") String location,
            Pageable pageable
    );

    @Query(
        value = "SELECT r FROM ReliefRequest r " +
                "LEFT JOIN r.victim v " +
                "WHERE r.rescuer = :rescuer " +
                "  AND (:status IS NULL OR r.status = :status) " +
                "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
                "ORDER BY r.updatedAt ASC, r.id ASC",
        countQuery = "SELECT COUNT(r) FROM ReliefRequest r " +
                     "WHERE r.rescuer = :rescuer " +
                     "  AND (:status IS NULL OR r.status = :status) " +
                     "  AND (:needType IS NULL OR :needType MEMBER OF r.needTypes) " +
                     "  AND (:urgencyLevel IS NULL OR r.urgencyLevel = :urgencyLevel) " +
                     "  AND (:location IS NULL OR :location = '' OR LOWER(r.location) LIKE LOWER(CONCAT('%', :location, '%')))"
    )
    Page<ReliefRequest> timKiemNhiemVuAcceptedAsc(
            @Param("rescuer") Rescuer rescuer,
            @Param("status") RequestStatus status,
            @Param("needType") NeedType needType,
            @Param("urgencyLevel") UrgencyLevel urgencyLevel,
            @Param("location") String location,
            Pageable pageable
    );
}