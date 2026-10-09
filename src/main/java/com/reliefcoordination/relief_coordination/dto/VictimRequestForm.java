package com.reliefcoordination.relief_coordination.dto;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Form tạo/sửa yêu cầu cứu trợ của Victim (không bind thẳng entity). */
@Getter
@Setter
public class VictimRequestForm {

    @NotBlank(message = "Vui lòng nhập địa điểm")
    @Size(max = 255, message = "Địa điểm tối đa 255 ký tự")
    private String location;

    /** GPS không bắt buộc; được điền bởi nút "Lấy vị trí" ở trình duyệt. */
    private Double latitude;
    private Double longitude;

    @NotNull(message = "Vui lòng chọn loại nhu cầu")
    private NeedType needType;

    @NotNull(message = "Vui lòng nhập số người cần hỗ trợ")
    @Min(value = 1, message = "Số người phải từ 1 trở lên")
    @Max(value = 1000, message = "Số người tối đa 1000")
    private Integer numberOfPeople;

    @NotNull(message = "Vui lòng chọn mức độ khẩn cấp")
    private UrgencyLevel urgencyLevel;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;

    public static VictimRequestForm from(ReliefRequest request) {
        VictimRequestForm form = new VictimRequestForm();
        form.setLocation(request.getLocation());
        form.setLatitude(request.getLatitude());
        form.setLongitude(request.getLongitude());
        form.setNeedType(request.getNeedType());
        form.setNumberOfPeople(request.getNumberOfPeople());
        form.setUrgencyLevel(request.getUrgencyLevel());
        form.setDescription(request.getDescription());
        return form;
    }
}
