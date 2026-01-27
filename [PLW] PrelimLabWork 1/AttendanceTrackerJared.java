// Name: Jared Wackyn Laordin

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AttendanceTrackerJared {

    // === GUI Components ===
    // Main frame of the application
    private JFrame frame;

    // Form fields for user input
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JComboBox<String> courseBox;
    private JComboBox<String> yearBox;
    private JTextField timeInField;

    // Signature Components
    // Label to show current status (Signed/Not Signed)
    private JLabel sigStatusLabel;
    // Popup dialog for drawing signature
    private JDialog signatureDialog;
    // Custom panel for drawing logic
    private SignaturePanel signaturePanel;

    // Button to submit attendance
    private JButton submitButton;

    // === Constants ===
    // Color palette for consistent UI theming
    private final Color PRIMARY_TEXT = new Color(44, 62, 80);
    private final Color BG_COLOR = new Color(240, 244, 248);
    private final Color SUCCESS_COLOR = new Color(46, 204, 113); // Green

    /**
     * Constructor Initializes the application
     */
    public AttendanceTrackerJared() {
        initializeFrame();
        createComponents();
        frame.setVisible(true);
    }

    /**
     * Sets up the main window properties (Title, Size, Layout).
     */
    private void initializeFrame() {
        frame = new JFrame("Attendance Tracker");
        frame.setSize(400, 380); // Fixed size for compact layout
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false); // Disable resizing to maintain layout integrity

        // Set Look and Feel to match the operating system
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates and arranges all UI elements using a BoxLayout.
     */
    private void createComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 20, 15, 20)); // Padding for aesthetics
        mainPanel.setBackground(BG_COLOR);

        // 1. HEADER SECTION
        JLabel titleLabel = new JLabel("Attendance Form");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(PRIMARY_TEXT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10)); // Spacing

        // 2. NAME FIELDS SECTION
        JPanel nameRow = new JPanel(new GridLayout(1, 2, 10, 0));
        nameRow.setOpaque(false); // Transparent background
        nameRow.setMaximumSize(new Dimension(400, 50));

        firstNameField = createStyledTextField();
        lastNameField = createStyledTextField();

        // Add fields with labels
        nameRow.add(createLabeledPanel("First Name", firstNameField));
        nameRow.add(createLabeledPanel("Last Name", lastNameField));
        mainPanel.add(nameRow);
        mainPanel.add(Box.createVerticalStrut(8)); // Spacing between rows

        // 3. COURSE & YEAR SECTION
        JPanel courseRow = new JPanel(new BorderLayout(10, 0));
        courseRow.setOpaque(false);
        courseRow.setMaximumSize(new Dimension(400, 50));

        // Expanded Course List with Meanings
        String[] courses = {
                "Select Course",
                "BA Comm - Bachelor of Arts in Communication",
                "BSA - Bachelor of Science in Accountancy",
                "BSArch - Bachelor of Science in Architecture",
                "BSBA - Business Administration",
                "BSCE - Civil Engineering",
                "BSCpE - Computer Engineering",
                "BSCrim - Criminology",
                "BSCS - Computer Science",
                "BSECE - Electronics Engineering",
                "BSIT - Information Technology",
                "BSME - Mechanical Engineering",
                "BSN - Nursing",
                "BSPsych - Psychology",
                "BSTM - Tourism Management"
        };
        courseBox = new JComboBox<>(courses);
        courseBox.setBackground(Color.WHITE);
        courseBox.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Ensure font fits expanded text

        String[] years = { "1st Year", "2nd Year", "3rd Year", "4th Year" };
        yearBox = new JComboBox<>(years);
        yearBox.setBackground(Color.WHITE);
        yearBox.setPreferredSize(new Dimension(90, 0)); // Fixed width for year dropdown

        courseRow.add(createLabeledPanel("Course", courseBox), BorderLayout.CENTER);
        courseRow.add(createLabeledPanel("Year", yearBox), BorderLayout.EAST);
        mainPanel.add(courseRow);
        mainPanel.add(Box.createVerticalStrut(8));

        // 4. TIME IN SECTION (System Time)
        timeInField = new JTextField();
        timeInField.setEditable(false); // Read-only
        timeInField.setBackground(Color.WHITE);
        timeInField.setHorizontalAlignment(JTextField.CENTER);
        timeInField.setFont(new Font("Consolas", Font.BOLD, 12));
        // Timestamp is initially empty until signed

        mainPanel.add(createLabeledPanel("Time In (System Time)", timeInField));
        mainPanel.add(Box.createVerticalStrut(8));

        // 5. SIGNATURE SECTION (Popup Trigger)
        JPanel sigPanel = new JPanel(new BorderLayout(10, 0));
        sigPanel.setOpaque(false);
        sigPanel.setMaximumSize(new Dimension(400, 50));

        JButton openSigBtn = new JButton("Open Signature Pad");
        openSigBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        openSigBtn.setFocusPainted(false);
        openSigBtn.addActionListener(e -> openSignatureDialog());

        sigStatusLabel = new JLabel("Not Signed", SwingConstants.CENTER);
        sigStatusLabel.setForeground(Color.RED);
        sigStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sigStatusLabel.setPreferredSize(new Dimension(90, 0));

        sigPanel.add(openSigBtn, BorderLayout.CENTER);
        sigPanel.add(sigStatusLabel, BorderLayout.EAST);

        mainPanel.add(createLabeledPanel("E-Signature", sigPanel));

        // Spacing before submit button
        mainPanel.add(Box.createVerticalStrut(15));

        // 6. SUBMIT BUTTON
        submitButton = new JButton("CONFIRM ATTENDANCE");
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(400, 40));
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Style for high visibility
        submitButton.setBackground(SUCCESS_COLOR);
        submitButton.setForeground(Color.BLACK);
        submitButton.setFocusPainted(false);

        submitButton.addActionListener(e -> handleSubmit());
        mainPanel.add(submitButton);

        frame.add(mainPanel);
    }

    /**
     * Updates the time field with the current system time formatted as string.
     */
    private void updateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        timeInField.setText(now.format(fmt));
    }

    /**
     * Handles the submit button click.
     * Validates inputs and displays a summary dialog.
     */
    private void handleSubmit() {
        // Validation: Check if name fields are empty
        if (firstNameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter your full name.", "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validation: Check if course is selected
        if (courseBox.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(frame, "Please select your course.", "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validation: Check if signature is signed
        if (!"Signed".equals(sigStatusLabel.getText())) {
            JOptionPane.showMessageDialog(frame, "Please provide your signature.", "Signature Missing",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Prepare Summary String
        String summary = String.format(
                "Attendance Recorded!\n\nName: %s %s\nCourse: %s (%s)\nTime In: %s\nSignature: Captured",
                firstNameField.getText(), lastNameField.getText(),
                courseBox.getSelectedItem(), yearBox.getSelectedItem(),
                timeInField.getText());

        // Show Success Message
        JOptionPane.showMessageDialog(frame, summary, "Success", JOptionPane.INFORMATION_MESSAGE);

        // Reset Form for next user
        firstNameField.setText("");
        lastNameField.setText("");
        sigStatusLabel.setText("Not Signed");
        sigStatusLabel.setForeground(Color.RED);
        timeInField.setText(""); // Clear time for next entry
        courseBox.setSelectedIndex(0); // Reset course selection
    }

    /**
     * Opens a modal dialog with a drawing canvas for the signature.
     */
    private void openSignatureDialog() {
        if (signatureDialog == null) {
            signatureDialog = new JDialog(frame, "E-Signature Pad", true); // Modal dialog
            signatureDialog.setSize(360, 280);
            signatureDialog.setLayout(new BorderLayout());
            signatureDialog.setLocationRelativeTo(frame);

            signaturePanel = new SignaturePanel();
            signatureDialog.add(signaturePanel, BorderLayout.CENTER);

            // Controls for the signature dialog
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton clearBtn = new JButton("Clear");
            clearBtn.addActionListener(e -> signaturePanel.clear());

            JButton confirmBtn = new JButton("Save & Close");
            confirmBtn.addActionListener(e -> {
                if (signaturePanel.hasSignature()) {
                    sigStatusLabel.setText("Signed");
                    sigStatusLabel.setForeground(new Color(46, 204, 113)); // Update label to Green
                    updateTime(); // Capture timestamp upon signing
                    signatureDialog.setVisible(false);
                } else {
                    JOptionPane.showMessageDialog(signatureDialog, "Canvas is empty. Please sign.");
                }
            });

            bottomPanel.add(clearBtn);
            bottomPanel.add(confirmBtn);
            signatureDialog.add(bottomPanel, BorderLayout.SOUTH);
        }
        signaturePanel.clear(); // Clear previous signature on open
        signatureDialog.setVisible(true);
    }

    // === Helper Methods ===

    /**
     * Creates a vertical panel containing a label and a component.
     * Useful for tidy form layout.
     */
    private JPanel createLabeledPanel(String text, JComponent component) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(Color.GRAY);
        p.add(l, BorderLayout.NORTH);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    /**
     * Generates a styled text field with padding and borders.
     */
    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return tf;
    }

    // === Signature Panel Logic ===
    /**
     * Custom JPanel that allows drawing via Mouse Listeners.
     */
    class SignaturePanel extends JPanel {
        private BufferedImage image;
        private Graphics2D g2d;
        private Point lastPoint;
        private boolean hasSig = false;

        public SignaturePanel() {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            // Mouse Adapter to handle drawing
            MouseAdapter ma = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    lastPoint = e.getPoint();
                }

                public void mouseDragged(MouseEvent e) {
                    if (lastPoint != null) {
                        draw(lastPoint, e.getPoint());
                        lastPoint = e.getPoint();
                        hasSig = true;
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        /**
         * Draws a line from the last point to the current point.
         */
        private void draw(Point p1, Point p2) {
            if (image == null)
                initImage();
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            repaint();
        }

        /**
         * Initializes the buffered image for drawing.
         */
        private void initImage() {
            image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            g2d = image.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2f));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null)
                initImage();
            g.drawImage(image, 0, 0, null);
        }

        /**
         * Clears the canvas.
         */
        public void clear() {
            if (g2d != null) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.BLACK);
                repaint();
                hasSig = false;
            }
        }

        public boolean hasSignature() {
            return hasSig;
        }
    }

    /**
     * Main method to launch the application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(AttendanceTrackerJared::new);
    }
}
