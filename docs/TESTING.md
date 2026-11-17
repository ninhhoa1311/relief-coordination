# TESTING — Kiểm thử Cần cứu trợ

**Điều hướng:** [README](../README.md) · [SPEC](SPEC.md) · [Kiến trúc](ARCHITECTURE.md) · [OOP Design](OOP-DESIGN.md) · [Database](DATABASE.md) · [Workflow](WORKFLOW.md) · [Routes](ROUTES.md) · [User Guide](USER-GUIDE.md)

---

## 1. Các class test hiện có

Tất cả test nằm trong `src/test/java/com/reliefcoordination/relief_coordination/service/`.

| Class | Phạm vi kiểm tra |
|---|---|
| `AdminServiceTest` | Khóa/mở khóa tài khoản, hủy yêu cầu, duyệt Rescuer, thống kê, lọc/sắp xếp tìm kiếm |
| `VictimRequestServiceTest` | Tạo yêu cầu, phân quyền (Victim A không xem/sửa được của Victim B), sửa/hủy chỉ khi SENT, xác nhận chỉ khi COMPLETED |
| `RescuerRequestServiceTest` | Nhận nhiệm vụ (lần 2 ném lỗi), sắp xếp, lọc theo location/needType/urgencyLevel, SOS khách vãng lai, tham số không hợp lệ fallback |
| `RescuerTaskServiceTest` | Phân quyền ownership, cập nhật trạng thái tuần tự, lỗi sau COMPLETED, lọc/sắp xếp nhiệm vụ, SOS khách vãng lai |
| `PublicStatsServiceTest` | Thống kê công khai chính xác theo trạng thái |

---

## 2. Cách chạy test

```bash
# macOS/Linux
./mvnw test

# Windows PowerShell
.\mvnw.cmd test
```

> **Lưu ý:** Test dùng `@SpringBootTest` + `@Transactional` — cần file `.env` với thông tin kết nối database **thật** (Neon branch của bạn). Mỗi test tự rollback sau khi chạy xong, không để lại dữ liệu thừa.

Chạy từng class riêng:
```bash
.\mvnw.cmd test -Dtest=AdminServiceTest
.\mvnw.cmd test -Dtest=VictimRequestServiceTest
```

---

## 3. Danh sách kịch bản kiểm thử tự động

### AdminServiceTest (14 test)

| Test | Kịch bản |
|---|---|
| `toggleKhoa_victim_khoa_enabled_chuyen_thanh_false` | Khóa Victim: enabled → false |
| `toggleKhoa_victim_mo_khoa_enabled_chuyen_thanh_true` | Mở khóa Victim: false → true |
| `toggleKhoa_loai_admin_phai_nem_IllegalArgumentException` | Loại "admin" bị từ chối |
| `toggleKhoa_loai_bat_hop_le_phai_nem_IllegalArgumentException` | Loại bất hợp lệ bị từ chối |
| `huyYeuCau_request_da_CONFIRMED_phai_nem_IllegalStateException` | Không hủy được CONFIRMED |
| `huyYeuCau_request_da_CANCELLED_phai_nem_IllegalStateException` | Không hủy được CANCELLED |
| `huyYeuCau_request_SENT_thanh_cong` | Hủy yêu cầu SENT thành công |
| `duyetRescuer_thanhCong_chuyen_enabled_thanh_true` | Duyệt 1 Rescuer |
| `duyetTatCaRescuer_thanhCong_va_khong_mo_khoa_rescuer_bi_khoa` | Duyệt tất cả, không ảnh hưởng tài khoản bị khóa |
| `timVictimTheoId_tonTai_traVeVictim` | Tìm Victim theo ID thành công |
| `timVictimTheoId_khongTonTai_throwsResourceNotFoundException` | ID không tồn tại → 404 |
| `timVictimTheoId_idLaRescuer_throwsResourceNotFoundException` | ID là Rescuer → không tìm thấy Victim |
| `thongKeYeuCauCuaVictim_chinhXac` | Thống kê đúng theo trạng thái |
| `danhSachYeuCauCuaVictim_phanTrangVaLocStatus` | Phân trang và lọc theo status |
| `thongKe_toanHeThong_dungGroupByQuery` | Thống kê toàn hệ thống |
| `timKiemYeuCauAdmin_locTheoCacTieuChi` | Lọc theo urgencyLevel, needType, keyword (tên victim, SĐT khách, địa điểm) |
| `timKiemYeuCauAdmin_thuTuSapXepChuaXongVaKhanCapLenDau` | Sắp xếp: CRITICAL trước, CANCELLED sau |

### VictimRequestServiceTest (8 test)

| Test | Kịch bản |
|---|---|
| `taoYeuCau_thanhCong_statusMacDinhLaSent` | Tạo yêu cầu mới, status = SENT |
| `victimAKhongXemDuocRequestCuaVictimB_throwsAccessDeniedException` | Phân quyền xem |
| `victimAKhongSuaDuocRequestCuaVictimB_throwsAccessDeniedException` | Phân quyền sửa |
| `khongSuaDuocKhiStatusKhacSent_throwsIllegalStateException` | Chỉ sửa khi SENT |
| `suaYeuCau_thanhCongKhiStatusSent` | Sửa thành công |
| `huyYeuCau_thanhCongKhiStatusSent` | Hủy thành công |
| `xacNhanDaNhan_thanhCongKhiStatusCompleted` | Xác nhận thành công |
| `xacNhanDaNhan_thatBaiKhiStatusKhacCompleted` | Chỉ xác nhận khi COMPLETED |

### RescuerRequestServiceTest (10 test)

| Test | Kịch bản |
|---|---|
| `nhanNhiemVu_lanDau_thanhCong_lanHai_throwsIllegalStateException` | Race condition: lần 2 ném lỗi |
| `danhSachDangCho_sapXepThoiGian_cuNhatTruoc` | Sắp xếp theo thời gian |
| `timKiem_theoLocation_khongPhanBietHoaThuong` | Tìm kiếm không phân biệt hoa thường |
| `timKiem_khongCoKetQua_traVeListRong` | Không có kết quả → list rỗng |
| `timKiemYeuCauCho_sapXepMacDinh_moiNhatTruoc` | Sắp xếp mặc định: mới nhất trước |
| `timKiemYeuCauCho_sortKhongHopLe_fallbackVeCreatedDesc` | Sort không hợp lệ → fallback |
| `timKiemYeuCauCho_sapXepThoiGianTao` | Sort created_desc và created_asc |
| `timKiemYeuCauCho_locTheoNeedType` | Lọc theo loại nhu cầu |
| `timKiemYeuCauCho_sosKhach_hoatDongBinhThuong` | SOS khách vãng lai (victim=null) xuất hiện trong danh sách |
| `timKiemYeuCauCho_thamSoKhongHopLe_fallbackKhongLoi` | Tham số không hợp lệ không gây lỗi 500 |

### RescuerTaskServiceTest (11 test)

| Test | Kịch bản |
|---|---|
| `rescuerAKhongCapNhatDuocNhiemVuCuaRescuerB_throwsAccessDeniedException` | Phân quyền ownership |
| `capNhatTrangThaiTiep_khiStatusLaSent_throwsIllegalStateException` | SENT không có bước tiếp theo |
| `capNhatTrangThaiTiep_haiLanLienTiep_chuyenDungThuTu` | ACCEPTED→PROCESSING→COMPLETED |
| `xemChiTiet_rescuerSai_throwsAccessDeniedException` | Phân quyền xem chi tiết |
| `capNhatTrangThaiTiep_lanBaSauCompleted_throwsIllegalStateException` | COMPLETED không có bước tiếp |
| `timKiemNhiemVu_chiTraNhiemVuCuaDungRescuer` | Chỉ trả nhiệm vụ của đúng Rescuer |
| `timKiemNhiemVu_locTheoTrangThai` | Lọc theo trạng thái COMPLETED |
| `timKiemNhiemVu_sapXepMacDinh_moiNhatTruoc` | Sắp xếp mặc định |
| `timKiemNhiemVu_sapXepThoiGianNhan` | Sort accepted_desc và accepted_asc |
| `timKiemNhiemVu_sosKhach_hoatDongBinhThuong` | Nhiệm vụ SOS khách vãng lai |
| `timKiemNhiemVu_thamSoKhongHopLe_fallbackKhongLoi` | Tham số không hợp lệ → không lỗi |

### PublicStatsServiceTest (1 test)

| Test | Kịch bản |
|---|---|
| `getPublicStats_khongLoi_vaTraVeSoLieuChinhXac` | Thống kê công khai chính xác (totalRequests, completedRequests, totalPeopleSupported) |

---

## 4. Kịch bản kiểm thử thủ công quan trọng

### 4.1. SOS khẩn cấp (không đăng nhập)

1. Truy cập `/emergency` khi đã đăng xuất.
2. Gửi SOS với SĐT mới → kiểm tra redirect thành công.
3. Gửi SOS lại cùng SĐT đó → kiểm tra thông báo "Đã ghi nhận trước đó".
4. Đăng nhập bằng tài khoản Rescuer → vào `/rescuer/home` → kiểm tra yêu cầu SOS xuất hiện với mức khẩn cấp CRITICAL và nổi lên đầu.

### 4.2. Duyệt Rescuer

1. Đăng ký tài khoản Rescuer mới.
2. Thử đăng nhập ngay → kiểm tra bị từ chối.
3. Đăng nhập bằng Admin → `/admin/users?tab=rescuer` → kiểm tra Rescuer mới ở trạng thái "Chờ duyệt".
4. Bấm "Duyệt" → kiểm tra flash message thành công.
5. Đăng xuất Admin → đăng nhập Rescuer mới → kiểm tra vào được `/rescuer/home`.

### 4.3. Lọc và sắp xếp

**Admin:**
1. `/admin/home` → lọc theo từng tiêu chí (trạng thái, mức khẩn, loại nhu cầu).
2. Tìm kiếm từ khóa → kiểm tra kết quả lọc đúng (tên Victim, SĐT khách, địa điểm).
3. Kiểm tra thứ tự: yêu cầu CRITICAL+SENT phải đứng đầu, yêu cầu CANCELLED xuống cuối.

**Rescuer:**
1. `/rescuer/home` → lọc địa điểm (không phân biệt hoa thường).
2. Sắp xếp "Cũ nhất trước" → kiểm tra thứ tự đảo lại.

### 4.4. Phân quyền

1. Đăng nhập Victim A → thử truy cập `/victim/requests/{id-của-Victim-B}` → kiểm tra trả về 403 hoặc redirect với thông báo lỗi.
2. Đăng nhập Rescuer A → thử truy cập `/rescuer/tasks/{id-của-Rescuer-B}` → kiểm tra bị từ chối.
3. Thử truy cập `/admin/home` khi đang đăng nhập bằng Victim → kiểm tra 403.

### 4.5. Optimistic Locking (race condition)

1. Mở 2 tab trình duyệt, cùng đăng nhập bằng 2 Rescuer khác nhau.
2. Cả 2 cùng vào trang chi tiết cùng 1 yêu cầu SENT.
3. Bấm "Nhận nhiệm vụ" ở tab 1 → thành công.
4. Bấm "Nhận nhiệm vụ" ở tab 2 → kiểm tra thông báo "Yêu cầu đã được người khác nhận".

### 4.6. Chuyển trạng thái hoàn chỉnh

1. Victim tạo yêu cầu → status SENT.
2. Rescuer nhận → ACCEPTED.
3. Rescuer cập nhật → PROCESSING.
4. Rescuer cập nhật → COMPLETED.
5. Victim xác nhận → CONFIRMED.
6. Kiểm tra Victim không còn thấy nút Sửa/Hủy.
7. Kiểm tra Rescuer không còn thấy nút cập nhật trạng thái.

