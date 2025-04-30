# EduZk - Quản lý Giáo dục Cơ bản

Ứng dụng desktop đơn giản giúp quản lý thông tin học viên, giáo viên, khóa học, lớp học và lịch trình cơ bản.

## Yêu cầu

*   JDK 8+
*   Thư viện JCalendar (`jcalendar-*.jar`)

## Cài đặt Nhanh

1.  **Thư mục:** Đặt code dự án vào thư mục `EduHub/`.
2.  **Thư viện:** Tạo `EduHub/lib/` và đặt `jcalendar-*.jar` vào đó.
3.  **Dữ liệu:** Tạo thư mục `EduHub/data/` (sẽ chứa các file `.dat`).
4.  **Icons:** Tạo `EduHub/resources/icons/` và đặt `add.png`, `edit.png`, `delete.png` vào đó.
5.  **IDE:**
    *   Thêm `jcalendar-*.jar` vào classpath.
    *   Đánh dấu `resources` là "Resources Root".

## Chạy Ứng dụng

*   Biên dịch dự án.
*   Chạy phương thức `main` trong `src/com/eduhub/App.java`.

## Đăng nhập Lần đầu

Khi chạy lần đầu (với thư mục `data/` trống), các tài khoản mặc định sau sẽ được tạo:

*   **Admin:** `admin` / `admin`
*   **Teacher:** `teacher1` / `teacher1` (Cần có Teacher với ID=1 tồn tại trước - Tạo bằng tài khoản Admin nếu cần).
*   **Student:** `student1` / `student1`

**Cảnh báo:** Mật khẩu mặc định không an toàn và được lưu dạng văn bản thường.

## Chức năng Cơ bản (Khi đăng nhập là Admin)

Sử dụng các tab để quản lý:

*   **Students:** Thêm, Sửa, Xóa, Tìm kiếm học viên.
*   **Teachers:** Thêm, Sửa, Xóa, Tìm kiếm giáo viên.
*   **Courses:** Thêm, Sửa, Xóa, Tìm kiếm khóa học.
*   **Rooms:** Thêm, Sửa, Xóa, Tìm kiếm phòng theo sức chứa.
*   **Classes:**
    *   Thêm, Sửa, Xóa lớp (liên kết Khóa học & Giáo viên).
    *   Chọn lớp để xem/quản lý học viên đã ghi danh (Enroll/Unenroll).
*   **Schedule:**
    *   Thêm, Sửa, Xóa buổi học (liên kết Lớp, Giáo viên, Phòng, Thời gian).
    *   Hệ thống tự động kiểm tra xung đột lịch Giáo viên/Phòng khi thêm/sửa.
    *   Lọc lịch theo khoảng ngày.

## Lưu ý Quan trọng

*   **Quản lý User:** Chưa có giao diện cho Admin quản lý tài khoản khác.
*   **Điểm/Điểm danh:** Chưa triển khai.
*   **Phân quyền chi tiết:** Teacher/Student hiện thấy toàn bộ dữ liệu và có thể có quyền chưa đúng.
*   **Bảo mật:** Mật khẩu **không** được mã hóa.
*   **Lưu trữ:** Dùng file `.dat`, không tối ưu cho dữ liệu lớn.