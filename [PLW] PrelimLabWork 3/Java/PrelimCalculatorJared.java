import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;

public class PrelimCalculatorJared extends JFrame {

    private final Color BG_COLOR = new Color(255, 255, 255);
    private final Color TEXT_PRIMARY = new Color(24, 24, 27);
    private final Color TEXT_SECONDARY = new Color(113, 113, 122);
    private final Color ACCENT_BLACK = new Color(0, 0, 0);
    private final Color BORDER_COLOR = new Color(228, 228, 231);
    private final Color SUCCESS_BG = new Color(220, 252, 231);
    private final Color SUCCESS_TEXT = new Color(22, 101, 52);
    private final Color WARNING_BG = new Color(254, 249, 195);
    private final Color WARNING_TEXT = new Color(133, 77, 14);
    private final Color ERROR_BG = new Color(254, 242, 242);
    private final Color ERROR_TEXT = new Color(185, 28, 28);

    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 24);
    private final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private final Font RESULT_LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font RESULT_VALUE_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private final Font INPUT_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    // Inputs
    private JTextField nameField;
    private JComboBox<String> week1, week2, week3, week4, week5;
    private JTextField lab1Field, lab2Field, lab3Field;

    // Outputs
    private JLabel resAttendance, resAttendancePercent;
    private JLabel resLab1, resLab2, resLab3;
    private JLabel resLabAvg, resClassStanding;
    private JPanel passingBox, excellentBox;
    private JLabel passingTitle, passingValue;
    private JLabel excellentTitle, excellentValue;
    private JTextArea remarksText;

    private DecimalFormat df = new DecimalFormat("#.##");
    private final String DB_FILE;

    // Constants
    private static final double PRELIM_EXAM_WEIGHT = 0.30;
    private static final double CLASS_STANDING_WEIGHT = 0.70;
    private static final double ATTENDANCE_WEIGHT = 0.40;
    private static final double LAB_WORK_WEIGHT = 0.60;
    private static final double PASSING_GRADE = 75.0;
    private static final double EXCELLENT_GRADE = 100.0;
    private static final int TOTAL_MEETINGS = 5;

    public PrelimCalculatorJared() {
        // Dynamic path resolution
        // Dynamic path resolution
        String cwd = System.getProperty("user.dir");
        if (cwd.endsWith("Java")) {
            DB_FILE = "database/student_records.csv";
        } else if (cwd.endsWith("PROGRAMMING")) {
            // Running from grand-parent directory
            DB_FILE = "[PLW] PrelimLabWork 3/Java/database/student_records.csv";
        } else {
            // Assume we are in the parent directory ([PLW] PrelimLabWork 3)
            DB_FILE = "Java/database/student_records.csv";
        }

        setTitle("Prelim Grade Calculator & Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(BG_COLOR);

        // Init Database File if not exists
        initDatabase();

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BG_COLOR);
        mainContainer.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(new EmptyBorder(0, 0, 25, 0));

        JLabel titleLabel = new JLabel("Prelim Grade Calculator");
        titleLabel.setFont(HEADER_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel("Calculate grades, manage records, and export data.");
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(TEXT_SECONDARY);

        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content
        JPanel contentGrid = new JPanel(new GridLayout(1, 2, 40, 0));
        contentGrid.setBackground(BG_COLOR);
        contentGrid.add(createInputSection());
        contentGrid.add(createResultSection());

        mainContainer.add(contentGrid, BorderLayout.CENTER);

        // Toolbar (Bottom) for Database Actions
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.setBackground(BG_COLOR);
        toolbar.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton viewDbBtn = createSecondaryButton("View Database");
        viewDbBtn.addActionListener(e -> showDatabaseDialog());

        JButton clearDbBtn = createSecondaryButton("Clear DB");
        clearDbBtn.setBackground(new Color(254, 242, 242)); // ERROR_BG
        clearDbBtn.setForeground(new Color(185, 28, 28)); // ERROR_TEXT
        clearDbBtn.addActionListener(e -> clearDatabase());

        JButton exportBtn = createSecondaryButton("Export CSV");
        exportBtn.addActionListener(e -> exportCSV());

        toolbar.add(viewDbBtn);
        toolbar.add(clearDbBtn);
        toolbar.add(exportBtn);
        mainContainer.add(toolbar, BorderLayout.SOUTH);

        add(mainContainer);
        setSize(1000, 870);
        setLocationRelativeTo(null);
    }

    private void clearDatabase() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete ALL student records?\nThis cannot be undone.",
                "Confirm Clear Database",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            File f = new File(DB_FILE);
            if (f.exists()) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                    // Write header only
                    pw.println("Timestamp,Name,Attendance,Lab1,Lab2,Lab3,Average,ClassStanding,RequiredExam,Status,WeeklyAttendance");
                    JOptionPane.showMessageDialog(this, "Database cleared successfully.");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error clearing database: " + e.getMessage());
                }
            }
        }
    }

    private void initDatabase() {
        File f = new File(DB_FILE);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs(); // Ensure directory exists
        }
        if (!f.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("Timestamp,Name,Attendance,Lab1,Lab2,Lab3,Average,ClassStanding,RequiredExam,Status,WeeklyAttendance");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveRecord(String name, int att, double l1, double l2, double l3, String avg, String cs, String req,
            String status, String weeklyAttendance) {
        File f = new File(DB_FILE);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs(); // Ensure directory exists
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            // CSV Format: Time, Name, Att, L1, L2, L3, Avg, CS, Req, Status, WeeklyAttendance
            pw.printf("%s,%s,%d,%.2f,%.2f,%.2f,%s,%s,%s,%s,%s%n",
                    timestamp, name, att, l1, l2, l3, avg, cs, req, status, weeklyAttendance);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving to database: " + e.getMessage());
        }
    }

    private JPanel createInputSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);

        JLabel header = new JLabel("Student Information");
        header.setFont(SECTION_FONT);
        header.setForeground(TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(header);
        panel.add(Box.createVerticalStrut(20));

        // Input Fields
        panel.add(createInputGroup("Student Name", "", nameField = createField(false, 200)));
        panel.add(Box.createVerticalStrut(15));
        
        // Attendance section with 5 weeks
        JLabel attendanceLabel = new JLabel("Weekly Attendance");
        attendanceLabel.setFont(SECTION_FONT);
        attendanceLabel.setForeground(TEXT_PRIMARY);
        attendanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(attendanceLabel);
        panel.add(Box.createVerticalStrut(10));
        
        // Create week comboboxes in a grid
        JPanel weekPanel = new JPanel(new GridLayout(5, 2, 10, 8));
        weekPanel.setBackground(BG_COLOR);
        weekPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        weekPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        String[] options = {"Select", "Present", "Absent", "Excused"};
        
        week1 = createWeekComboBox(options);
        week2 = createWeekComboBox(options);
        week3 = createWeekComboBox(options);
        week4 = createWeekComboBox(options);
        week5 = createWeekComboBox(options);
        
        weekPanel.add(createWeekLabel("Week 1:"));
        weekPanel.add(week1);
        weekPanel.add(createWeekLabel("Week 2:"));
        weekPanel.add(week2);
        weekPanel.add(createWeekLabel("Week 3:"));
        weekPanel.add(week3);
        weekPanel.add(createWeekLabel("Week 4:"));
        weekPanel.add(week4);
        weekPanel.add(createWeekLabel("Week 5:"));
        weekPanel.add(week5);
        
        panel.add(weekPanel);
        panel.add(Box.createVerticalStrut(20));
        
        panel.add(createInputGroup("Lab Work 1", "/ 100", lab1Field = createField(false, 100)));
        panel.add(Box.createVerticalStrut(15));
        panel.add(createInputGroup("Lab Work 2", "/ 100", lab2Field = createField(false, 100)));
        panel.add(Box.createVerticalStrut(15));
        panel.add(createInputGroup("Lab Work 3", "/ 100", lab3Field = createField(false, 100)));
        panel.add(Box.createVerticalStrut(25));

        // Buttons
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        btnPanel.setBackground(BG_COLOR);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JButton calcBtn = new JButton("Calculate & Save") {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isPressed())
                    g.setColor(ACCENT_BLACK.darker());
                else
                    g.setColor(ACCENT_BLACK);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
            }
        };
        calcBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        calcBtn.setForeground(Color.WHITE);
        calcBtn.setContentAreaFilled(false);
        calcBtn.setBorderPainted(false);
        calcBtn.setFocusPainted(false);
        calcBtn.setPreferredSize(new Dimension(0, 50));
        calcBtn.addActionListener(e -> calculateAndSave());

        JButton clearBtn = createSecondaryButton("Clear");
        clearBtn.setPreferredSize(new Dimension(0, 50));
        clearBtn.addActionListener(e -> clearForm());

        btnPanel.add(calcBtn);
        btnPanel.add(clearBtn);
        panel.add(btnPanel);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(Color.WHITE);
        btn.setForeground(TEXT_PRIMARY);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(8, 15, 8, 15)));
        btn.setFocusPainted(false);
        return btn;
    }

    private JComboBox<String> createWeekComboBox(String[] options) {
        JComboBox<String> combo = new JComboBox<>(options);
        combo.setFont(INPUT_FONT);
        combo.setPreferredSize(new Dimension(100, 40));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXT_PRIMARY);
        return combo;
    }

    private JLabel createWeekLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private JPanel createInputGroup(String labelText, String suffixText, JTextField field) {
        JPanel group = new JPanel(new BorderLayout(5, 5));
        group.setBackground(BG_COLOR);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_PRIMARY);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_COLOR);

        if (!suffixText.isEmpty()) {
            JLabel suffix = new JLabel(suffixText);
            suffix.setForeground(TEXT_SECONDARY);
            suffix.setBorder(new EmptyBorder(0, 10, 0, 0));
            wrapper.add(suffix, BorderLayout.EAST);
        }

        wrapper.add(field, BorderLayout.CENTER);
        wrapper.setBorder(new EmptyBorder(0, 0, 0, 5));

        group.add(label, BorderLayout.NORTH);
        group.add(wrapper, BorderLayout.CENTER);

        return group;
    }

    private JTextField createField(boolean isAttendance, int width) {
        JTextField field = new JTextField();
        field.setFont(INPUT_FONT);
        field.setPreferredSize(new Dimension(width, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 10, 5, 10)));

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT_BLACK, 1),
                        new EmptyBorder(5, 10, 5, 10)));
            }

            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1),
                        new EmptyBorder(5, 10, 5, 10)));
            }
        });

        // Add document filter only for numeric fields (attendance/labs)
        if (width <= 100) {
            ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                        throws BadLocationException {
                    if (isValid(string, fb.getDocument().getText(0, fb.getDocument().getLength()), offset,
                            isAttendance))
                        super.insertString(fb, offset, string, attr);
                }

                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    if (isValid(text, fb.getDocument().getText(0, fb.getDocument().getLength()), offset, isAttendance))
                        super.replace(fb, offset, length, text, attrs);
                }
            });
        }
        return field;
    }

    private boolean isValid(String change, String current, int offset, boolean isAtt) {
        if (change == null)
            return true;
        try {
            String newVal = current.substring(0, offset) + change + current.substring(offset);
            if (newVal.isEmpty())
                return true;
            if (newVal.equals(".") && !isAtt)
                return true;
            double val = Double.parseDouble(newVal);
            if (isAtt)
                return val >= 0 && val <= 4 && !newVal.contains(".");
            return val >= 0 && val <= 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private JPanel createResultSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);

        JLabel header = new JLabel("Grade Breakdown");
        header.setFont(SECTION_FONT);
        header.setForeground(TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(header);
        panel.add(Box.createVerticalStrut(20));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BG_COLOR);
        listPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(15, 15, 15, 15)));
        listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        addResultRow(listPanel, "Attendance (40%):", resAttendance = createResLabel("-"));
        addResultRow(listPanel, "Percentage:", resAttendancePercent = createResLabel("-"));
        listPanel.add(createSeparator());
        addResultRow(listPanel, "Lab 1:", resLab1 = createResLabel("-"));
        addResultRow(listPanel, "Lab 2:", resLab2 = createResLabel("-"));
        addResultRow(listPanel, "Lab 3:", resLab3 = createResLabel("-"));
        listPanel.add(createSeparator());
        addResultRow(listPanel, "Lab Average (60%):", resLabAvg = createResLabel("-"));

        JPanel csRow = new JPanel(new BorderLayout());
        csRow.setBackground(BG_COLOR);
        csRow.setBorder(new EmptyBorder(3, 0, 3, 0));
        JLabel csLbl = new JLabel("Class Standing (70%):");
        csLbl.setFont(RESULT_LABEL_FONT);
        csLbl.setForeground(TEXT_SECONDARY);
        resClassStanding = new JLabel("-");
        resClassStanding.setFont(RESULT_VALUE_FONT);
        resClassStanding.setForeground(TEXT_PRIMARY);
        csRow.add(csLbl, BorderLayout.WEST);
        csRow.add(resClassStanding, BorderLayout.EAST);
        listPanel.add(csRow);

        panel.add(listPanel);
        panel.add(Box.createVerticalStrut(20));

        JPanel reqGrid = new JPanel(new GridLayout(1, 2, 15, 0));
        reqGrid.setBackground(BG_COLOR);
        reqGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        passingBox = createReqBox(SUCCESS_BG);
        passingTitle = createReqTitle("TO PASS (75)", SUCCESS_TEXT);
        passingValue = createReqValue(SUCCESS_TEXT);
        passingBox.add(passingTitle);
        passingBox.add(Box.createVerticalStrut(5));
        passingBox.add(passingValue);
        reqGrid.add(passingBox);

        excellentBox = createReqBox(WARNING_BG);
        excellentTitle = createReqTitle("EXCELLENT (100)", WARNING_TEXT);
        excellentValue = createReqValue(WARNING_TEXT);
        excellentBox.add(excellentTitle);
        excellentBox.add(Box.createVerticalStrut(5));
        excellentBox.add(excellentValue);
        reqGrid.add(excellentBox);
        reqGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        panel.add(reqGrid);
        panel.add(Box.createVerticalStrut(20));

        remarksText = new JTextArea(
                "To pass the subject with a grade of 75, you need to score at least - on the Prelim Exam.", 5, 20);
        remarksText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        remarksText.setMargin(new Insets(10, 5, 10, 5));
        remarksText.setBackground(BG_COLOR);
        remarksText.setLineWrap(true);
        remarksText.setWrapStyleWord(true);
        remarksText.setEditable(false);
        remarksText.setForeground(TEXT_PRIMARY);
        remarksText.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(remarksText);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // --- Logic & Database Methods ---

    private void calculateAndSave() {
        try {
            if (nameField.getText().trim().isEmpty() ||
                    lab1Field.getText().isEmpty() || lab2Field.getText().isEmpty() || lab3Field.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields (including Name and all attendance selections).");
                return;
            }

            // Check if all weeks are selected
            if (week1.getSelectedIndex() == 0 || week2.getSelectedIndex() == 0 || 
                week3.getSelectedIndex() == 0 || week4.getSelectedIndex() == 0 || week5.getSelectedIndex() == 0) {
                JOptionPane.showMessageDialog(this, "Please select attendance status for all 5 weeks.");
                return;
            }

            String name = nameField.getText().trim();
            
            // Get attendance for each week
            String[] weeks = {
                (String) week1.getSelectedItem(),
                (String) week2.getSelectedItem(),
                (String) week3.getSelectedItem(),
                (String) week4.getSelectedItem(),
                (String) week5.getSelectedItem()
            };
            
            // Count present and absent (Excused counts as present)
            int presentCount = 0;
            int absentCount = 0;
            
            for (String week : weeks) {
                if (week.equals("Present") || week.equals("Excused")) {
                    presentCount++;
                } else if (week.equals("Absent")) {
                    absentCount++;
                }
            }
            
            // Check for late enrollee: absent in weeks 1&2 but present in 3,4,5
            boolean isLateEnrollee = false;
            String week1Status = (String) week1.getSelectedItem();
            String week2Status = (String) week2.getSelectedItem();
            String week3Status = (String) week3.getSelectedItem();
            String week4Status = (String) week4.getSelectedItem();
            String week5Status = (String) week5.getSelectedItem();
            
            if ((week1Status.equals("Absent") || week2Status.equals("Absent")) &&
                !week3Status.equals("Absent") && !week4Status.equals("Absent") && !week5Status.equals("Absent")) {
                isLateEnrollee = true;
            }
            
            double l1 = Double.parseDouble(lab1Field.getText());
            double l2 = Double.parseDouble(lab2Field.getText());
            double l3 = Double.parseDouble(lab3Field.getText());

            // Display Logic
            resAttendance.setText(presentCount + " / 5");
            double attPct = (presentCount / (double) TOTAL_MEETINGS) * 100;
            resAttendancePercent.setText(df.format(attPct) + "%");
            resLab1.setText(df.format(l1));
            resLab2.setText(df.format(l2));
            resLab3.setText(df.format(l3));

            String reqExamStr = "-";
            String statusStr = "Pending";
            String weeklyAttendance = String.format("W1:%s,W2:%s,W3:%s,W4:%s,W5:%s", 
                week1Status, week2Status, week3Status, week4Status, week5Status);

            // AUTO-FAIL: If 4 or more absences OR (less than 2 present AND not a late enrollee)
            if (absentCount >= 4 || (presentCount < 2 && !isLateEnrollee)) {
                // FAIL scenario
                resLabAvg.setText("-");
                resClassStanding.setText("-");
                updateBox(passingBox, passingTitle, passingValue, ERROR_BG, ERROR_TEXT, "FAIL");
                updateBox(excellentBox, excellentTitle, excellentValue, ERROR_BG, ERROR_TEXT, "FAIL");
                
                if (absentCount >= 4) {
                    remarksText.setText("Automatic Failure: 4 or more absences detected. Attendance does not meet minimum requirements.");
                } else {
                    remarksText.setText("Automatic Failure: Less than 50% attendance and not a late enrollee.");
                }
                remarksText.setForeground(ERROR_TEXT);
                statusStr = "Failed (Attendance)";
            } else {
                double avg = (l1 + l2 + l3) / 3.0;
                resLabAvg.setText(df.format(avg));

                double cs = (attPct * ATTENDANCE_WEIGHT) + (avg * LAB_WORK_WEIGHT);
                resClassStanding.setText(df.format(cs));

                double reqPass = Math.ceil((PASSING_GRADE - (cs * CLASS_STANDING_WEIGHT)) / PRELIM_EXAM_WEIGHT);
                double reqExc = Math.ceil((EXCELLENT_GRADE - (cs * CLASS_STANDING_WEIGHT)) / PRELIM_EXAM_WEIGHT);

                updatePassingBox(reqPass, reqExc);
                updateExcellentBox(reqExc);

                reqExamStr = (reqPass <= 0) ? "Passed" : (reqPass > 100 ? "Impossible" : String.valueOf((int) reqPass));
                statusStr = (reqPass <= 0) ? "Passed" : "Ongoing";
            }

            // Save to DB
            saveRecord(name, presentCount, l1, l2, l3, resLabAvg.getText(), resClassStanding.getText(), reqExamStr, statusStr, weeklyAttendance);
            JOptionPane.showMessageDialog(this, "Grades Calculated & Saved for: " + name);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid numbers. Please check your inputs.");
        }
    }

    private void showDatabaseDialog() {
        JDialog d = new JDialog(this, "Student Database Records", true);
        d.setSize(1200, 500);
        d.setLocationRelativeTo(this);

        DefaultTableModel model = new DefaultTableModel();
        // Load columns
        String[] columns = { "Timestamp", "Name", "Att", "Lab1", "Lab2", "Lab3", "Avg", "CS", "Required", "Status", "Weekly" };
        for (String c : columns)
            model.addColumn(c);

        // Load data
        try (BufferedReader br = new BufferedReader(new FileReader(DB_FILE))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                } // Skip header
                model.addRow(line.split(","));
            }
        } catch (IOException e) {
            // model.addRow(new String[]{"No records found or error reading file."});
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        d.add(new JScrollPane(table));

        d.setVisible(true);
    }

    private void exportCSV() {
        // Use FileDialog (Native) instead of JFileChooser (Swing) to avoid Windows L&F
        // NPE bugs
        FileDialog fd = new FileDialog(this, "Export Database to CSV", FileDialog.SAVE);
        fd.setFile("PrelimGrades_Export.csv");
        fd.setVisible(true);

        String filename = fd.getFile();
        String directory = fd.getDirectory();

        if (filename != null && directory != null) {
            File dest = new File(directory, filename);
            try {
                // Copy DB file to Dest
                BufferedReader br = new BufferedReader(new FileReader(DB_FILE));
                PrintWriter pw = new PrintWriter(new FileWriter(dest));

                String line;
                while ((line = br.readLine()) != null) {
                    pw.println(line);
                }

                pw.close();
                br.close();
                JOptionPane.showMessageDialog(this,
                        "Top-tier CSV Export Successful!\nSaved to: " + dest.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Export Failed: " + e.getMessage());
            }
        }
    }

    // --- Helper UI Methods ---

    private void updateBox(JPanel box, JLabel title, JLabel val, Color bg, Color txtColor, String text) {
        box.setBackground(bg);
        title.setForeground(txtColor);
        val.setForeground(txtColor);
        val.setText(text);
    }

    private void updatePassingBox(double val, double reqExc) {
        if (val <= 0) {
            updateBox(passingBox, passingTitle, passingValue, SUCCESS_BG, SUCCESS_TEXT, "Passed");
            StringBuilder remarks = new StringBuilder("Congratulations! You have already secured a passing grade.");
            if (reqExc > 100) {
                remarks.append(" However, it is impossible to achieve an Excellent grade (100).");
            }
            remarksText.setText(remarks.toString());
            remarksText.setForeground(SUCCESS_TEXT);
        } else if (val > 100) {
            updateBox(passingBox, passingTitle, passingValue, ERROR_BG, ERROR_TEXT, "N/A");
            remarksText.setText("It is impossible to reach the passing grade with the current class standing.");
            remarksText.setForeground(ERROR_TEXT);
        } else {
            int iVal = (int) Math.ceil(val);
            updateBox(passingBox, passingTitle, passingValue, SUCCESS_BG, SUCCESS_TEXT, String.valueOf(iVal));
            
            // Calculate CS from the displayed values
            String csText = resClassStanding.getText();
            double cs = Double.parseDouble(csText);
            
            // Calculate projected grade with exam score of 100
            double projectedGrade = (cs * CLASS_STANDING_WEIGHT) + (100 * PRELIM_EXAM_WEIGHT);
            
            StringBuilder remarks = new StringBuilder();
            remarks.append("Current Standing: ").append(df.format(cs)).append(" - ");
            remarks.append("You have not yet passed. Your grade depends on your exam performance. ");
            remarks.append("With an exam score of 100, your Prelim grade would be ").append(df.format(projectedGrade)).append(". ");
            remarks.append("To pass (75), you need at least ").append(iVal).append(" on the Exam.");
            if (reqExc > 100) {
                remarks.append(" It is impossible to achieve an Excellent grade (100).");
            }
            remarksText.setText(remarks.toString());
            remarksText.setForeground(TEXT_PRIMARY);
        }
    }

    private void updateExcellentBox(double val) {
        if (val <= 0)
            updateBox(excellentBox, excellentTitle, excellentValue, WARNING_BG, WARNING_TEXT, "Secured");
        else
            updateBox(excellentBox, excellentTitle, excellentValue, WARNING_BG, WARNING_TEXT,
                    String.valueOf((int) Math.ceil(val)));
    }

    private void clearForm() {
        nameField.setText("");
        lab1Field.setText("");
        lab2Field.setText("");
        lab3Field.setText("");
        resAttendance.setText("-");
        resAttendancePercent.setText("-");
        resLab1.setText("-");
        resLab2.setText("-");
        resLab3.setText("-");
        resLabAvg.setText("-");
        resClassStanding.setText("-");

        updateBox(passingBox, passingTitle, passingValue, SUCCESS_BG, SUCCESS_TEXT, "-");
        updateBox(excellentBox, excellentTitle, excellentValue, WARNING_BG, WARNING_TEXT, "-");
        remarksText.setText("Enter grades to see requirements.");
        remarksText.setForeground(TEXT_PRIMARY);
        nameField.requestFocus();
    }

    // -- Component Creations (Same as before mostly) --
    private void addResultRow(JPanel p, String label, JLabel valLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_COLOR);
        row.setBorder(new EmptyBorder(3, 0, 3, 0));
        JLabel lbl = new JLabel(label);
        lbl.setFont(RESULT_LABEL_FONT);
        lbl.setForeground(TEXT_SECONDARY);
        row.add(lbl, BorderLayout.WEST);
        row.add(valLabel, BorderLayout.EAST);
        p.add(row);
    }

    private JLabel createResLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(RESULT_VALUE_FONT);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    private JSeparator createSeparator() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDER_COLOR);
        s.setBackground(BG_COLOR);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        return s;
    }

    private JPanel createReqBox(Color bg) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bg);
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        return p;
    }

    private JLabel createReqTitle(String text, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(c);
        return l;
    }

    private JLabel createReqValue(Color c) {
        JLabel l = new JLabel("-");
        l.setFont(new Font("Segoe UI", Font.BOLD, 22));
        l.setForeground(c);
        return l;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new PrelimCalculatorJared().setVisible(true);
        });
    }
}
