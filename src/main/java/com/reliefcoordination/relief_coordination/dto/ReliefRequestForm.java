package com.reliefcoordination.relief_coordination.dto;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReliefRequestForm {

    @NotBlank(message = "Địa điểm không được để trống")
    private String location;

    @NotNull(message = "Loại nhu cầu không được để trống")
    private NeedType needType;

    @NotNull(message = "Số người không được để trống")
    @Min(value = 1, message = "Số người phải ít nhất là 1")
    private Integer numberOfPeople;

    @NotNull(message = "Mức độ khẩn cấp không được để trống")
    private UrgencyLevel urgencyLevel;

    private String description;

    /** GPS — null nếu Victim không cung cấp */
    private Double latitude;

    private Double longitude;

    public static ReliefRequestForm from(ReliefRequest request) {
        ReliefRequestForm form = new ReliefRequestForm();
        form.setLocation(request.getLocation());
        form.setNeedType(request.getNeedType());
        form.setNumberOfPeople(request.getNumberOfPeople());
        form.setUrgencyLevel(request.getUrgencyLevel());
        form.setDescription(request.getDescription());
        form.setLatitude(request.getLatitude());
        form.setLongitude(request.getLongitude());
        return form;
    }
}
