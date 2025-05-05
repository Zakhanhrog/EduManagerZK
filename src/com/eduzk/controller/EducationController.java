package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.*;
import com.eduzk.model.entities.*;
import com.eduzk.model.entities.ArtStatus;
import com.eduzk.model.entities.ConductRating;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.dao.impl.LogService; // Giả sử LogService ở đây
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.EducationPanel; // Sẽ tạo panel này

import javax.swing.table.TableModel; // Cần cho export
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


public class EducationController {

    private final User currentUser;
    private final IAcademicRecordDAO recordDAO;
    private final IEduClassDAO classDAO;
    private final IStudentDAO studentDAO;
    private final LogService logService;
    private EducationPanel educationPanel; // Tham chiếu đến View
    private List<AcademicRecord> currentDisplayedRecords;
    private List<Student> currentDisplayedStudents;
    private int currentSelectedClassId = -1;


    public EducationController(User currentUser, IAcademicRecordDAO recordDAO, IEduClassDAO classDAO, IStudentDAO studentDAO, LogService logService) {
        this.currentUser = currentUser;
        this.recordDAO = recordDAO;
        this.classDAO = classDAO;
        this.studentDAO = studentDAO;
        this.logService = logService;
        this.currentDisplayedRecords = new ArrayList<>();
        this.currentDisplayedStudents = new ArrayList<>();
    }

    public void setEducationPanel(EducationPanel educationPanel) {
        this.educationPanel = educationPanel;
    }

    // --- Lấy danh sách lớp cho Sidebar ---
    public List<EduClass> getClassesForCurrentUser() {
        if (currentUser == null) return Collections.emptyList();
        try {
            switch (currentUser.getRole()) {
                case ADMIN:
                    // Admin thấy tất cả các lớp, sắp xếp theo tên?
                    return classDAO.getAll().stream()
                            .sorted(Comparator.comparing(EduClass::getClassName))
                            .collect(Collectors.toList());
                case TEACHER:
                    // Teacher thấy lớp mình dạy
                    if (currentUser.getTeacherId() != null) {
                        return classDAO.findByTeacherId(currentUser.getTeacherId()).stream()
                                .sorted(Comparator.comparing(EduClass::getClassName))
                                .collect(Collectors.toList());
                    } else {
                        return Collections.emptyList(); // Teacher chưa được gán ID?
                    }
                case STUDENT:
                default:
                    return Collections.emptyList(); // Student không xem danh sách lớp ở đây
            }
        } catch (DataAccessException e) {
            System.err.println("Error getting classes for user: " + e.getMessage());
            // Có thể thông báo lỗi ở đây nếu cần
            return Collections.emptyList();
        }
    }

    // --- Lấy dữ liệu điểm và học sinh khi chọn lớp (cho Admin/Teacher) ---
    public void loadDataForClass(int classId) {
        if (currentUser.getRole() == Role.STUDENT || classId <= 0) {
            this.currentSelectedClassId = -1;
            this.currentDisplayedStudents = new ArrayList<>();
            this.currentDisplayedRecords = new ArrayList<>();
            if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
            return;
        }

        this.currentSelectedClassId = classId;
        System.out.println("Loading data for class ID: " + classId);

        try {
            // 1. Lấy thông tin lớp để biết danh sách studentId
            EduClass selectedClass = classDAO.getById(classId);
            if (selectedClass == null) {
                this.currentDisplayedStudents = new ArrayList<>();
                this.currentDisplayedRecords = new ArrayList<>();
                if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
                System.err.println("Class not found: " + classId);
                return; // Lớp không tồn tại
            }

            List<Integer> studentIds = selectedClass.getStudentIds();
            if (studentIds == null || studentIds.isEmpty()) {
                this.currentDisplayedStudents = new ArrayList<>();
                this.currentDisplayedRecords = new ArrayList<>();
                if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
                System.out.println("Class has no students: " + classId);
                return; // Lớp không có học sinh
            }

            // 2. Lấy thông tin chi tiết từng học sinh
            this.currentDisplayedStudents = studentIds.stream()
                    .map(studentDAO::getById)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Student::getFullName)) // Sắp xếp theo tên HS
                    .collect(Collectors.toList());

            // 3. Lấy bản ghi điểm của các học sinh trong lớp này
            // Cách 1: Lấy tất cả record của lớp rồi map với student
            // List<AcademicRecord> recordsInClass = recordDAO.findAllByClassId(classId);
            // this.currentDisplayedRecords = this.currentDisplayedStudents.stream()
            //         .map(student -> findOrCreateRecordForStudent(student.getStudentId(), classId, recordsInClass))
            //         .collect(Collectors.toList());

            // Cách 2: Lấy record cho từng student (có thể nhiều query hơn nhưng đảm bảo có record cho mỗi HS)
            this.currentDisplayedRecords = this.currentDisplayedStudents.stream()
                    .map(student -> findOrCreateRecordForStudent(student.getStudentId(), classId))
                    .collect(Collectors.toList());


            // 4. Cập nhật View
            if(educationPanel != null) {
                educationPanel.updateTableData(this.currentDisplayedStudents, this.currentDisplayedRecords);
                writeLog("Viewed Grades", "Viewed grade data for class ID: " + classId);
            }

        } catch (DataAccessException e) {
            System.err.println("Error loading grade data for class " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(null, "Data Load Error", "Failed to load student or grade data for the selected class.");
            this.currentDisplayedStudents = new ArrayList<>();
            this.currentDisplayedRecords = new ArrayList<>();
            if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
        }
    }

    // --- Lấy dữ liệu cho học sinh đang đăng nhập ---
    public void loadDataForCurrentStudent() {
        if (currentUser == null || currentUser.getRole() != Role.STUDENT || currentUser.getStudentId() == null) {
            this.currentSelectedClassId = -1;
            this.currentDisplayedStudents = new ArrayList<>();
            this.currentDisplayedRecords = new ArrayList<>();
            if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
            return;
        }

        int studentId = currentUser.getStudentId();
        System.out.println("Loading grade data for current student ID: " + studentId);
        try {
            Student currentStudent = studentDAO.getById(studentId);
            if (currentStudent == null) {
                // Lỗi: User là student nhưng không có hồ sơ student tương ứng?
                System.err.println("Error: Student profile not found for current user ID: " + studentId);
                this.currentDisplayedStudents = new ArrayList<>();
                this.currentDisplayedRecords = new ArrayList<>();
                if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
                return;
            }

            // Lấy tất cả các bản ghi học tập của học sinh này
            this.currentDisplayedRecords = recordDAO.findAllByStudentId(studentId);
            // Chỉ cần hiển thị thông tin của chính học sinh này
            this.currentDisplayedStudents = List.of(currentStudent); // List chỉ chứa 1 student

            // *** Quan trọng: Cần điều chỉnh View để hiển thị khác cho Student ***
            // View chỉ nên hiển thị các record của student này, có thể theo dạng khác (không cần bảng lớn?)
            if (educationPanel != null) {
                // Gọi một phương thức khác của panel để hiển thị cho student?
                educationPanel.updateTableDataForStudent(currentStudent, this.currentDisplayedRecords);
                writeLog("Viewed Grades", "Student viewed their own grades.");
            }

        } catch (DataAccessException e) {
            System.err.println("Error loading grade data for student " + studentId + ": " + e.getMessage());
            UIUtils.showErrorMessage(null, "Data Load Error", "Failed to load your academic records.");
            this.currentDisplayedStudents = new ArrayList<>();
            this.currentDisplayedRecords = new ArrayList<>();
            if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList()); // Reset view
        }
    }


    // Helper: Tìm hoặc tạo record mới nếu chưa có
    private AcademicRecord findOrCreateRecordForStudent(int studentId, int classId) {
        try {
            return recordDAO.findByStudentAndClass(studentId, classId)
                    .orElseGet(() -> new AcademicRecord(studentId, classId)); // Tạo mới nếu không tìm thấy
        } catch (DataAccessException e) {
            System.err.println("Error finding/creating record for student " + studentId + " in class " + classId + ": " + e.getMessage());
            return new AcademicRecord(studentId, classId); // Trả về record trống nếu lỗi
        }
    }
    // Helper: Tìm record trong danh sách đã tải (cho cách 1 của loadDataForClass)
    // private AcademicRecord findOrCreateRecordForStudent(int studentId, int classId, List<AcademicRecord> recordsInClass) {
    //     return recordsInClass.stream()
    //                 .filter(r -> r.getStudentId() == studentId)
    //                 .findFirst()
    //                 .orElseGet(() -> new AcademicRecord(studentId, classId));
    // }


    // --- Xử lý khi dữ liệu trong bảng thay đổi (View gọi hàm này) ---
    public void updateRecordInMemory(int rowIndex, String subjectKey, Object value) {
        if (!canCurrentUserEdit()) return; // Kiểm tra quyền

        if (rowIndex >= 0 && rowIndex < currentDisplayedRecords.size()) {
            AcademicRecord record = currentDisplayedRecords.get(rowIndex);
            boolean changed = false;
            try {
                if ("Hạnh kiểm".equals(subjectKey)) { // <<< Key cho hạnh kiểm
                    if (value instanceof ConductRating) {
                        record.setConductRating((ConductRating) value);
                        changed = true;
                    }
                } else if ("Nghệ thuật".equals(subjectKey)) { // <<< Key cho Nghệ thuật
                    if (value instanceof ArtStatus) {
                        record.setArtStatus((ArtStatus) value);
                        changed = true;
                    }
                } else { // Các môn học khác (điểm số)
                    Double grade = null;
                    if (value instanceof Number) {
                        grade = ((Number) value).doubleValue();
                        // Thêm validation điểm (0-10)
                        if (grade < 0 || grade > 10) {
                            throw new IllegalArgumentException("Grade must be between 0 and 10.");
                        }
                    } else if (value instanceof String && !((String) value).trim().isEmpty()) {
                        try {
                            grade = Double.parseDouble(((String) value).trim());
                            if (grade < 0 || grade > 10) {
                                throw new IllegalArgumentException("Grade must be between 0 and 10.");
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid number format for grade.");
                        }
                    }
                    // Chỉ set nếu có thay đổi hoặc là giá trị mới (khác null)
                    if (!Objects.equals(record.getGrade(subjectKey), grade)) {
                        record.setGrade(subjectKey, grade); // Dùng setGrade để xử lý cả null
                        changed = true;
                    }
                }

                if (changed && educationPanel != null) {
                    // Yêu cầu view cập nhật lại các giá trị tính toán cho hàng đó
                    educationPanel.updateCalculatedValues(rowIndex, record);
                    educationPanel.markChangesPending(true); // Báo hiệu có thay đổi chưa lưu
                }
            } catch (IllegalArgumentException e) {
                UIUtils.showWarningMessage(educationPanel,"Validation Error", e.getMessage());
                // Có thể cần reset giá trị ô trong bảng về giá trị cũ
                if(educationPanel != null) educationPanel.refreshTableCell(rowIndex, subjectKey);
            }
        }
    }

    // --- Lưu các thay đổi ---
    public void saveAllChanges() {
        if (!canCurrentUserEdit()) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "You do not have permission to save changes.");
            return;
        }
        if (currentDisplayedRecords == null || currentDisplayedRecords.isEmpty()) {
            UIUtils.showInfoMessage(educationPanel, "No Data", "There is no data to save.");
            return;
        }

        // Xác nhận trước khi lưu
        if (!UIUtils.showConfirmDialog(educationPanel, "Confirm Save", "Save all changes made to grades and conduct for class ID " + currentSelectedClassId + "?")) {
            return;
        }

        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();

        System.out.println("Saving " + currentDisplayedRecords.size() + " academic records...");
        for (AcademicRecord record : currentDisplayedRecords) {
            try {
                // Chỉ lưu những record có studentId và classId hợp lệ
                if (record.getStudentId() > 0 && record.getClassId() > 0) {
                    recordDAO.addOrUpdate(record);
                    successCount++;
                }
            } catch (DataAccessException | IllegalArgumentException e) {
                errorCount++;
                String studentName = getStudentNameById(record.getStudentId());
                String msg = "Failed to save record for " + studentName + ": " + e.getMessage();
                System.err.println(msg);
                errorMessages.add(msg);
            }
        }

        System.out.println("Save complete. Success: " + successCount + ", Errors: " + errorCount);

        // Ghi log hành động lưu
        writeLog("Saved Grades", "Saved " + successCount + "/" + currentDisplayedRecords.size() + " records for class ID: " + currentSelectedClassId + (errorCount > 0 ? " ("+errorCount+" errors)" : ""));


        // Hiển thị kết quả
        if (errorCount > 0) {
            UIUtils.showWarningMessage(educationPanel, "Save Partially Successful",
                    "Successfully saved " + successCount + " records.\n" +
                            "Failed to save " + errorCount + " records.\n\nFirst few errors:\n" +
                            String.join("\n", errorMessages.stream().limit(3).collect(Collectors.toList())) +
                            (errorMessages.size() > 3 ? "\n..." : ""));
        } else {
            UIUtils.showInfoMessage(educationPanel, "Save Successful", "All changes saved successfully.");
        }

        // Cập nhật trạng thái nút Save trong View
        if(educationPanel != null) educationPanel.markChangesPending(false); // Reset trạng thái pending

        // Tải lại dữ liệu sau khi lưu để đảm bảo tính nhất quán (tùy chọn)
        // loadDataForClass(currentSelectedClassId);
    }


    // --- Helper lấy tên học sinh ---
    public String getStudentNameById(int studentId) {
        if (currentDisplayedStudents != null) {
            // Tìm trong danh sách đang hiển thị trước cho nhanh
            Optional<Student> opt = currentDisplayedStudents.stream().filter(s -> s.getStudentId() == studentId).findFirst();
            if(opt.isPresent()) return opt.get().getFullName();
        }
        // Nếu không có trong list hiện tại, query DAO
        try {
            Student student = studentDAO.getById(studentId);
            return (student != null) ? student.getFullName() : "Unknown Student [" + studentId + "]";
        } catch (DataAccessException e) {
            return "Error [" + studentId + "]";
        }
    }

    // --- Kiểm tra quyền chỉnh sửa ---
    public boolean canCurrentUserEdit() {
        if (currentUser == null) return false;
        // Admin luôn có quyền
        if (currentUser.getRole() == Role.ADMIN) return true;
        // Teacher có quyền nếu đang xem lớp họ quản lý
        if (currentUser.getRole() == Role.TEACHER) {
            if (currentSelectedClassId <= 0 || currentUser.getTeacherId() == null) return false;
            // Kiểm tra xem lớp đang chọn có phải do teacher này dạy không
            try {
                EduClass selectedClass = classDAO.getById(currentSelectedClassId);
                // Teacher có quyền nếu họ là GVCN của lớp này
                return selectedClass != null && currentUser.getTeacherId().equals(selectedClass.getPrimaryTeacher().getTeacherId());
                // Hoặc logic phức tạp hơn nếu có GV kiêm nhiệm
            } catch (DataAccessException e) {
                return false;
            }
        }
        return false; // Các role khác không có quyền sửa
    }

    // --- Chuẩn bị dữ liệu cho Export ---
    public Object[][] getGradeDataForExport(int classId) {
        if (currentUser.getRole() == Role.STUDENT) return new Object[0][0]; // Học sinh không export bảng điểm lớp

        // Tải dữ liệu mới nhất cho lớp này (giống loadDataForClass nhưng không cập nhật UI)
        List<Student> students;
        List<AcademicRecord> records;
        try {
            EduClass selectedClass = classDAO.getById(classId);
            if (selectedClass == null || selectedClass.getStudentIds() == null || selectedClass.getStudentIds().isEmpty()) {
                return new Object[0][0];
            }
            List<Integer> studentIds = selectedClass.getStudentIds();
            students = studentIds.stream()
                    .map(studentDAO::getById)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Student::getFullName))
                    .collect(Collectors.toList());
            records = students.stream()
                    .map(student -> findOrCreateRecordForStudent(student.getStudentId(), classId))
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            System.err.println("Error preparing export data for class " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(null, "Export Error", "Could not load data for export.");
            return new Object[0][0];
        }


        // --- Định nghĩa các cột (PHẢI KHỚP VỚI BẢNG HIỂN THỊ) ---
        String[] columnKeys = {"STT", "Tên HS", "Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", "Nghệ thuật", "TB KHTN", "TB KHXH", "TB môn học", "Hạnh kiểm"};
        Object[][] data = new Object[students.size()][columnKeys.length];

        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            AcademicRecord record = records.get(i); // Đảm bảo thứ tự khớp nhau
            int stt = i + 1;

            data[i][0] = stt;
            data[i][1] = student.getFullName();
            data[i][2] = record.getGrade("Toán");
            data[i][3] = record.getGrade("Văn");
            data[i][4] = record.getGrade("Anh");
            data[i][5] = record.getGrade("Lí");
            data[i][6] = record.getGrade("Hoá");
            data[i][7] = record.getGrade("Sinh");
            data[i][8] = record.getGrade("Sử");
            data[i][9] = record.getGrade("Địa");
            data[i][10] = record.getGrade("GDCD");
            data[i][11] = (record.getArtStatus() != null) ? record.getArtStatus().toString() : "";
            data[i][12] = record.calculateAvgNaturalSciences(); // Gọi hàm tính
            data[i][13] = record.calculateAvgSocialSciences(); // Gọi hàm tính
            data[i][14] = record.calculateAvgOverallSubjects(); // Gọi hàm tính
            data[i][15] = (record.getConductRating() != null) ? record.getConductRating().toString() : "";
        }
        writeLog("Exported Grades", "Exported grade data for class ID: " + classId);
        return data;
    }
    // --- Phương thức ghi log chung ---
    private void writeLog(String action, String details) {
        if (logService != null && currentUser != null) {
            try {
                LogEntry log = new LogEntry(
                        LocalDateTime.now(),
                        currentUser.getDisplayName(),
                        currentUser.getRole().name(),
                        action,
                        details
                );
                logService.addLogEntry(log);
            } catch (Exception e) {
                System.err.println("!!! Failed to write log entry: Action=" + action + ", Details=" + details + " - Error: " + e.getMessage());
            }
        } else {
            System.err.println("LogService or CurrentUser is null. Cannot write log for action: " + action);
        }
    }
    public int getCurrentSelectedClassId() {
        return currentSelectedClassId;
    }
}