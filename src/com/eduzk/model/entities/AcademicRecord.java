package com.eduzk.model.entities;

import com.eduzk.model.entities.ArtStatus;
import com.eduzk.model.entities.ConductRating;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AcademicRecord implements Serializable {
    private static final long serialVersionUID = 3L; // Thay đổi serialVersionUID

    private int recordId;
    private int studentId;
    private int classId;
    private Map<String, Double> subjectGrades;

    private ArtStatus artStatus;
    private ConductRating conductRating;
    public AcademicRecord() {
        subjectGrades = new HashMap<>();
        // Khởi tạo giá trị mặc định nếu cần
        this.artStatus = ArtStatus.PASSED;
        this.conductRating = ConductRating.GOOD;
    }

    public AcademicRecord(int studentId, int classId) {
        this(); // Gọi constructor mặc định
        this.studentId = studentId;
        this.classId = classId;
    }

    // --- Getters and Setters ---
    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public Map<String, Double> getSubjectGrades() {
        if (subjectGrades == null) subjectGrades = new HashMap<>(); // Đảm bảo không null
        return subjectGrades;
    }
    public void setSubjectGrades(Map<String, Double> subjectGrades) { this.subjectGrades = subjectGrades; }

    public ArtStatus getArtStatus() { return artStatus; }
    public void setArtStatus(ArtStatus artStatus) { this.artStatus = artStatus; }

    public ConductRating getConductRating() { return conductRating; }
    public void setConductRating(ConductRating conductRating) { this.conductRating = conductRating; }

    // --- Phương thức tiện ích để lấy/đặt điểm ---
    public Double getGrade(String subjectKey) {
        return getSubjectGrades().get(subjectKey); // Trả về null nếu chưa có điểm
    }
    public void setGrade(String subjectKey, Double grade) {
        if (grade == null) {
            getSubjectGrades().remove(subjectKey);
        } else {
            // Có thể thêm validation điểm ở đây (0-10)
            getSubjectGrades().put(subjectKey, grade);
        }
    }


    // --- Các phương thức tính toán ---
    // (Nên đặt logic tính toán trong Controller hoặc Service để linh hoạt)
    // Nhưng có thể đặt ở đây để tiện truy cập
    public double calculateAvgNaturalSciences() {
        double sum = 0;
        int count = 0;
        String[] keys = {"Lí", "Hoá", "Sinh"}; // *** Cần thống nhất key môn học ***
        for (String key : keys) {
            Double grade = getGrade(key);
            if (grade != null) {
                sum += grade;
                count++;
            }
        }
        return (count > 0) ? sum / count : 0.0;
    }

    public double calculateAvgSocialSciences() {
        double sum = 0;
        int count = 0;
        String[] keys = {"Sử", "Địa", "GDCD"}; // *** Cần thống nhất key môn học ***
        for (String key : keys) {
            Double grade = getGrade(key);
            if (grade != null) {
                sum += grade;
                count++;
            }
        }
        return (count > 0) ? sum / count : 0.0;
    }

    public double calculateAvgOverallSubjects() {
        double sum = 0;
        int count = 0;
        for (Map.Entry<String, Double> entry : getSubjectGrades().entrySet()) {
            // *** Bỏ qua môn Nghệ thuật - Cần key thống nhất ***
            if (!entry.getKey().equalsIgnoreCase("Nghệ thuật") && entry.getValue() != null) {
                sum += entry.getValue();
                count++;
            }
        }
        return (count > 0) ? sum / count : 0.0;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcademicRecord that = (AcademicRecord) o;
        // ID là duy nhất HOẶC kết hợp studentId và classId (và term/year nếu có)
        if (recordId > 0 && that.recordId > 0) {
            return recordId == that.recordId;
        }
        // Nếu ID chưa có (chưa lưu), so sánh theo student và class
        return studentId == that.studentId && classId == that.classId;
    }

    @Override
    public int hashCode() {
        // Nếu ID > 0, dùng ID. Nếu không, dùng studentId và classId
        return Objects.hash(recordId > 0 ? recordId : Objects.hash(studentId, classId));
    }

    @Override
    public String toString() {
        return "AcademicRecord{" +
                "recordId=" + recordId +
                ", studentId=" + studentId +
                ", classId=" + classId +
                ", grades=" + subjectGrades.size() + // Chỉ hiện số lượng điểm
                ", art=" + artStatus +
                ", conduct=" + conductRating +
                '}';
    }
}