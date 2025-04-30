package com.eduzk.view.components;

import com.eduzk.utils.DateUtils;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

public class CustomDatePicker extends JPanel {

    private JSpinner dateSpinner;
    private SpinnerDateModel dateModel;

    public CustomDatePicker() {
        this(LocalDate.now()); // Default to today
    }

    public CustomDatePicker(LocalDate initialDate) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0)); // No gaps

        dateModel = new SpinnerDateModel();
        dateSpinner = new JSpinner(dateModel);

        // Use a standard date editor format
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy");
        dateSpinner.setEditor(dateEditor);

        // Set initial value
        if (initialDate != null) {
            setDate(initialDate);
        } else {
            setDate(LocalDate.now());
        }


        // Make spinner slightly wider
        Dimension d = dateSpinner.getPreferredSize();
        d.width = 100;
        dateSpinner.setPreferredSize(d);


        add(dateSpinner);
    }

    public LocalDate getDate() {
        Date selectedDate = dateModel.getDate();
        if (selectedDate == null) {
            return null;
        }
        // Convert java.util.Date to java.time.LocalDate
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        return LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    public void setDate(LocalDate localDate) {
        if (localDate == null) {
            // Set spinner to null or keep current? Decide behavior.
            // Setting to current date as a fallback
            localDate = LocalDate.now();
        }
        // Convert java.time.LocalDate to java.util.Date
        Calendar cal = Calendar.getInstance();
        cal.clear(); // Clear existing fields
        cal.set(localDate.getYear(), localDate.getMonthValue() - 1, localDate.getDayOfMonth());
        Date date = cal.getTime();
        dateModel.setValue(date);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        dateSpinner.setEnabled(enabled);
    }

    // Convenience method to get the formatted string directly
    public String getFormattedDate() {
        return DateUtils.formatDate(getDate());
    }
}