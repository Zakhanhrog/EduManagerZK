package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.ITeacherDAO;
import com.eduzk.model.entities.Teacher;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TeacherDAOImpl extends BaseDAO<Teacher> implements ITeacherDAO {

    private final IdGenerator idGenerator;

    public TeacherDAOImpl(String dataFilePath, String idFilePath) {
        super(dataFilePath);
        this.idGenerator = new IdGenerator(idFilePath);
        System.out.println("TeacherDAOImpl initialized. dataList size after load: " + (dataList == null ? "null" : dataList.size()));
    }

    @Override
    public Teacher getById(int id) {
        System.out.println("TeacherDAOImpl.getById(" + id + ") called. Current dataList size: " + (dataList == null ? "null" : dataList.size()));
        lock.readLock().lock();
        try {
            Optional<Teacher> found = dataList.stream()
                    .filter(teacher -> teacher.getTeacherId() == id)
                    .findFirst();
            System.out.println("TeacherDAOImpl.getById(" + id + ") - Found: " + found.isPresent());
            return found.orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(Teacher teacher) {
        if (teacher == null) {
            throw new IllegalArgumentException("Teacher cannot be null.");
        }
        teacher.setTeacherId(idGenerator.getNextTeacherId());

        lock.writeLock().lock();
        try {
            // Optional: Check for duplicate email/phone
            this.dataList.add(teacher);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Teacher teacher) {
        if (teacher == null) {
            throw new IllegalArgumentException("Teacher cannot be null.");
        }
        lock.writeLock().lock();
        try {
            int index = -1;
            for (int i = 0; i < dataList.size(); i++) {
                if (dataList.get(i).getTeacherId() == teacher.getTeacherId()) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                // Optional: Check for duplicate email/phone before update
                dataList.set(index, teacher);
                saveData();
            } else {
                throw new DataAccessException("Teacher with ID " + teacher.getTeacherId() + " not found for update.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int id) {
        lock.writeLock().lock();
        try {
            // Check if teacher is assigned to any active classes or schedules before deleting?
            // This might require calls to EduClassDAO or ScheduleDAO.
            // For simplicity now, we just delete. Add checks later if needed.

            boolean removed = dataList.removeIf(teacher -> teacher.getTeacherId() == id);
            if (removed) {
                saveData();
            } else {
                System.err.println("Warning: Teacher with ID " + id + " not found for deletion.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Teacher> findBySpecialization(String specialization) {
        lock.readLock().lock();
        try {
            String lowerCaseSpec = specialization.toLowerCase();
            return dataList.stream()
                    .filter(teacher -> teacher.getSpecialization() != null &&
                            teacher.getSpecialization().toLowerCase().contains(lowerCaseSpec))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public int deleteMultiple(List<Integer> ids) throws DataAccessException {
        if (ids == null || ids.isEmpty()) {
            return 0; // Không có gì để xóa
        }

        int initialSize;
        int finalSize;

        lock.writeLock().lock(); // Lấy khóa ghi vì ta sẽ thay đổi dataList
        try {
            initialSize = dataList.size(); // Ghi lại kích thước ban đầu
            System.out.println("TeacherDAOImpl.deleteMultiple: Initial size = " + initialSize + ", attempting to remove IDs: " + ids);

            // Sử dụng removeIf để xóa hiệu quả các phần tử khớp với danh sách IDs
            // Cần chuyển List<Integer> thành một cấu trúc cho phép kiểm tra nhanh (ví dụ: Set) nếu danh sách ID lớn
            // Với danh sách nhỏ, contains() trên List cũng tạm ổn.
            // List<Integer> finalIds = new ArrayList<>(ids); // Tạo bản copy final để dùng trong lambda
            boolean removed = dataList.removeIf(teacher -> ids.contains(teacher.getTeacherId()));

            finalSize = dataList.size(); // Ghi lại kích thước sau khi xóa
            int deletedCount = initialSize - finalSize;
            System.out.println("TeacherDAOImpl.deleteMultiple: Removed " + deletedCount + " items. Final size = " + finalSize);


            // Chỉ gọi saveData MỘT LẦN nếu có sự thay đổi
            if (deletedCount > 0) {
                System.out.println("TeacherDAOImpl.deleteMultiple: Saving data after deletion...");
                saveData(); // Lưu trạng thái mới của dataList vào file
                System.out.println("TeacherDAOImpl.deleteMultiple: Data saved.");
            } else {
                System.out.println("TeacherDAOImpl.deleteMultiple: No items were removed matching the IDs.");
            }

            return deletedCount; // Trả về số lượng đã xóa

        } catch (Exception e) { // Bắt lỗi chung khi remove hoặc save
            // Gói lại lỗi thành DataAccessException để Controller xử lý nhất quán
            System.err.println("Error during multiple delete operation: " + e.getMessage());
            e.printStackTrace();
            throw new DataAccessException("Error deleting multiple teachers.", e);
        } finally {
            lock.writeLock().unlock(); // Luôn nhả khóa ghi
        }
    }

    // getAll() is inherited from BaseDAO
}