/**
 * Prelim Grade Calculator - Full Logic with Database & CSV Export
 */

const PRELIM_EXAM_WEIGHT = 0.30;
const CLASS_STANDING_WEIGHT = 0.70;
const ATTENDANCE_WEIGHT = 0.40;
const LAB_WORK_WEIGHT = 0.60;
const PASSING_GRADE = 75.0;
const EXCELLENT_GRADE = 100.0;
const TOTAL_MEETINGS = 4;

// DOM Elements
const form = document.getElementById('gradeForm');
const resultsSection = document.getElementById('results');
const clearBtn = document.getElementById('clearBtn');
const recordsBody = document.getElementById('recordsBody');

// Inputs
const nameInput = document.getElementById('studentName');
const attendanceInput = document.getElementById('attendance');
const lab1Input = document.getElementById('lab1');
const lab2Input = document.getElementById('lab2');
const lab3Input = document.getElementById('lab3');

// Results
const resAttendance = document.getElementById('resAttendance');
const resAttendancePercent = document.getElementById('resAttendancePercent');
const resLab1 = document.getElementById('resLab1');
const resLab2 = document.getElementById('resLab2');
const resLab3 = document.getElementById('resLab3');
const resLabAvg = document.getElementById('resLabAvg');
const resClassStanding = document.getElementById('resClassStanding');

const passingBox = document.getElementById('passingBox');
const passingContent = document.getElementById('passingContent');
const excellentBox = document.getElementById('excellentBox');
const excellentContent = document.getElementById('excellentContent');
const remarksText = document.getElementById('remarksText');

// State
let records = [];

// Initialize
function init() {
    const saved = localStorage.getItem('grade_records');
    if (saved) {
        records = JSON.parse(saved);
        renderTable();
    }
}

function formatNumber(num) {
    if (typeof num !== 'number') return num;
    return Math.round(num * 100) / 100; // 2 decimal places max
}

/**
 * Updates a requirement box with minimal styling
 */
function updateRequirementBox(box, contentDiv, score, label, isPassing) {
    // Reset classes
    box.className = 'req-box';

    let display = '';

    if (typeof score === 'string') {
        // Handle "Unique" status strings
        if (score === 'N/A' || score === 'FAIL') {
            box.classList.add('impossible');
            display = score;
        } else if (score === 'Secured' || score === 'Passed') {
            box.classList.add(isPassing ? 'passing' : 'excellent');
            display = score;
        } else {
            box.classList.add(isPassing ? 'passing' : 'excellent');
            display = score;
        }
    } else {
        // Numeric handling
        if (score > 100) {
            box.classList.add('impossible');
            display = 'N/A';
        } else if (score <= 0) {
            box.classList.add(isPassing ? 'passing' : 'excellent');
            display = isPassing ? 'Passed' : 'Secured';
        } else {
            box.classList.add(isPassing ? 'passing' : 'excellent');
            display = Math.round(score);
        }
    }

    contentDiv.innerHTML = `<span class="req-score">${display}</span>`;
}

function calculateGrade(event) {
    event.preventDefault();

    const nameVal = nameInput.value.trim();
    const attendanceVal = attendanceInput.value.trim();
    const lab1Val = lab1Input.value.trim();
    const lab2Val = lab2Input.value.trim();
    const lab3Val = lab3Input.value.trim();

    if (nameVal === '' || attendanceVal === '' || lab1Val === '' || lab2Val === '' || lab3Val === '') {
        alert("Please fill all fields (including Name).");
        return;
    }

    const attendanceCount = parseInt(attendanceVal);
    const lab1 = parseFloat(lab1Val);
    const lab2 = parseFloat(lab2Val);
    const lab3 = parseFloat(lab3Val);

    // Validation
    if (isNaN(attendanceCount) || attendanceCount < 1 || attendanceCount > 4) {
        alert("Attendance must be 1-4."); return;
    }
    if (lab1 < 0 || lab1 > 100 || lab2 < 0 || lab2 > 100 || lab3 < 0 || lab3 > 100) {
        alert("Grades must be 0-100."); return;
    }

    // 1. Basic Stats
    resAttendance.textContent = `${attendanceCount} / 4`;
    const attendancePercent = (attendanceCount / TOTAL_MEETINGS) * 100;
    resAttendancePercent.textContent = `${attendancePercent}%`;

    resLab1.textContent = formatNumber(lab1);
    resLab2.textContent = formatNumber(lab2);
    resLab3.textContent = formatNumber(lab3);

    let finalCS = 0;
    let finalReqExam = 0;
    let finalStatus = "Ongoing";

    // 2. Auto-Fail Check
    if (attendanceCount < 2) {
        resLabAvg.textContent = "-";
        resClassStanding.textContent = "-";

        passingBox.className = 'req-box impossible';
        passingContent.innerHTML = '<span class="req-score">FAIL</span>';

        excellentBox.className = 'req-box impossible';
        excellentContent.innerHTML = '<span class="req-score">FAIL</span>';

        remarksText.innerHTML = "<span style='color: var(--error-text)'>Automatic Failure: Less than 50% attendance.</span>";

        finalCS = 0;
        finalStatus = "Failed (Attendance)";
        saveRecordToDB(nameVal, 0, "FAIL", "Failed");

        resultsSection.classList.remove('results-hidden');
        resultsSection.classList.add('active');
        return;
    }

    // 3. Calculation
    const labAvg = (lab1 + lab2 + lab3) / 3;
    const classStanding = (attendancePercent * ATTENDANCE_WEIGHT) + (labAvg * LAB_WORK_WEIGHT);

    // Use Math.ceil to ensure we meet the target (no decimals)
    const reqPass = Math.ceil((PASSING_GRADE - (classStanding * CLASS_STANDING_WEIGHT)) / PRELIM_EXAM_WEIGHT);
    const reqExc = Math.ceil((EXCELLENT_GRADE - (classStanding * CLASS_STANDING_WEIGHT)) / PRELIM_EXAM_WEIGHT);

    resLabAvg.textContent = formatNumber(labAvg);
    resClassStanding.textContent = formatNumber(classStanding);

    // 4. Update Boxes
    updateRequirementBox(passingBox, passingContent, reqPass, 'Pass', true);
    updateRequirementBox(excellentBox, excellentContent, reqExc, 'Excellent', false);

    // 5. Remarks
    if (reqPass <= 0) {
        remarksText.innerHTML = "<span style='color: var(--success-text)'>Great! You have already passed based on Class Standing.</span>";
        finalStatus = "Passed";
    } else if (reqPass > 100) {
        remarksText.innerHTML = "<span style='color: var(--error-text)'>Warning: It is mathematically impossible to reach 75.</span>";
        finalStatus = "Impossible";
    } else {
        remarksText.innerHTML = `To pass the subject with a grade of 75, you need to score at least <span>${Math.ceil(reqPass)}</span> on the Prelim Exam.`;
        finalStatus = "Ongoing";
    }

    resultsSection.classList.remove('results-hidden');
    resultsSection.classList.add('active');

    // 6. Save to Database (Local Storage)
    const reqExamDisplay = reqPass <= 0 ? "Passed" : (reqPass > 100 ? "Impossible" : Math.ceil(reqPass));
    saveRecordToDB(nameVal, formatNumber(classStanding), reqExamDisplay, finalStatus);
}

// --- Database Functions ---

function saveRecordToDB(name, cs, reqExam, status) {
    const record = {
        id: Date.now(),
        date: new Date().toLocaleString(),
        name: name,
        cs: cs,
        reqExam: reqExam,
        status: status
    };

    records.push(record);
    localStorage.setItem('grade_records', JSON.stringify(records));
    // renderTable is called inside exportCSV implicitly via the flow or we can call it here
    renderTable();
}

function renderTable() {
    if (records.length === 0) {
        recordsBody.innerHTML = '<tr><td colspan="4" style="padding: 20px; text-align: center; color: #999;">No records found.</td></tr>';
        return;
    }

    recordsBody.innerHTML = records.map(rec => `
        <tr style="border-bottom: 1px solid #eee;">
            <td style="padding: 10px;">${rec.name}</td>
            <td style="padding: 10px;">${rec.cs}</td>
            <td style="padding: 10px;">${rec.reqExam}</td>
            <td style="padding: 10px;">
                <span style="
                    padding: 4px 8px; 
                    border-radius: 12px; 
                    font-size: 11px; 
                    background: ${getStatusColor(rec.status)}; 
                    color: white;">
                    ${rec.status}
                </span>
            </td>
        </tr>
    `).join('');
}

function getStatusColor(status) {
    if (status.includes("Failed") || status === "Impossible") return '#ef4444'; // Red
    if (status === "Passed" || status === "Secured") return '#22c55e'; // Green
    return '#f59e0b'; // Amber/Orange
}

window.clearDatabase = function () {
    if (confirm('Are you sure you want to clear all records?')) {
        records = [];
        localStorage.removeItem('grade_records');
        renderTable();
    }
};

window.exportCSV = function () {
    if (records.length === 0) return;

    // CSV Header
    let csvContent = "data:text/csv;charset=utf-8,";
    csvContent += "Date,Name,Class Standing,Required Exam,Status\n";

    // CSV Rows
    records.forEach(rec => {
        const row = `${rec.date},"${rec.name}",${rec.cs},"${rec.reqExam}","${rec.status}"`;
        csvContent += row + "\n";
    });

    // Create Download Link
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    // Fixed name allows browser to auto-increment (e.g. student_records (1).csv) 
    // or overwrite depending on settings.
    link.setAttribute("download", "student_records.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
};

function clearForm() {
    nameInput.value = '';
    attendanceInput.value = '';
    lab1Input.value = '';
    lab2Input.value = '';
    lab3Input.value = '';

    resAttendance.textContent = '-';
    resAttendancePercent.textContent = '-';
    resLab1.textContent = '-';
    resLab2.textContent = '-';
    resLab3.textContent = '-';
    resLabAvg.textContent = '-';
    resClassStanding.textContent = '-';

    resultsSection.classList.remove('active');
    resultsSection.classList.add('results-hidden');
    nameInput.focus();
}

form.addEventListener('submit', calculateGrade);
clearBtn.addEventListener('click', clearForm);

// Modal Logic
const modal = document.getElementById("dbModal");
const btn = document.getElementById("viewDbBtn");
const span = document.getElementsByClassName("close-modal")[0];

btn.onclick = function () {
    modal.style.display = "block";
    renderTable(); // Ensure fresh data
}

span.onclick = function () {
    modal.style.display = "none";
}

window.onclick = function (event) {
    if (event.target == modal) {
        modal.style.display = "none";
    }
}

// Start
init();
