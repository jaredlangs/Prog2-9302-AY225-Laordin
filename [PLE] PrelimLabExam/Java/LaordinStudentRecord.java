/*
 * Programmer: Jared Wackyn Laordin [23-1270-536]
 * Project: Student Record System - Java 
 */

import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class LaordinStudentRecord extends JFrame {
    
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtID, txtName, txtLab1, txtLab2, txtLab3, txtPrelim, txtAttendance;

    // --- THEME COLORS (Red & Black) ---
    private final Color COLOR_BG = new Color(18, 18, 18);
    private final Color COLOR_PANEL = new Color(30, 30, 30);
    private final Color COLOR_ACCENT = new Color(180, 0, 0);
    private final Color COLOR_TEXT = new Color(240, 240, 240);
    private final Font FONT_MAIN = new Font("Consolas", Font.PLAIN, 12); 

    public LaordinStudentRecord() {
        // --- 1. Identity & Frame Setup ---
        this.setTitle("Records - Jared Wackyn Laordin [23-1270-536]");
        this.setSize(1000, 700); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(COLOR_BG);

        // --- 2. HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_BG);
        headerPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel lblTitle = new JLabel("S T U D E N T   R E C O R D S   [F U L L]");
        lblTitle.setFont(new Font("Consolas", Font.BOLD, 24));
        lblTitle.setForeground(COLOR_ACCENT);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(lblTitle, BorderLayout.CENTER);
        this.add(headerPanel, BorderLayout.NORTH);

        // --- 3. Table Setup (8 Columns) ---
        String[] columns = {"ID", "First Name", "Last Name", "Lab 1", "Lab 2", "Lab 3", "Prelim", "Attendance"};
        
        model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= 3) return Integer.class;
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };

        table = new JTable(model);
        styleTable(table);
        table.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(COLOR_BG);
        scrollPane.setBorder(new LineBorder(COLOR_ACCENT, 1));
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(COLOR_BG);
        tableContainer.setBorder(new EmptyBorder(0, 15, 0, 15));
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        this.add(tableContainer, BorderLayout.CENTER);

        // --- 4. Input Panel ---
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 1, 5, 5)); 
        inputPanel.setBackground(COLOR_PANEL);
        inputPanel.setBorder(new LineBorder(COLOR_ACCENT, 1));

        // Row 1
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row1.setBackground(COLOR_PANEL);
        txtID = createStyledField(9); 
        txtName = createStyledField(20);
        JButton btnAdd = createStyledButton("ADD RECORD");
        JButton btnDelete = createStyledButton("DELETE SELECTED");
        
        row1.add(createStyledLabel("ID:")); row1.add(txtID);
        row1.add(createStyledLabel("Name:")); row1.add(txtName);
        row1.add(btnAdd); row1.add(btnDelete);

        // Row 2
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row2.setBackground(COLOR_PANEL);
        txtLab1 = createStyledField(3);
        txtLab2 = createStyledField(3);
        txtLab3 = createStyledField(3);
        txtPrelim = createStyledField(3);
        txtAttendance = createStyledField(3);

        row2.add(createStyledLabel("Lab 1:")); row2.add(txtLab1);
        row2.add(createStyledLabel("Lab 2:")); row2.add(txtLab2);
        row2.add(createStyledLabel("Lab 3:")); row2.add(txtLab3);
        row2.add(createStyledLabel("Prelim:")); row2.add(txtPrelim);
        row2.add(createStyledLabel("Attend:")); row2.add(txtAttendance);

        inputPanel.add(row1);
        inputPanel.add(row2);
        this.add(inputPanel, BorderLayout.SOUTH);

        // --- 5. Load Data (FIXED) ---
        loadCSV("MOCK_DATA.csv");

        // --- 6. Button Logic ---
        btnAdd.addActionListener(e -> {
            String id = txtID.getText().trim();
            String nameFull = txtName.getText().trim();
            
            if(id.isEmpty() || nameFull.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Missing ID or Name."); return;
            }
            if (!id.matches("\\d{9}")) {
                JOptionPane.showMessageDialog(this, "ID must be exactly 9 digits."); return;
            }
            if (!nameFull.matches("^[a-zA-Z\\s']+$")) {
                JOptionPane.showMessageDialog(this, "Name invalid."); return;
            }

            try {
                int l1 = parseGrade(txtLab1.getText());
                int l2 = parseGrade(txtLab2.getText());
                int l3 = parseGrade(txtLab3.getText());
                int pre = parseGrade(txtPrelim.getText());
                int att = parseGrade(txtAttendance.getText());

                String[] nameParts = nameFull.split(" ", 2);
                String first = nameParts[0];
                String last = (nameParts.length > 1) ? nameParts[1] : "-";

                model.addRow(new Object[]{id, first, last, l1, l2, l3, pre, att});
                clearFields();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "All grades must be numbers between 0-100!");
            }
        });

        btnDelete.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                model.removeRow(table.convertRowIndexToModel(selectedRow));
            } else {
                JOptionPane.showMessageDialog(this, "Select a row.");
            }
        });

        this.setVisible(true);
    }

    private int parseGrade(String text) throws NumberFormatException {
        if(text.isEmpty()) return 0;
        int g = Integer.parseInt(text.trim());
        if (g < 0 || g > 100) throw new NumberFormatException("Range Error");
        return g;
    }

    private void clearFields() {
        txtID.setText(""); txtName.setText(""); 
        txtLab1.setText(""); txtLab2.setText(""); txtLab3.setText(""); 
        txtPrelim.setText(""); txtAttendance.setText("");
    }

    // --- FIX: CSV LOADER WITH AUTO-CREATE ---
    private void loadCSV(String filename) {
        File file = new File(filename);

        // 1. If file doesn't exist, create it with sample data
        if(!file.exists()) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("id,first_name,last_name,lab1,lab2,lab3,prelim,attendance\n");
                fw.write("231270536,Jared,Laordin,0,0,0,0,0\n");
                System.out.println("Created new MOCK_DATA.csv");
            } catch (IOException e) {
                System.err.println("Could not create dummy file: " + e.getMessage());
            }
        }

        // 2. Now read the file
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine(); // Skip Header
            
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if(data.length >= 8) {
                    try {
                        model.addRow(new Object[]{
                            data[0], data[1], data[2], 
                            Integer.parseInt(data[3]), Integer.parseInt(data[4]), 
                            Integer.parseInt(data[5]), Integer.parseInt(data[6]), 
                            Integer.parseInt(data[7])
                        });
                    } catch (NumberFormatException e) { /* Ignore bad rows */ }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading CSV: " + e.getMessage());
        }
    }

    private void styleTable(JTable table) {
        table.setBackground(COLOR_BG);
        table.setForeground(COLOR_TEXT);
        table.setGridColor(new Color(60, 60, 60));
        table.setSelectionBackground(COLOR_ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setRowHeight(25);
        table.setFont(FONT_MAIN);
        JTableHeader header = table.getTableHeader();
        header.setBackground(COLOR_ACCENT);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Consolas", Font.BOLD, 14));
        header.setBorder(new LineBorder(Color.BLACK));
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);
        table.setDefaultRenderer(Integer.class, centerRenderer);
    }

    private JTextField createStyledField(int cols) {
        JTextField field = new JTextField(cols);
        if (cols == 9) {
            field.setDocument(new PlainDocument() {
                @Override
                public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                    if (str == null) return;
                    if ((getLength() + str.length()) <= 9) super.insertString(offs, str, a);
                }
            });
        }
        field.setBackground(new Color(50, 50, 50));
        field.setForeground(Color.WHITE);
        field.setCaretColor(COLOR_ACCENT);
        field.setBorder(new LineBorder(COLOR_ACCENT, 1));
        field.setFont(FONT_MAIN);
        return field;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(COLOR_ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Consolas", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createStyledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(COLOR_TEXT);
        lbl.setFont(FONT_MAIN);
        return lbl;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LaordinStudentRecord());
    }
}