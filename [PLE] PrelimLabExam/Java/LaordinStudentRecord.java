import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Stack;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

public class LaordinStudentRecord extends JFrame {

    // --- THEME COLORS ---
    private static final Color BG_COLOR = new Color(244, 244, 245);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(24, 24, 27);
    private static final Color TEXT_SECONDARY = new Color(113, 113, 122);
    private static final Color BORDER_COLOR = new Color(228, 228, 231);
    private static final Color DANGER_COLOR = new Color(239, 68, 68);
    
    // Notification Colors
    private static final Color TOAST_BG = new Color(24, 24, 27); // Standard Black
    private static final Color TOAST_TEXT = Color.WHITE;
    private static final Color SUCCESS_BG = new Color(34, 197, 94); // Modern Green

    // --- DATA & STATE ---
    private ArrayList<Student> studentData = new ArrayList<>();
    private Stack<State> undoStack = new Stack<>();
    private Stack<State> redoStack = new Stack<>();
    private final String CSV_FILE_NAME = "MOCK_DATA.csv";

    // --- COMPONENTS ---
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel lblSelectionCount;
    private JLabel lblTotalCount; 
    
    // Notification Components
    private JPanel notificationPanel;
    private JLabel lblNotification;
    private Timer notificationTimer;
    
    // Inputs
    private JTextField txtSearch;
    private JTextField txtID, txtName, txtLab1, txtLab2, txtLab3, txtPrelim, txtAtt;

    public LaordinStudentRecord() {
        setTitle("Records - Jared Wackyn Laordin [23-1270-536]");
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);
        setLayout(new BorderLayout(20, 20));

        // --- 1. HEADER PANEL ---
        JPanel headerPanel = new JPanel(new BorderLayout(0, 15));
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 20, 0, 20));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(BG_COLOR);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1));
        titleBlock.setBackground(BG_COLOR);
        JLabel lblTitle = new JLabel("STUDENT RECORDS");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(TEXT_PRIMARY);
        JLabel lblSub = new JLabel("System Admin: Jared Wackyn Laordin — 23-1270-536");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_SECONDARY);
        titleBlock.add(lblTitle);
        titleBlock.add(lblSub);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        toolbar.setBackground(BG_COLOR);
        
        JButton btnUndo = createButton("Undo", false); 
        JButton btnRedo = createButton("Redo", false);
        btnUndo.addActionListener(e -> undo());
        btnRedo.addActionListener(e -> redo());
        toolbar.add(btnUndo);
        toolbar.add(btnRedo);

        topRow.add(titleBlock, BorderLayout.WEST);
        topRow.add(toolbar, BorderLayout.EAST);

        // Search Bar
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(BG_COLOR);
        JLabel lblSearchIcon = new JLabel("Search: ");
        lblSearchIcon.setForeground(TEXT_SECONDARY);
        
        txtSearch = new JTextField();
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR),
            new EmptyBorder(8, 10, 8, 10)
        ));
        
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });

        searchPanel.add(lblSearchIcon, BorderLayout.WEST);
        searchPanel.add(txtSearch, BorderLayout.CENTER);

        headerPanel.add(topRow, BorderLayout.NORTH);
        headerPanel.add(searchPanel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. MAIN CONTENT ---
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        // --- TABLE CARD ---
        JPanel tableCard = createCardPanel();
        tableCard.setLayout(new BorderLayout());
        
        // UPDATED COLUMNS TO INCLUDE AVERAGE
        String[] columns = {
            "ID", "First Name", "Last Name", 
            "Lab 1 (/100)", "Lab 2 (/100)", "Lab 3 (/100)", 
            "Prelim (/100)", "Attend (/100)", "Average"
        };
        
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        table = new JTable(tableModel);
        styleTable(table);
        
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> updateFooterCounts());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int viewRow = table.getSelectedRow();
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    openEditDialog(modelRow);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        tableCard.add(scrollPane, BorderLayout.CENTER);
        
        // --- TABLE FOOTER (Total, Hint, Selection) ---
        JPanel tableFooter = new JPanel(new BorderLayout());
        tableFooter.setBackground(Color.WHITE);
        tableFooter.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        lblTotalCount = new JLabel("Total Records: 0");
        lblTotalCount.setForeground(TEXT_PRIMARY);
        lblTotalCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Hint Label in Center
        JLabel lblHint = new JLabel("Double-click a row to edit", SwingConstants.CENTER);
        lblHint.setForeground(Color.LIGHT_GRAY);
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        lblSelectionCount = new JLabel("0 selected");
        lblSelectionCount.setForeground(TEXT_SECONDARY);
        lblSelectionCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        tableFooter.add(lblTotalCount, BorderLayout.WEST);
        tableFooter.add(lblHint, BorderLayout.CENTER);
        tableFooter.add(lblSelectionCount, BorderLayout.EAST);
        tableCard.add(tableFooter, BorderLayout.SOUTH);

        mainPanel.add(tableCard);
        mainPanel.add(Box.createVerticalStrut(20)); 

        // --- INPUT CARD ---
        JPanel inputCard = createCardPanel();
        inputCard.setLayout(new BorderLayout(0, 15));

        JLabel lblManage = new JLabel("MANAGE RECORDS");
        lblManage.setFont(new Font("Segoe UI", Font.BOLD, 14));
        inputCard.add(lblManage, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(2, 1, 15, 15));
        formPanel.setBackground(Color.WHITE);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        row1.setBackground(Color.WHITE);
        txtID = createStyledTextField(12);
        txtName = createStyledTextField(25);
        row1.add(createInputWrapper("Student ID", txtID));
        row1.add(createInputWrapper("Full Name", txtName));

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        row2.setBackground(Color.WHITE);
        txtLab1 = createStyledTextField(5);
        txtLab2 = createStyledTextField(5);
        txtLab3 = createStyledTextField(5);
        txtPrelim = createStyledTextField(5);
        txtAtt = createStyledTextField(5);
        
        row2.add(createInputWrapper("Lab 1 (Max 100)", txtLab1));
        row2.add(createInputWrapper("Lab 2 (Max 100)", txtLab2));
        row2.add(createInputWrapper("Lab 3 (Max 100)", txtLab3));
        row2.add(createInputWrapper("Prelim (Max 100)", txtPrelim));
        row2.add(createInputWrapper("Attend (Max 100)", txtAtt));

        formPanel.add(row1);
        formPanel.add(row2);
        inputCard.add(formPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(Color.WHITE);
        
        JButton btnDelete = createButton("Delete Selected", true);
        JButton btnAdd = createButton("Add Record", false);
        
        // Custom styling for Add Button
        btnAdd.setBackground(Color.WHITE);
        btnAdd.setForeground(Color.BLACK);
        btnAdd.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.BLACK, 1), 
            new EmptyBorder(8, 15, 8, 15)
        ));
        
        btnDelete.addActionListener(e -> deleteSelected());
        btnAdd.addActionListener(e -> addRecord());

        actionPanel.add(btnDelete);
        actionPanel.add(btnAdd);
        inputCard.add(actionPanel, BorderLayout.SOUTH);

        mainPanel.add(inputCard);
        add(mainPanel, BorderLayout.CENTER);

        // --- NOTIFICATION PANEL ---
        notificationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        notificationPanel.setBackground(BG_COLOR);
        notificationPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        lblNotification = new JLabel(" ");
        lblNotification.setOpaque(true);
        lblNotification.setBackground(TOAST_BG);
        lblNotification.setForeground(TOAST_TEXT);
        lblNotification.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNotification.setBorder(new EmptyBorder(8, 20, 8, 20));
        lblNotification.setVisible(false);
        
        notificationPanel.add(lblNotification);
        add(notificationPanel, BorderLayout.SOUTH);

        loadCSVData();
    }

    // --- LOGIC: SHOW NOTIFICATION ---
    private void showNotification(String message) {
        showNotification(message, false);
    }
    
    private void showNotification(String message, boolean isSuccess) {
        lblNotification.setText(message);
        lblNotification.setBackground(isSuccess ? SUCCESS_BG : TOAST_BG);
        lblNotification.setVisible(true);
        
        if (notificationTimer != null && notificationTimer.isRunning()) {
            notificationTimer.stop();
        }
        
        notificationTimer = new Timer(4000, e -> {
            lblNotification.setVisible(false);
        });
        notificationTimer.setRepeats(false);
        notificationTimer.start();
    }

    // --- LOGIC: DATA PERSISTENCE ---
    private void saveCSVData() {
        File file = new File(CSV_FILE_NAME);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("StudentID,first_name,last_name,LAB WORK 1,LAB WORK 2,LAB WORK 3,PRELIM EXAM,ATTENDANCE GRADE");
            writer.newLine();
            for (Student s : studentData) {
                String line = String.format("%s,%s,%s,%d,%d,%d,%d,%d",
                    s.id, s.firstName, s.lastName, s.l1, s.l2, s.l3, s.pre, s.att
                );
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            showNotification("Error saving CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- LOGIC: CSV LOADING ---
    private void loadCSVData() {
        File file = new File(CSV_FILE_NAME);
        
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                parseAndLoad(br);
                showNotification("Loaded records from file", true);
                return;
            } catch (IOException e) {
                System.out.println("Error reading local file: " + e.getMessage());
            }
        } 
        
        InputStream is = getClass().getResourceAsStream("/" + CSV_FILE_NAME);
        if (is == null) is = getClass().getResourceAsStream(CSV_FILE_NAME);

        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                parseAndLoad(br);
                showNotification("Loaded records from internal resource", true);
            } catch (IOException e) {
                System.out.println("Error reading resource: " + e.getMessage());
            }
        } else {
            showNotification("MOCK_DATA.csv not found. Created empty list.");
        }
    }

    private void parseAndLoad(BufferedReader br) throws IOException {
        String line;
        boolean isFirstLine = true;
        
        while ((line = br.readLine()) != null) {
            if (isFirstLine && line.toLowerCase().contains("first_name")) { 
                isFirstLine = false; continue; 
            }
            String[] cols = line.split(",");
            if (cols.length >= 8) {
                addStudentToModel(new Student(
                    cols[0].trim(), cols[1].trim(), cols[2].trim(),
                    parseSafe(cols[3]), parseSafe(cols[4]), parseSafe(cols[5]),
                    parseSafe(cols[6]), parseSafe(cols[7])
                ));
            }
        }
        renderTable();
    }

    // --- LOGIC: FILTERING ---
    private void filter() {
        String text = txtSearch.getText();
        if (text.trim().length() == 0) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1, 2));
        updateFooterCounts();
    }

    // --- LOGIC: UPDATE FOOTER ---
    private void updateFooterCounts() {
        if (lblSelectionCount != null && table != null) {
            lblSelectionCount.setText(table.getSelectedRowCount() + " selected");
        }
        if (lblTotalCount != null && table != null) {
            lblTotalCount.setText("Total Records: " + table.getRowCount());
        }
    }

    // --- LOGIC: VALIDATION ---
    private String validateInput(int l1, int l2, int l3, int pre, int att) {
        if(l1 < 0 || l1 > 100) return "Lab 1 must be between 0 and 100.";
        if(l2 < 0 || l2 > 100) return "Lab 2 must be between 0 and 100.";
        if(l3 < 0 || l3 > 100) return "Lab 3 must be between 0 and 100.";
        if(pre < 0 || pre > 100) return "Prelim must be between 0 and 100.";
        if(att < 0 || att > 100) return "Attendance must be between 0 and 100.";
        return null;
    }

    // --- LOGIC: ACTIONS ---
    private void addRecord() {
        String id = txtID.getText().trim();
        String name = txtName.getText().trim();
        int l1 = parseSafe(txtLab1.getText());
        int l2 = parseSafe(txtLab2.getText());
        int l3 = parseSafe(txtLab3.getText());
        int pre = parseSafe(txtPrelim.getText());
        int att = parseSafe(txtAtt.getText());

        if (id.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in ID and Name.");
            return;
        }
        String error = validateInput(l1, l2, l3, pre, att);
        if(error != null) { JOptionPane.showMessageDialog(this, error); return; }

        String[] parts = name.split(" ");
        String last = parts.length > 1 ? parts[parts.length - 1] : "-";
        String first = parts.length > 1 ? name.substring(0, name.lastIndexOf(" ")) : name;

        saveState("Added record: " + first + " " + last);

        Student s = new Student(id, first, last, l1, l2, l3, pre, att);
        studentData.add(s);
        
        saveCSVData(); // SAVE TO FILE
        renderTable();
        clearInputs();
        showNotification("Added record: " + first + " " + last);
    }

    private void deleteSelected() {
        int[] viewRows = table.getSelectedRows();
        if (viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "No rows selected.");
            return;
        }

        StringBuilder names = new StringBuilder();
        ArrayList<Student> toRemove = new ArrayList<>();

        for (int viewRow : viewRows) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            Student s = studentData.get(modelRow);
            toRemove.add(s);
            names.append("• ").append(s.firstName).append(" ").append(s.lastName).append("\n");
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Delete " + viewRows.length + " record(s)?\n\n" + names.toString(),
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            saveState("Deleted " + viewRows.length + " record(s)");
            studentData.removeAll(toRemove);
            
            saveCSVData(); // SAVE TO FILE
            renderTable();
            showNotification("Deleted " + viewRows.length + " records");
        }
    }

    private void openEditDialog(int modelRow) {
        Student s = studentData.get(modelRow);
        
        JTextField eId = new JTextField(s.id); eId.setEditable(false);
        JTextField eFirst = new JTextField(s.firstName);
        JTextField eLast = new JTextField(s.lastName);
        JTextField eL1 = new JTextField(String.valueOf(s.l1));
        JTextField eL2 = new JTextField(String.valueOf(s.l2));
        JTextField eL3 = new JTextField(String.valueOf(s.l3));
        JTextField ePre = new JTextField(String.valueOf(s.pre));
        JTextField eAtt = new JTextField(String.valueOf(s.att));

        Object[] message = {
            "ID:", eId, "First Name:", eFirst, "Last Name:", eLast,
            "Lab 1:", eL1, "Lab 2:", eL2, "Lab 3:", eL3, "Prelim:", ePre, "Attend:", eAtt
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Edit Record", JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            int l1 = parseSafe(eL1.getText());
            int l2 = parseSafe(eL2.getText());
            int l3 = parseSafe(eL3.getText());
            int pre = parseSafe(ePre.getText());
            int att = parseSafe(eAtt.getText());
            
            String error = validateInput(l1, l2, l3, pre, att);
            if(error != null) { JOptionPane.showMessageDialog(this, error); return; }

            ArrayList<String> changes = new ArrayList<>();
            if(!s.firstName.equals(eFirst.getText())) changes.add("First Name");
            if(!s.lastName.equals(eLast.getText())) changes.add("Last Name");
            if(s.l1 != l1) changes.add("Lab 1");
            if(s.l2 != l2) changes.add("Lab 2");
            if(s.l3 != l3) changes.add("Lab 3");
            if(s.pre != pre) changes.add("Prelim");
            if(s.att != att) changes.add("Attend");
            
            if(changes.isEmpty()) { showNotification("No changes made."); return; }

            String changeStr = "Updated " + s.firstName + ": " + String.join(", ", changes);
            saveState(changeStr);

            s.firstName = eFirst.getText(); s.lastName = eLast.getText();
            s.l1 = l1; s.l2 = l2; s.l3 = l3; s.pre = pre; s.att = att;
            
            saveCSVData(); // SAVE TO FILE
            renderTable();
            showNotification(changeStr);
        }
    }

    // --- LOGIC: UNDO / REDO ---
    private void saveState(String action) {
        ArrayList<Student> copy = new ArrayList<>();
        for (Student s : studentData) copy.add(s.copy());
        
        undoStack.push(new State(copy, action));
        if (undoStack.size() > 50) undoStack.remove(0);
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty()) { showNotification("Nothing to undo"); return; }
        State lastState = undoStack.pop();
        
        ArrayList<Student> currentCopy = new ArrayList<>();
        for (Student s : studentData) currentCopy.add(s.copy());
        redoStack.push(new State(currentCopy, lastState.action));

        studentData = lastState.data;
        saveCSVData(); // SAVE TO FILE
        renderTable();
        showNotification("Undid: " + lastState.action);
    }

    private void redo() {
        if (redoStack.isEmpty()) { showNotification("Nothing to redo"); return; }
        State futureState = redoStack.pop();
        
        ArrayList<Student> currentCopy = new ArrayList<>();
        for (Student s : studentData) currentCopy.add(s.copy());
        undoStack.push(new State(currentCopy, futureState.action));

        studentData = futureState.data;
        saveCSVData(); // SAVE TO FILE
        renderTable();
        showNotification("Redid: " + futureState.action);
    }

    // --- HELPERS ---
    private int parseSafe(String text) {
        try { return Integer.parseInt(text.trim()); } catch (Exception e) { return 0; }
    }

    private void addStudentToModel(Student s) { studentData.add(s); }

    // --- UPDATED RENDER TABLE WITH CALCULATION ---
    private void renderTable() {
        tableModel.setRowCount(0);
        for (Student s : studentData) {
            // Calculate Average: (Sum of 5 components) / 5.0
            double avg = (s.l1 + s.l2 + s.l3 + s.pre + s.att) / 5.0;
            // Format to 2 decimal places
            String avgStr = String.format("%.2f", avg);
            
            tableModel.addRow(new Object[]{
                s.id, s.firstName, s.lastName, 
                s.l1, s.l2, s.l3, s.pre, s.att, 
                avgStr // Add average to row
            });
        }
        updateFooterCounts();
    }

    private void clearInputs() {
        txtID.setText(""); txtName.setText("");
        txtLab1.setText(""); txtLab2.setText(""); txtLab3.setText(""); 
        txtPrelim.setText(""); txtAtt.setText("");
    }

    // --- UI FACTORIES ---
    private JPanel createCardPanel() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR, 1), new EmptyBorder(20, 20, 20, 20)));
        return p;
    }

    private JPanel createInputWrapper(String labelText, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(Color.WHITE);
        JLabel l = new JLabel(labelText.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_SECONDARY);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JTextField createStyledTextField(int cols) {
        JTextField tf = new JTextField(cols);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR), new EmptyBorder(5, 8, 5, 8)));
        return tf;
    }

    private JButton createButton(String text, boolean isDanger) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setBackground(Color.WHITE);
        b.setForeground(isDanger ? DANGER_COLOR : TEXT_PRIMARY);
        b.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR), new EmptyBorder(8, 15, 8, 15)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(35);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(new Color(244, 244, 245));
        table.setSelectionForeground(Color.BLACK);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(250, 250, 250));
        header.setForeground(TEXT_SECONDARY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LaordinStudentRecord().setVisible(true));
    }

    // --- INNER CLASSES ---
    static class Student {
        String id, firstName, lastName;
        int l1, l2, l3, pre, att;

        public Student(String id, String f, String l, int l1, int l2, int l3, int pre, int att) {
            this.id = id; this.firstName = f; this.lastName = l;
            this.l1 = l1; this.l2 = l2; this.l3 = l3; this.pre = pre; this.att = att;
        }
        public Student copy() { return new Student(id, firstName, lastName, l1, l2, l3, pre, att); }
    }

    static class State {
        ArrayList<Student> data;
        String action;
        public State(ArrayList<Student> data, String action) { this.data = data; this.action = action; }
    }
}