// ==========================================
// 1. CONFIGURATION & STATE
// ==========================================

// Predefined users for authentication (Mock Database)
const USERS = {
    "admin": "password123",
    "jared": "bossing",
    "laordin": "paldo",
    "student1": "learn123",
    "guest": "guestPass"
};

// Audio Effects
const correctSound = new Audio('correct.mp3');
const wrongSound = new Audio('wrong.mp3');

// Persistent Attendance State (Loads from LocalStorage if available)
let attendanceRecords = JSON.parse(localStorage.getItem('attendanceRecords')) || [];
let fileDownloadUrl = null;

// ==========================================
// 2. DOM ELEMENTS SELECTION
// ==========================================

const loginForm = document.getElementById('login-form');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const errorMsg = document.getElementById('error-msg');

const loginSection = document.getElementById('login-section');
const dashboardSection = document.getElementById('dashboard-section');

const displayUsername = document.getElementById('display-username');
const clockElement = document.getElementById('clock');
const dateElement = document.getElementById('date');
const loginTimeElement = document.getElementById('login-time');

const downloadBtn = document.getElementById('download-btn');
const logoutBtn = document.getElementById('logout-btn');

// ==========================================
// 3. EVENT LISTENERS
// ==========================================

// Login Form Handler
loginForm.addEventListener('submit', (e) => {
    e.preventDefault(); // Prevent page reload

    // Get inputs
    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    // Validate using the USERS dictionary
    if (USERS[username] && USERS[username] === password) {
        handleLoginSuccess(username);
    } else {
        handleLoginFailure();
    }
});

// Logout Button Handler
logoutBtn.addEventListener('click', () => {
    // Clear inputs and error states
    loginForm.reset();
    errorMsg.classList.remove('show');

    // Switch Views (Dashboard -> Login)
    dashboardSection.classList.remove('active');
    dashboardSection.classList.add('hidden');

    setTimeout(() => {
        loginSection.classList.remove('hidden');
        loginSection.classList.add('active');
    }, 300);
});

// ==========================================
// 4. CORE FUNCTIONS
// ==========================================

/**
 * Handles actions to perform when login fails.
 * Plays sound, flashes screen red, and shows error message.
 */
function handleLoginFailure() {
    // 1. Play Error Sound
    wrongSound.currentTime = 0;
    wrongSound.play().catch(e => console.log("Audio play failed:", e));

    // 2. Trigger Red Glow Animation (CSS Class)
    document.body.classList.add('login-error');
    setTimeout(() => {
        document.body.classList.remove('login-error');
    }, 800);

    // 3. Show Visual Error Message
    errorMsg.classList.add('show');
    passwordInput.value = ''; // Clear password field for retry
    passwordInput.focus();
}

/**
 * Handles actions to perform when login is successful.
 * Logs attendance, updates UI, and switches to dashboard.
 */
function handleLoginSuccess(username) {
    errorMsg.classList.remove('show');

    // 1. Play Success Sound
    correctSound.currentTime = 0;
    correctSound.play().catch(e => console.log("Audio play failed:", e));

    // 2. Trigger Green Glow Animation
    document.body.classList.add('login-success');
    setTimeout(() => {
        document.body.classList.remove('login-success');
    }, 800);

    // 1. Capture Current Timestamp
    const now = new Date();
    const timestamp = now.toLocaleString('en-US', { hour12: true });

    // 2. Save to Attendance Log (LocalStorage)
    attendanceRecords.push({ username, timestamp });
    localStorage.setItem('attendanceRecords', JSON.stringify(attendanceRecords));

    // 3. Switch Views (Login -> Dashboard)
    loginSection.classList.remove('active');
    loginSection.classList.add('hidden');

    setTimeout(() => {
        dashboardSection.classList.remove('hidden');
        dashboardSection.classList.add('active');
    }, 300);

    // 4. Update Dashboard Content
    displayUsername.textContent = username;
    loginTimeElement.textContent = timestamp; // Show static login time

    // 5. Prepare the CSV download link
    prepareDownload();
}

/**
 * Generates a real-time clock on the dashboard.
 * Updates every second.
 */
function updateClock() {
    const now = new Date();

    // 12-hour format time
    clockElement.textContent = now.toLocaleTimeString('en-US', { hour12: true });

    // Date format (MM/DD/YYYY)
    dateElement.textContent = now.toLocaleDateString('en-US', {
        month: '2-digit', day: '2-digit', year: 'numeric'
    });
}
// Start clock immediately and update every 1000ms
setInterval(updateClock, 1000);
updateClock();

/**
 * Prepares the CSV file for download.
 * Iterates through all localStorage records and creates a Blob.
 */
function prepareDownload() {
    // 1. Define CSV Header
    let csvContent = "Username,Timestamp\n";

    // 2. Append all records
    attendanceRecords.forEach(record => {
        csvContent += `${record.username},"${record.timestamp}"\n`;
    });

    // 3. Create Blob object
    const blob = new Blob([csvContent], { type: 'text/csv' });

    // 4. cleanup previous URL to free memory
    if (fileDownloadUrl) {
        window.URL.revokeObjectURL(fileDownloadUrl);
    }

    // 5. Create new URL
    fileDownloadUrl = window.URL.createObjectURL(blob);

    // 6. Bind to download button
    downloadBtn.onclick = () => {
        const link = document.createElement('a');
        link.href = fileDownloadUrl;
        link.download = 'attendance_summary.csv';
        link.click();
    };
}
