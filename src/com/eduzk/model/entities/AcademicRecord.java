package com.eduzk.model.entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

public class AcademicRecord implements Serializable {
    private static final long serialVersionUID = 3L;
    private int recordId;
    private int studentId;
    private int classId;
    private Map<String, Double> subjectGrades;
    private ArtStatus artStatus;
    private ConductRating conductRating;
    private static final String[] SUBJECTS_TO_CLEAR = {
            "Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD"
    };

    public AcademicRecord() {
        subjectGrades = new HashMap<>();
        this.artStatus = ArtStatus.PASSED;
        this.conductRating = ConductRating.GOOD;
    }

    public AcademicRecord(int studentId, int classId) {
        this();
        this.studentId = studentId;
        this.classId = classId;
    }

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public Map<String, Double> getSubjectGrades() {
        if (subjectGrades == null) subjectGrades = new HashMap<>();
        return subjectGrades;
    }

    public ArtStatus getArtStatus() { return artStatus; }
    public void setArtStatus(ArtStatus artStatus) { this.artStatus = artStatus; }
    public ConductRating getConductRating() { return conductRating; }
    public void setConductRating(ConductRating conductRating) { this.conductRating = conductRating; }
    public Double getGrade(String subjectKey) {
        return getSubjectGrades().get(subjectKey);
    }
    public void setGrade(String subjectKey, Double grade) {
        if (grade == null) {
            getSubjectGrades().remove(subjectKey);
        } else {
            getSubjectGrades().put(subjectKey, grade);
        }
    }

    public double calculateAvgNaturalSciences() {
        double sum = 0;
        int count = 0;
        String[] keys = {"Lí", "Hoá", "Sinh"};
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
        String[] keys = {"Sử", "Địa", "GDCD"};
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
            if (!entry.getKey().equalsIgnoreCase("Nghệ thuật") && entry.getValue() != null) {
                sum += entry.getValue();
                count++;
            }
        }
        return (count > 0) ? sum / count : 0.0;
    }
    public String getAchievementTitle() {
        final double MIN_SCORE_REQUIRED = 2.00;
        String[] subjectsToCheck = {"Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD"};

        for (String subject : subjectsToCheck) {
            Double score = this.getGrade(subject);
            if (score == null || score < MIN_SCORE_REQUIRED) {
                return "\"Không đủ điều kiện xét\"";
            }
        }

        String part1Title = null;
        double overallAvg = this.calculateAvgOverallSubjects();
        ConductRating conduct = this.getConductRating();
        Double mathScore = this.getGrade("Toán");
        Double literatureScore = this.getGrade("Văn");
        Double englishScore = this.getGrade("Anh");

        if (conduct == null || mathScore == null || literatureScore == null || englishScore == null) {
            part1Title = "Chưa đủ thông tin xếp loại";
        } else {
            if (overallAvg >= 9.00 && mathScore >= 9.00 && literatureScore >= 9.00 && englishScore >= 9.00 &&
                    conduct == ConductRating.EXCELLENT) {
                part1Title = "HS Xuất sắc";
            } else if (overallAvg >= 8.00 && mathScore >= 8.00 && literatureScore >= 8.00 && englishScore >= 8.00 &&
                    (conduct == ConductRating.EXCELLENT || conduct == ConductRating.GOOD)) {
                part1Title = "HS Giỏi";
            } else if (overallAvg >= 6.50 && mathScore >= 6.5 && literatureScore >= 6.5 && englishScore >= 6.5 &&
                    (conduct == ConductRating.EXCELLENT || conduct == ConductRating.GOOD || conduct == ConductRating.FAIR)) {
                part1Title = "HS Khá";
            } else if (overallAvg < 6.50 &&
                    (conduct == ConductRating.EXCELLENT || conduct == ConductRating.GOOD || conduct == ConductRating.FAIR || conduct == ConductRating.AVERAGE)) {
                part1Title = "HS Trung bình";
            } else {
                part1Title = "Đạt chương trình học tập";
            }
        }

        List<String> part2Titles = new ArrayList<>();
        double avgKHTN = this.calculateAvgNaturalSciences();
        double avgKHXH = this.calculateAvgSocialSciences();

        if (avgKHTN >= 9.50) {
            part2Titles.add("Chuyên KHTN");
        } else if (avgKHTN >= 9.00) {
            part2Titles.add("Bán chuyên KHTN");
        }

        if (avgKHXH >= 9.50) {
            part2Titles.add("Chuyên KHXH");
        } else if (avgKHXH >= 9.00) {
            part2Titles.add("Bán chuyên KHXH");
        }

        StringBuilder finalTitle = new StringBuilder("");
        boolean hasPart1 = false;
        boolean hasPart2 = !part2Titles.isEmpty();

        if (part1Title != null && !part1Title.trim().isEmpty()) {
            if (!part1Title.equalsIgnoreCase("Chưa đủ thông tin xếp loại") &&
                    !part1Title.equalsIgnoreCase("Đạt chương trình học tập") &&
                    !part1Title.equalsIgnoreCase("Không đủ điều kiện xét") &&
                    !part1Title.equalsIgnoreCase("Chưa xếp loại")) {
                finalTitle.append("\"").append(part1Title.trim()).append("\"");
                hasPart1 = true;
            } else {
                finalTitle.append(part1Title.trim());
            }
        } else {
            finalTitle.append("\"Chưa xếp loại\"");
        }

        if (hasPart2) {
            if (hasPart1) {
                finalTitle.append(" , ");
            } else if (finalTitle.length() > "Học lực: ".length()) {
                finalTitle.append(" , ");
            }
            finalTitle.append("\"").append(String.join(", ", part2Titles)).append("\"");
        }

        if (!hasPart1 && !hasPart2) {
            String currentBuiltTitle = finalTitle.toString();
            if (currentBuiltTitle.equals("\"Chưa xếp loại\"") ||
                    currentBuiltTitle.equals("Đạt chương trình học tập") ||
                    currentBuiltTitle.equals("")) {
                return "(Không có học lực nổi bật)";
            }
        }

        return finalTitle.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcademicRecord that = (AcademicRecord) o;
        if (recordId > 0 && that.recordId > 0) {
            return recordId == that.recordId;
        }
        return studentId == that.studentId && classId == that.classId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId > 0 ? recordId : Objects.hash(studentId, classId));
    }

    @Override
    public String toString() {
        return "AcademicRecord{" +
                "recordId=" + recordId +
                ", studentId=" + studentId +
                ", classId=" + classId +
                ", grades=" + subjectGrades.size() +
                ", art=" + artStatus +
                ", conduct=" + conductRating +
                '}';
    }
    public boolean clearSubjectGrades() {
        boolean changed = false;
        if (this.subjectGrades == null) {
            return false;
        }

        for (String subjectKey : SUBJECTS_TO_CLEAR) {
            if (this.subjectGrades.containsKey(subjectKey) && this.subjectGrades.get(subjectKey) != null) {
                this.subjectGrades.put(subjectKey, null);
                changed = true;
            }
        }
        return changed;
    }
}