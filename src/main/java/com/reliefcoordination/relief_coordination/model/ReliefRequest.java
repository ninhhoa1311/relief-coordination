package com.reliefcoordination.relief_coordination.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "relief_requests")
@Getter
@Setter
public class ReliefRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "victim_id")
    private Victim victim;

    @ManyToOne
    @JoinColumn(name = "rescuer_id")
    private Rescuer rescuer;

    @Column(nullable = false)
    private String location;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NeedType needType;

    @Column(nullable = false)
    private Integer numberOfPeople;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyLevel urgencyLevel;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private RequestStatus status = RequestStatus.SENT;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    private Long version;

    /** Đổi trạng thái, chỉ cho phép bước đi hợp lệ. Kiểm tra AI được đổi nằm ở Service. */
    public void changeStatus(RequestStatus next) {
        Objects.requireNonNull(next, "Trạng thái mới không được null");
        if (!this.status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Không thể chuyển từ trạng thái " + this.status + " sang " + next);
        }
        this.status = next;
    }

    /** Rescuer nhận yêu cầu: kiểm tra bước đi trước, rồi mới gán rescuer. */
    public void accept(Rescuer rescuer) {
        Objects.requireNonNull(rescuer, "Rescuer không được null");
        changeStatus(RequestStatus.ACCEPTED);
        this.rescuer = rescuer;
    }

    /** Victim còn được sửa/hủy khi yêu cầu chưa ai nhận. */
    public boolean isEditable() {
        return this.status == RequestStatus.SENT;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    /** Link Google Maps theo SPEC; null nếu Victim không cung cấp GPS. */
    public String getMapUrl() {
        if (!hasLocation()) {
            return null;
        }
        return "https://www.google.com/maps?q=" + latitude + "," + longitude;
    }

    @PreUpdate
    public void beforeUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}