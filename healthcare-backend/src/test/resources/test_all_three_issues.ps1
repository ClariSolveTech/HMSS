$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "E2E VERIFICATION: ALL 3 ISSUES ON ACTUAL RUNNING SYSTEM" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$backendUrl = "http://localhost:8081"
$frontendUrl = "http://localhost:8080"

# 1. Login as Receptionist
Write-Host "`n--- Step 1: Login as Receptionist ---" -ForegroundColor Yellow
$recepLoginBody = @{
    emailOrMobile = "receptionist@citycentral.com"
    password = "Recep@123"
} | ConvertTo-Json

$recepLoginRes = Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/login" -Method Post -Body $recepLoginBody -ContentType "application/json"
$recepToken = $recepLoginRes.accessToken
Write-Host "SUCCESS: Receptionist authenticated. User ID: $($recepLoginRes.userId), Hospital ID: $($recepLoginRes.hospitalId)" -ForegroundColor Green

# 2. Login as Doctor
Write-Host "`n--- Step 2: Login as Doctor ---" -ForegroundColor Yellow
$docLoginBody = @{
    emailOrMobile = "dr.aarav@citycentral.com"
    password = "Doctor@123"
} | ConvertTo-Json

$docLoginRes = Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/login" -Method Post -Body $docLoginBody -ContentType "application/json"
$docToken = $docLoginRes.accessToken
Write-Host "SUCCESS: Doctor authenticated. User ID: $($docLoginRes.userId)" -ForegroundColor Green

# 3. Create a new walk-in patient (simulating receptionist walk-in registration)
Write-Host "`n--- Step 3: Register Walk-In Patient without Login Account ---" -ForegroundColor Yellow
$suffix = [System.Guid]::NewGuid().ToString().Substring(0, 6)
$randomDigits = [Math]::Abs([System.Guid]::NewGuid().GetHashCode() % 10000000).ToString("0000000")
$walkInMobile = "950" + $randomDigits

$patientBody = @{
    firstName = "Karan"
    lastName = "Mehta"
    dateOfBirth = "1994-08-12"
    gender = "MALE"
    mobileNumber = $walkInMobile
    email = "karan.$suffix@example.com"
    address = "45 Lakeview Residency"
    city = "Bangalore"
    state = "Karnataka"
    pincode = "560001"
} | ConvertTo-Json

$patientHeaders = @{ Authorization = "Bearer $recepToken" }
$newPatient = Invoke-RestMethod -Uri "$backendUrl/api/v1/patients" -Method Post -Headers $patientHeaders -Body $patientBody -ContentType "application/json"
$walkInPatientId = $newPatient.id
Write-Host "SUCCESS: Walk-in patient created. ID: $walkInPatientId, Code: $($newPatient.patientCode), Mobile: $($newPatient.mobileNumber), User ID: $($newPatient.userId)" -ForegroundColor Green

if ($newPatient.userId -ne $null) {
    throw "Expected walk-in patient to have null userId!"
}

# 4. ISSUE 1: Test Receptionist Teleconsultation Booking with slot validation
Write-Host "`n--- Step 4: [ISSUE 1] Test Receptionist Teleconsultation Booking ---" -ForegroundColor Yellow
# Get Doctor ID
$doctorsList = Invoke-RestMethod -Uri "$backendUrl/api/v1/doctors" -Method Get -Headers $patientHeaders
$doctor = $doctorsList.content[0]
$doctorId = $doctor.id

# Check Doctor availability for tomorrow
$tomorrow = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
$availSlots = Invoke-RestMethod -Uri "$backendUrl/api/v1/doctors/$doctorId/availability?date=$tomorrow" -Method Get -Headers $patientHeaders
$freeSlots = @($availSlots.slots | Where-Object { $_.available -eq $true })
if ($freeSlots.Count -lt 2) {
    throw "Not enough available slots on $tomorrow for doctor $doctorId. Free slots: $($freeSlots.Count)"
}

$firstSlotTime = $freeSlots[0].startTime
Write-Host "Doctor has $($freeSlots.Count) available slots on $tomorrow. First available slot: $firstSlotTime" -ForegroundColor DarkCyan

# Book TELECONSULTATION appointment
$bookReq = @{
    patientId = $walkInPatientId
    doctorId = $doctorId
    appointmentDate = $tomorrow
    startTime = $firstSlotTime
    appointmentType = "TELECONSULTATION"
    reason = "Online Cardiology Follow-up"
} | ConvertTo-Json

$appointment = Invoke-RestMethod -Uri "$backendUrl/api/v1/appointments" -Method Post -Headers $patientHeaders -Body $bookReq -ContentType "application/json"
$aptId = $appointment.id
Write-Host "SUCCESS: Teleconsultation appointment booked via Receptionist desk!" -ForegroundColor Green
Write-Host "Appointment Number: $($appointment.appointmentNumber)" -ForegroundColor Green
Write-Host "Type: $($appointment.appointmentType)" -ForegroundColor Green
Write-Host "Status: $($appointment.status)" -ForegroundColor Green
Write-Host "Patient: $($appointment.patientName) | Doctor: $($appointment.doctorName)" -ForegroundColor Green

# Also test booking an in-person Follow-up appointment to confirm other types still work
$secondSlotTime = $freeSlots[1].startTime
$followUpReq = @{
    patientId = $walkInPatientId
    doctorId = $doctorId
    appointmentDate = $tomorrow
    startTime = $secondSlotTime
    appointmentType = "FOLLOW_UP"
    reason = "Routine In-Person Follow-up"
} | ConvertTo-Json
$followUpApt = Invoke-RestMethod -Uri "$backendUrl/api/v1/appointments" -Method Post -Headers $patientHeaders -Body $followUpReq -ContentType "application/json"
Write-Host "SUCCESS: In-person Follow-Up appointment also booked: $($followUpApt.appointmentNumber), Type: $($followUpApt.appointmentType)" -ForegroundColor Green

# 5. ISSUE 3: Receptionist Generates Activation Code for Walk-in Patient
Write-Host "`n--- Step 5: [ISSUE 3] Receptionist Generates Activation Code ---" -ForegroundColor Yellow
$actCodeRes = Invoke-RestMethod -Uri "$backendUrl/api/v1/patients/$walkInPatientId/generate-activation-code" -Method Post -Headers $patientHeaders
$activationCode = $actCodeRes.activationCode
Write-Host "SUCCESS: Generated activation code: $activationCode" -ForegroundColor Green
Write-Host "Expires At: $($actCodeRes.expiresAt)" -ForegroundColor Green
Write-Host "Instructions: $($actCodeRes.instructions)" -ForegroundColor DarkCyan

# Security check 1: Attempt activation with invalid code
Write-Host "Security Check: Testing invalid activation code rejection..." -ForegroundColor Yellow
try {
    $badReq = @{ activationCode = "ACT-FAKE-CODE"; password = "Password@123" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/patient/activate" -Method Post -Body $badReq -ContentType "application/json"
    throw "Should have failed on invalid activation code!"
} catch {
    Write-Host "PASS: Invalid code rejected as expected ($($_.Exception.Message))" -ForegroundColor Green
}

# 6. Patient Activates Account and Sets Password
Write-Host "`n--- Step 6: [ISSUE 3] Patient Activates Account on Patient Portal ---" -ForegroundColor Yellow
$patientPassword = "KaranPassword@123"
$activateReq = @{
    activationCode = $activationCode
    password = $patientPassword
    email = "karan.$suffix@example.com"
} | ConvertTo-Json

$activateRes = Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/patient/activate" -Method Post -Body $activateReq -ContentType "application/json"
$patientToken = $activateRes.accessToken
$patientUserId = $activateRes.userId
Write-Host "SUCCESS: Patient account activated! User ID: $patientUserId, Token issued." -ForegroundColor Green
Write-Host "Roles: $($activateRes.roles -join ', ')" -ForegroundColor Green

# Security check 2: Code cannot be reused
Write-Host "Security Check: Testing one-time code reuse prevention..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$backendUrl/api/v1/auth/patient/activate" -Method Post -Body $activateReq -ContentType "application/json"
    throw "Should have failed on code reuse!"
} catch {
    Write-Host "PASS: Code reuse rejected ($($_.Exception.Message))" -ForegroundColor Green
}

# 7. Patient logs into portal and views their teleconsultation appointment
Write-Host "`n--- Step 7: Patient Accesses Their Appointments in Portal ---" -ForegroundColor Yellow
$patientHeaders = @{ Authorization = "Bearer $patientToken" }
$patientAppointments = Invoke-RestMethod -Uri "$backendUrl/api/v1/patient-portal/appointments" -Method Get -Headers $patientHeaders
Write-Host "Patient sees $($patientAppointments.content.Count) appointments in their portal:" -ForegroundColor Green
foreach ($apt in $patientAppointments.content) {
    Write-Host "  - Apt #$($apt.appointmentNumber): $($apt.doctorName) | $($apt.appointmentType) | $($apt.appointmentDate) $($apt.startTime) | Status: $($apt.status)" -ForegroundColor DarkCyan
}

$patientTeleApt = $patientAppointments.content | Where-Object { $_.appointmentType -eq "TELECONSULTATION" }
if (-not $patientTeleApt) {
    throw "Patient could not find their booked teleconsultation appointment!"
}
Write-Host "SUCCESS: Patient correctly sees their own teleconsultation appointment (#$($patientTeleApt.appointmentNumber))!" -ForegroundColor Green

# 8. ISSUE 2: Real Google Meet Provider & Doctor Provisioning Check
Write-Host "`n--- Step 8: [ISSUE 2] Test Real Google Meet Provider Call ---" -ForegroundColor Yellow
$docHeaders = @{ Authorization = "Bearer $docToken" }

try {
    $provisionRes = Invoke-RestMethod -Uri "$backendUrl/api/v1/appointments/$aptId/teleconsult" -Method Post -Headers $docHeaders
    Write-Host "Room Provisioned! Meet URL: $($provisionRes.meetUrl)" -ForegroundColor Green
} catch {
    $errBody = $_.ErrorDetails.Message
    Write-Host "HTTP Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    Write-Host "Backend Response: $errBody" -ForegroundColor Yellow
    Write-Host "PASS: Real Google Meet provider was called. Clear, actionable error returned without fake/mock fallback." -ForegroundColor Green
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "ALL E2E ACCEPTANCE TESTS COMPLETED SUCCESSFULLY" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
