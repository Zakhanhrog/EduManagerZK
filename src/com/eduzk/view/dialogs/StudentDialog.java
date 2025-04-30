package com.eduzk.view.dialogs; // Giữ nguyên package của bạn

import com.eduzk.controller.StudentController;
import com.eduzk.model.entities.Student;
import com.eduzk.utils.DateUtils; // Import DateUtils nếu bạn dùng trong code (ví dụ: để parse/format)
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.view.components.CustomDatePicker; // Sử dụng CustomDatePicker

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Objects; // Import Objects nếu cần so sánh

/**
 * JDialog để Thêm hoặc Sửa thông tin Học sinh (Student).
 * Bao gồm chức năng cho Admin xem và cập nhật mật khẩu tài khoản User liên kết.
 */
public class StudentDialog extends JDialog {

    private final StudentController controller;
    private final Student studentToEdit; // Dữ liệu student hiện tại (null nếu là chế độ Add)
    private final boolean isEditMode;    // Cờ xác định chế độ Add hay Edit

    // --- UI Components ---
    private JTextField idField;           // Hiển thị ID (chỉ đọc, chỉ hiện ở Edit mode)
    private JTextField nameField;         // Nhập/Sửa Họ tên
    private CustomDatePicker dobPicker;     // Chọn Ngày sinh
    private JComboBox<String> genderComboBox; // Chọn Giới tính
    private JTextField addressField;      // Nhập/Sửa Địa chỉ
    private JTextField parentNameField;   // Nhập/Sửa Tên phụ huynh
    private JTextField phoneField;        // Nhập/Sửa Số điện thoại (dùng làm username đăng nhập)
    private JTextField emailField;        // Nhập/Sửa Email
    private JButton saveButton;           // Nút Lưu
    private JButton cancelButton;         // Nút Hủy
    // --- Components cho Password (chỉ Admin thấy/sửa khi Edit) ---
    private JLabel passwordLabel;         // Nhãn cho ô mật khẩu
    private JTextField passwordField;     // Ô hiển thị/nhập mật khẩu (Plain text - KHÔNG AN TOÀN)
    // Có thể thay bằng JPasswordField nếu chỉ muốn cho phép đặt lại mật khẩu mới
    // private JPasswordField passwordField;
    // --- Kết thúc Components ---

    /**
     * Constructor của StudentDialog.
     * @param owner      Frame cha (thường là MainView).
     * @param controller Đối tượng StudentController để tương tác với logic và DAO.
     * @param student    Đối tượng Student cần sửa (null nếu ở chế độ Add).
     */
    public StudentDialog(Frame owner, StudentController controller, Student student) {
        super(owner, true); // Tạo JDialog modal
        this.controller = controller;
        this.studentToEdit = student;
        this.isEditMode = (student != null); // Xác định chế độ dựa trên student có null hay không

        // Đặt tiêu đề cửa sổ phù hợp
        setTitle(isEditMode ? "Edit Student Information" : "Add New Student");

        // Khởi tạo các thành phần giao diện
        initComponents();
        // Thiết lập bố cục các thành phần
        setupLayout();
        // Gán hành động cho các nút bấm
        setupActions();
        // Điền dữ liệu vào các trường nếu ở chế độ Edit
        populateFields();
        // Cấu hình các thuộc tính cửa sổ dialog
        configureDialog();
    }

    /** Khởi tạo các thành phần giao diện (JTextField, JComboBox, JButton, ...). */
    private void initComponents() {
        idField = new JTextField(5);
        idField.setEditable(false); // ID không cho phép sửa
        idField.setToolTipText("Student ID (Auto-generated)");

        nameField = new JTextField(25);
        dobPicker = new CustomDatePicker(); // Dùng component chọn ngày tùy chỉnh
        genderComboBox = new JComboBox<>(new String[]{"Male", "Female", "Other"}); // Các lựa chọn giới tính
        addressField = new JTextField(30);
        parentNameField = new JTextField(25);
        phoneField = new JTextField(15);
        emailField = new JTextField(25);

        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");

        // Khởi tạo component cho mật khẩu
        passwordLabel = new JLabel("Account Password:");
        passwordField = new JTextField(25);
        // Cảnh báo bảo mật khi hiển thị/sửa mật khẩu plain text
        passwordField.setToolTipText("WARNING: Editing/Viewing plain text password. Consider using a 'Reset Password' feature instead.");
        // Ban đầu ẩn các component mật khẩu đi
        passwordLabel.setVisible(false);
        passwordField.setVisible(false);
    }

    /** Thiết lập bố cục các thành phần giao diện trong dialog sử dụng GridBagLayout. */
    private void setupLayout() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Padding
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST; // Căn lề trái
        gbc.insets = new Insets(5, 5, 5, 5); // Khoảng cách giữa các component
        int currentRow = 0; // Biến đếm dòng hiện tại

        // --- Hàng 0: ID (Chỉ hiển thị khi Edit) ---
        if (isEditMode) {
            gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
            formPanel.add(new JLabel("Student ID:"), gbc);
            gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            formPanel.add(idField, gbc);
            currentRow++; // Tăng dòng lên
        }

        // --- Hàng 1: Họ Tên ---
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Full Name*:"), gbc); // Dấu * chỉ trường bắt buộc
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(nameField, gbc);
        currentRow++;

        // --- Hàng 2: Ngày sinh và Giới tính ---
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Date of Birth:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5; // Chia đôi không gian
        formPanel.add(dobPicker, gbc);

        gbc.gridx = 2; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST; // Căn label Gender sang phải
        formPanel.add(new JLabel("Gender:"), gbc);
        gbc.gridx = 3; gbc.gridy = currentRow; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5; // Chia đôi không gian
        formPanel.add(genderComboBox, gbc);
        currentRow++;

        // --- Hàng 3: Địa chỉ ---
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(addressField, gbc);
        currentRow++;

        // --- Hàng 4: Tên Phụ huynh ---
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Parent Name:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(parentNameField, gbc);
        currentRow++;

        // --- Hàng 5: Số điện thoại (Username) ---
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Phone (Username)*:"), gbc); // Nhấn mạnh là username
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(phoneField, gbc);
        currentRow++;

        // --- Hàng 6: Email ---
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(emailField, gbc);
        currentRow++;

        // --- Hàng 7: Mật khẩu (Chỉ Admin thấy khi Edit) ---
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(passwordLabel, gbc); // Thêm label mật khẩu
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(passwordField, gbc); // Thêm trường mật khẩu
        currentRow++;

        // --- Panel chứa các nút bấm ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // Căn phải
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // --- Thêm các panel vào Dialog ---
        setLayout(new BorderLayout(0, 10)); // Khoảng cách dọc giữa form và nút
        add(formPanel, BorderLayout.CENTER); // Form ở giữa
        add(buttonPanel, BorderLayout.SOUTH); // Nút ở dưới
    }

    /** Gán các hành động (ActionListeners) cho các nút bấm. */
    private void setupActions() {
        saveButton.addActionListener(e -> saveStudent()); // Gọi hàm saveStudent khi nhấn Save
        cancelButton.addActionListener(e -> dispose());   // Đóng dialog khi nhấn Cancel
    }

    /** Điền dữ liệu từ `studentToEdit` vào các trường khi ở chế độ Edit. */
    private void populateFields() {
        // Mặc định ẩn phần mật khẩu
        passwordLabel.setVisible(false);
        passwordField.setVisible(false);
        passwordField.setText(""); // Xóa giá trị cũ (nếu có)

        if (isEditMode && studentToEdit != null) {
            // Điền thông tin cơ bản của Student
            idField.setText(String.valueOf(studentToEdit.getStudentId()));
            nameField.setText(studentToEdit.getFullName());
            dobPicker.setDate(studentToEdit.getDateOfBirth());
            genderComboBox.setSelectedItem(studentToEdit.getGender());
            addressField.setText(studentToEdit.getAddress());
            parentNameField.setText(studentToEdit.getParentName());
            phoneField.setText(studentToEdit.getPhone());
            emailField.setText(studentToEdit.getEmail());

            // --- Lấy và hiển thị mật khẩu nếu có tài khoản User liên kết ---
            // Chỉ Admin mới thấy và sửa được mật khẩu này
            // Cần kiểm tra vai trò của người dùng đang đăng nhập (lấy từ StudentController)
            if (controller != null && controller.isCurrentUserAdmin()) { // Giả sử có hàm này trong Controller
                String currentPassword = controller.getPasswordForStudent(studentToEdit.getStudentId());
                if (currentPassword != null) {
                    passwordLabel.setVisible(true); // Hiện nếu có tài khoản
                    passwordField.setVisible(true);
                    // !!! HIỂN THỊ PLAIN TEXT - CẢNH BÁO BẢO MẬT !!!
                    passwordField.setText(currentPassword);
                } else {
                    // Có thể hiển thị thông báo "Chưa đăng ký tài khoản" nếu muốn
                    passwordLabel.setVisible(true);
                    passwordField.setVisible(true);
                    passwordField.setText(""); // Để trống nếu chưa có tài khoản
                    passwordField.setToolTipText("Student has not registered an account yet.");
                    passwordLabel.setText("Account Password (N/A):");
                }
            }
            // --- Kết thúc phần mật khẩu ---

        } else {
            // Đặt giá trị mặc định cho chế độ Add nếu cần
            genderComboBox.setSelectedIndex(0);
        }
        pack(); // Gọi pack() sau khi ẩn/hiện component để điều chỉnh kích thước
    }

    /** Cấu hình các thuộc tính chung của JDialog. */
    private void configureDialog() {
        // pack() đã được gọi trong populateFields
        setMinimumSize(new Dimension(450, 500)); // Đặt kích thước tối thiểu
        setLocationRelativeTo(getOwner());      // Hiển thị dialog ở giữa cửa sổ cha
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Đóng dialog khi nhấn nút X
    }

    /**
     * Xử lý sự kiện khi nhấn nút Save.
     * Thu thập dữ liệu, kiểm tra tính hợp lệ, gọi Controller để lưu thông tin Student
     * và cập nhật mật khẩu (nếu có thay đổi và là Admin).
     */
    private void saveStudent() {
        // --- 1. Thu thập dữ liệu từ các trường nhập liệu ---
        String name = nameField.getText().trim();
        LocalDate dob = dobPicker.getDate();
        String gender = (String) genderComboBox.getSelectedItem();
        String address = addressField.getText().trim();
        String parentName = parentNameField.getText().trim();
        String phone = phoneField.getText().trim(); // Số điện thoại cũng là username
        String email = emailField.getText().trim();
        String newPassword = passwordField.getText(); // Lấy mật khẩu từ trường nhập (plain text)

        // --- 2. Kiểm tra tính hợp lệ (Validation) ---
        if (!ValidationUtils.isNotEmpty(name)) { UIUtils.showWarningMessage(this, "Validation Error", "Full Name cannot be empty."); nameField.requestFocusInWindow(); return; }
        if (!ValidationUtils.isNotEmpty(phone)) { UIUtils.showWarningMessage(this, "Validation Error", "Phone Number (Username) cannot be empty."); phoneField.requestFocusInWindow(); return; }
        if (!ValidationUtils.isValidPhoneNumber(phone)) { UIUtils.showWarningMessage(this, "Validation Error", "Invalid phone number format."); phoneField.requestFocusInWindow(); return; }
        if (ValidationUtils.isNotEmpty(email) && !ValidationUtils.isValidEmail(email)) { UIUtils.showWarningMessage(this, "Validation Error", "Invalid email address format."); emailField.requestFocusInWindow(); return; }
        // Thêm validation khác nếu cần (ngày sinh hợp lệ, ...)

        // --- 3. Tạo hoặc cập nhật đối tượng Student ---
        Student student = isEditMode ? studentToEdit : new Student();
        student.setFullName(name);
        student.setDateOfBirth(dob);
        student.setGender(gender);
        student.setAddress(address);
        student.setParentName(parentName);
        student.setPhone(phone); // Lưu SĐT
        student.setEmail(email);
        // ID sẽ được DAO xử lý khi thêm mới

        // --- 4. Gọi Controller để lưu thông tin Student ---
        boolean studentSaveSuccess;
        if (isEditMode) {
            studentSaveSuccess = controller.updateStudent(student);
        } else {
            studentSaveSuccess = controller.addStudent(student);
            // Nếu thêm thành công, student object bây giờ sẽ có ID
            // (Cần thiết nếu muốn cập nhật pass ngay sau khi thêm - nhưng luồng này không hợp lý lắm)
        }

        // --- 5. Xử lý cập nhật mật khẩu (Chỉ khi Edit và là Admin và có thay đổi) ---
        // Đảm bảo studentSaveSuccess là true trước khi cập nhật pass
        if (isEditMode && studentSaveSuccess && controller.isCurrentUserAdmin() && passwordField.isVisible()) {
            String currentPasswordInDB = controller.getPasswordForStudent(studentToEdit.getStudentId());

            // Chỉ cập nhật nếu mật khẩu mới khác mật khẩu cũ VÀ mật khẩu mới không rỗng
            // Hoặc nếu user cũ chưa có pass (currentPasswordInDB == null) và pass mới không rỗng
            boolean shouldUpdatePassword = ValidationUtils.isNotEmpty(newPassword) &&
                    !newPassword.equals(currentPasswordInDB);

            if (shouldUpdatePassword) {
                // Kiểm tra độ dài mật khẩu mới trước khi cập nhật
                if (!ValidationUtils.isValidPassword(newPassword)) {
                    UIUtils.showWarningMessage(this, "Password Error", "New password must be at least 6 characters long.");
                    passwordField.requestFocusInWindow();
                    // Không đóng dialog vì lưu student đã thành công nhưng pass lỗi
                    return; // Dừng ở đây
                } else {
                    // Gọi controller để cập nhật mật khẩu cho User liên kết
                    // Controller sẽ xử lý việc tìm User và gọi userDAO.update()
                    System.out.println("Attempting to update password for student ID: " + studentToEdit.getStudentId());
                    boolean passUpdateSuccess = controller.updatePasswordForStudent(studentToEdit.getStudentId(), newPassword);
                    // Thông báo thành công/thất bại đã được xử lý trong controller
                    // Nếu cập nhật pass lỗi sau khi student đã lưu thành công? => Cân nhắc UX
                }
            }
        }
        // --- Kết thúc xử lý mật khẩu ---

        // --- 6. Đóng Dialog nếu lưu thông tin Student thành công ---
        // (Việc cập nhật pass lỗi không ngăn đóng dialog ở đây, vì student đã được lưu)
        if (studentSaveSuccess) {
            dispose(); // Đóng cửa sổ dialog
        }
        // Controller sẽ gọi panel.refreshTable() sau khi add/update student thành công.
    }
}