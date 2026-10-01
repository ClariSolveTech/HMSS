/**
 * Domain types for the AI-powered healthcare OS.
 * These mirror the shape of the future backend/ML API contracts so that
 * swapping mock data for live endpoints requires no UI changes.
 */

export type Priority = "routine" | "urgent" | "emergency";
export type VisitType = "New" | "Follow-up" | "Emergency" | "Teleconsult" | "Review";
export type QueueState = "waiting" | "in-consultation" | "completed" | "skipped";
export type DoctorStatus = "available" | "consulting" | "break" | "lunch" | "emergency" | "offline";

export interface Vitals {
  bloodPressure: string;
  pulse: number;
  temperature: number;
  weight: number;
  height: number;
  bmi: number;
  bloodSugar: number;
  spo2: number;
  respiration: number;
  recordedBy?: string;
  recordedAt?: string;
}

export interface Medication {
  id: string;
  name: string;
  dosage: string;
  morning: boolean;
  afternoon: boolean;
  night: boolean;
  food: "before" | "after";
  duration: string;
  instructions?: string;
}

export interface VisitRecord {
  id: string;
  date: string;
  doctor: string;
  department: string;
  diagnosis: string;
  prescription: string[];
  labReports: string[];
  scans: string[];
  vitals: Partial<Vitals>;
  bill: number;
  aiSummary: string;
  notes: string;
}

export interface Patient {
  id: string;
  upid: string;
  name: string;
  age: number;
  gender: "Male" | "Female" | "Other";
  photo: string;
  mobile: string;
  bloodGroup: string;
  height: number;
  weight: number;
  bmi: number;
  allergies: string[];
  emergencyContact: { name: string; relation: string; phone: string };
  insurance: { provider: string; policy: string; validTill: string };
  alerts: string[];
  chronic: string[];
  currentMedications: string[];
  vitals: Vitals;
  timeline: VisitRecord[];
  riskScore: number;
}

export interface QueueEntry {
  token: string;
  patientId: string;
  visitType: VisitType;
  waitMinutes: number;
  priority: Priority;
  state: QueueState;
  appointmentNo: string;
  slot: string;
}

export interface Notification {
  id: string;
  kind: "appointment" | "lab" | "emergency" | "waiting" | "reminder-failed";
  title: string;
  body: string;
  time: string;
  unread: boolean;
}

const face = (seed: string) =>
  `https://api.dicebear.com/9.x/notionists/svg?seed=${seed}&backgroundColor=dbeafe,e0f2fe,ede9fe`;

export const doctor = {
  name: "Dr. Ananya Rao",
  specialty: "Internal Medicine · Endocrinology",
  regNo: "MCI-88213",
  photo: face("Ananya"),
  hospital: "Meridian Health Institute",
};

export const patients: Patient[] = [
  {
    id: "p1",
    upid: "UPID-4471-8820",
    name: "Rahul Verma",
    age: 54,
    gender: "Male",
    photo: face("Rahul"),
    mobile: "+91 98450 22187",
    bloodGroup: "B+",
    height: 174,
    weight: 84,
    bmi: 27.7,
    allergies: ["Penicillin", "Sulfa drugs"],
    emergencyContact: { name: "Meera Verma", relation: "Spouse", phone: "+91 98450 22190" },
    insurance: { provider: "Star Health", policy: "SH-9920-4471", validTill: "12 Mar 2027" },
    alerts: ["Uncontrolled Type 2 Diabetes", "Stage 1 Hypertension"],
    chronic: ["Type 2 Diabetes Mellitus", "Hypertension", "Dyslipidemia"],
    currentMedications: ["Metformin 500mg", "Telmisartan 40mg", "Atorvastatin 10mg"],
    vitals: {
      bloodPressure: "148/94",
      pulse: 88,
      temperature: 98.4,
      weight: 84,
      height: 174,
      bmi: 27.7,
      bloodSugar: 186,
      spo2: 96,
      respiration: 18,
      recordedBy: "Nurse Kavya",
      recordedAt: "09:42 AM",
    },
    riskScore: 72,
    timeline: [
      {
        id: "v1",
        date: "18 Apr 2026",
        doctor: "Dr. Ananya Rao",
        department: "Internal Medicine",
        diagnosis: "Type 2 Diabetes Mellitus — poor glycemic control",
        prescription: ["Metformin 500mg — 1-0-1", "Telmisartan 40mg — 1-0-0"],
        labReports: ["HbA1c: 8.4%", "Fasting Glucose: 168 mg/dL"],
        scans: [],
        vitals: { bloodPressure: "146/92", pulse: 84, bloodSugar: 168 },
        bill: 1450,
        aiSummary:
          "Glycemic control worsened vs. Jan visit (HbA1c 7.6 → 8.4). BP trending up. Adherence gaps reported on evening dose.",
        notes: "Counselled on diet. Advised 30 min brisk walk daily.",
      },
      {
        id: "v2",
        date: "07 Jan 2026",
        doctor: "Dr. Ananya Rao",
        department: "Internal Medicine",
        diagnosis: "Type 2 Diabetes Mellitus — stable",
        prescription: ["Metformin 500mg — 1-0-0"],
        labReports: ["HbA1c: 7.6%", "Lipid Profile: LDL 138 mg/dL"],
        scans: ["ECG — normal sinus rhythm"],
        vitals: { bloodPressure: "138/86", pulse: 78, bloodSugar: 142 },
        bill: 1200,
        aiSummary: "Stable quarter. LDL borderline high — statin initiated.",
        notes: "Continue current regimen. Review in 3 months.",
      },
      {
        id: "v3",
        date: "22 Sep 2025",
        doctor: "Dr. Imran Sheikh",
        department: "Cardiology",
        diagnosis: "Essential hypertension — newly diagnosed",
        prescription: ["Telmisartan 40mg — 1-0-0"],
        labReports: ["Renal Function: normal"],
        scans: ["2D Echo — EF 58%"],
        vitals: { bloodPressure: "152/96", pulse: 82 },
        bill: 2600,
        aiSummary: "Cardiac structure normal. Antihypertensive started, salt restriction advised.",
        notes: "Referred back to Internal Medicine for combined management.",
      },
    ],
  },
  {
    id: "p2",
    upid: "UPID-2210-7734",
    name: "Sana Iqbal",
    age: 29,
    gender: "Female",
    photo: face("Sana"),
    mobile: "+91 90087 41220",
    bloodGroup: "O-",
    height: 162,
    weight: 58,
    bmi: 22.1,
    allergies: ["Latex"],
    emergencyContact: { name: "Adil Iqbal", relation: "Brother", phone: "+91 90087 41225" },
    insurance: { provider: "HDFC Ergo", policy: "HE-7734-1122", validTill: "30 Jun 2026" },
    alerts: ["Pregnancy — 22 weeks", "Iron deficiency anemia"],
    chronic: ["Hypothyroidism"],
    currentMedications: ["Levothyroxine 50mcg", "Ferrous ascorbate"],
    vitals: {
      bloodPressure: "112/74",
      pulse: 92,
      temperature: 98.1,
      weight: 58,
      height: 162,
      bmi: 22.1,
      bloodSugar: 94,
      spo2: 99,
      respiration: 16,
      recordedBy: "Nurse Kavya",
      recordedAt: "09:58 AM",
    },
    riskScore: 41,
    timeline: [
      {
        id: "v1",
        date: "02 Jun 2026",
        doctor: "Dr. Ananya Rao",
        department: "Internal Medicine",
        diagnosis: "Hypothyroidism in pregnancy",
        prescription: ["Levothyroxine 50mcg — 1-0-0 (empty stomach)"],
        labReports: ["TSH: 4.8 mIU/L", "Hb: 10.2 g/dL"],
        scans: ["USG — single live intrauterine gestation"],
        vitals: { bloodPressure: "110/72", pulse: 88 },
        bill: 900,
        aiSummary: "TSH above trimester target. Dose escalation considered. Mild anemia.",
        notes: "Coordinate with OB-GYN. Repeat TSH in 4 weeks.",
      },
    ],
  },
  {
    id: "p3",
    upid: "UPID-9903-1187",
    name: "Joseph Mathew",
    age: 67,
    gender: "Male",
    photo: face("Joseph"),
    mobile: "+91 99456 33019",
    bloodGroup: "A+",
    height: 168,
    weight: 71,
    bmi: 25.2,
    allergies: ["Aspirin"],
    emergencyContact: { name: "Anna Mathew", relation: "Daughter", phone: "+91 99456 33020" },
    insurance: { provider: "CGHS", policy: "CG-1187-0031", validTill: "01 Jan 2028" },
    alerts: ["Post-MI (2024)", "Low oxygen saturation 91%"],
    chronic: ["Coronary Artery Disease", "COPD"],
    currentMedications: ["Clopidogrel 75mg", "Rosuvastatin 20mg", "Tiotropium inhaler"],
    vitals: {
      bloodPressure: "134/82",
      pulse: 96,
      temperature: 99.2,
      weight: 71,
      height: 168,
      bmi: 25.2,
      bloodSugar: 118,
      spo2: 91,
      respiration: 22,
      recordedBy: "Nurse Devi",
      recordedAt: "10:05 AM",
    },
    riskScore: 88,
    timeline: [
      {
        id: "v1",
        date: "11 May 2026",
        doctor: "Dr. Imran Sheikh",
        department: "Cardiology",
        diagnosis: "Stable angina, COPD exacerbation risk",
        prescription: ["Clopidogrel 75mg — 0-1-0", "Tiotropium — 1 puff OD"],
        labReports: ["Troponin: negative", "CBC: WBC 9.1"],
        scans: ["Chest X-Ray — hyperinflated lungs"],
        vitals: { bloodPressure: "136/84", pulse: 92, spo2: 93 },
        bill: 3100,
        aiSummary: "No acute ischemia. Respiratory reserve reduced; pulmonary rehab suggested.",
        notes: "Advise flu vaccination. Watch for desaturation.",
      },
    ],
  },
  {
    id: "p4",
    upid: "UPID-5561-2043",
    name: "Priya Nair",
    age: 36,
    gender: "Female",
    photo: face("Priya"),
    mobile: "+91 88006 55412",
    bloodGroup: "AB+",
    height: 158,
    weight: 62,
    bmi: 24.8,
    allergies: [],
    emergencyContact: { name: "Vinod Nair", relation: "Spouse", phone: "+91 88006 55414" },
    insurance: { provider: "ICICI Lombard", policy: "IL-2043-8891", validTill: "18 Nov 2026" },
    alerts: ["Migraine with aura"],
    chronic: ["Migraine"],
    currentMedications: ["Propranolol 20mg"],
    vitals: {
      bloodPressure: "118/76",
      pulse: 74,
      temperature: 98.6,
      weight: 62,
      height: 158,
      bmi: 24.8,
      bloodSugar: 88,
      spo2: 98,
      respiration: 15,
      recordedBy: "Nurse Kavya",
      recordedAt: "10:14 AM",
    },
    riskScore: 24,
    timeline: [
      {
        id: "v1",
        date: "28 Feb 2026",
        doctor: "Dr. Ananya Rao",
        department: "Internal Medicine",
        diagnosis: "Chronic migraine — prophylaxis",
        prescription: ["Propranolol 20mg — 1-0-1"],
        labReports: ["CBC: normal"],
        scans: ["MRI Brain — unremarkable"],
        vitals: { bloodPressure: "116/74", pulse: 72 },
        bill: 4200,
        aiSummary: "Imaging clear. Attack frequency 6/month, prophylaxis indicated.",
        notes: "Maintain headache diary.",
      },
    ],
  },
  {
    id: "p5",
    upid: "UPID-7788-6610",
    name: "Aarav Sharma",
    age: 8,
    gender: "Male",
    photo: face("Aarav"),
    mobile: "+91 97411 20087",
    bloodGroup: "O+",
    height: 128,
    weight: 26,
    bmi: 15.9,
    allergies: ["Peanuts"],
    emergencyContact: { name: "Neha Sharma", relation: "Mother", phone: "+91 97411 20087" },
    insurance: { provider: "Niva Bupa", policy: "NB-6610-2277", validTill: "09 Aug 2026" },
    alerts: ["Severe peanut allergy — anaphylaxis history"],
    chronic: ["Asthma"],
    currentMedications: ["Salbutamol inhaler PRN"],
    vitals: {
      bloodPressure: "100/64",
      pulse: 102,
      temperature: 100.8,
      weight: 26,
      height: 128,
      bmi: 15.9,
      bloodSugar: 96,
      spo2: 97,
      respiration: 24,
      recordedBy: "Nurse Devi",
      recordedAt: "10:22 AM",
    },
    riskScore: 35,
    timeline: [
      {
        id: "v1",
        date: "14 Mar 2026",
        doctor: "Dr. Leela Menon",
        department: "Pediatrics",
        diagnosis: "Mild persistent asthma",
        prescription: ["Salbutamol inhaler — PRN"],
        labReports: ["Absolute eosinophil count: 480"],
        scans: [],
        vitals: { pulse: 98, spo2: 98 },
        bill: 700,
        aiSummary: "Trigger-linked wheeze. Spacer technique reviewed.",
        notes: "Avoid dust exposure; school inhaler plan shared.",
      },
    ],
  },
  {
    id: "p6",
    upid: "UPID-3312-9088",
    name: "Fatima Khan",
    age: 45,
    gender: "Female",
    photo: face("Fatima"),
    mobile: "+91 93800 11276",
    bloodGroup: "B-",
    height: 160,
    weight: 77,
    bmi: 30.1,
    allergies: ["Iodine contrast"],
    emergencyContact: { name: "Zoya Khan", relation: "Sister", phone: "+91 93800 11280" },
    insurance: { provider: "Star Health", policy: "SH-9088-3312", validTill: "22 Dec 2026" },
    alerts: ["Obesity class I", "Pre-diabetes"],
    chronic: ["PCOS", "Fatty liver"],
    currentMedications: ["Metformin 500mg"],
    vitals: {
      bloodPressure: "128/84",
      pulse: 80,
      temperature: 98.3,
      weight: 77,
      height: 160,
      bmi: 30.1,
      bloodSugar: 124,
      spo2: 98,
      respiration: 17,
      recordedBy: "Nurse Kavya",
      recordedAt: "10:31 AM",
    },
    riskScore: 58,
    timeline: [
      {
        id: "v1",
        date: "05 Jan 2026",
        doctor: "Dr. Ananya Rao",
        department: "Internal Medicine",
        diagnosis: "PCOS with insulin resistance",
        prescription: ["Metformin 500mg — 1-0-0"],
        labReports: ["HbA1c: 6.1%", "LFT: mild elevation"],
        scans: ["USG Abdomen — grade 2 fatty liver"],
        vitals: { bloodPressure: "126/82", pulse: 78 },
        bill: 1800,
        aiSummary: "Metabolic syndrome features. Weight reduction of 7% targeted.",
        notes: "Dietitian referral placed.",
      },
    ],
  },
];

export const queue: QueueEntry[] = [
  {
    token: "A-104",
    patientId: "p1",
    visitType: "Follow-up",
    waitMinutes: 0,
    priority: "urgent",
    state: "in-consultation",
    appointmentNo: "APT-20260731-104",
    slot: "10:30 AM",
  },
  {
    token: "A-105",
    patientId: "p3",
    visitType: "Emergency",
    waitMinutes: 4,
    priority: "emergency",
    state: "waiting",
    appointmentNo: "APT-20260731-105",
    slot: "10:45 AM",
  },
  {
    token: "A-106",
    patientId: "p2",
    visitType: "Review",
    waitMinutes: 12,
    priority: "routine",
    state: "waiting",
    appointmentNo: "APT-20260731-106",
    slot: "11:00 AM",
  },
  {
    token: "A-107",
    patientId: "p5",
    visitType: "New",
    waitMinutes: 21,
    priority: "urgent",
    state: "waiting",
    appointmentNo: "APT-20260731-107",
    slot: "11:15 AM",
  },
  {
    token: "A-108",
    patientId: "p6",
    visitType: "Follow-up",
    waitMinutes: 33,
    priority: "routine",
    state: "waiting",
    appointmentNo: "APT-20260731-108",
    slot: "11:30 AM",
  },
  {
    token: "A-103",
    patientId: "p4",
    visitType: "Teleconsult",
    waitMinutes: 0,
    priority: "routine",
    state: "completed",
    appointmentNo: "APT-20260731-103",
    slot: "10:15 AM",
  },
];

export const notifications: Notification[] = [
  {
    id: "n1",
    kind: "emergency",
    title: "Emergency patient assigned",
    body: "Joseph Mathew (A-105) — SpO₂ 91%, triaged to your queue.",
    time: "2 min ago",
    unread: true,
  },
  {
    id: "n2",
    kind: "lab",
    title: "Lab reports ready",
    body: "HbA1c & Lipid Profile for Rahul Verma are available.",
    time: "14 min ago",
    unread: true,
  },
  {
    id: "n3",
    kind: "waiting",
    title: "Patient waiting over 30 min",
    body: "Fatima Khan (A-108) has been waiting 33 minutes.",
    time: "21 min ago",
    unread: true,
  },
  {
    id: "n4",
    kind: "appointment",
    title: "New appointment booked",
    body: "Teleconsult with Priya Nair scheduled for 04:30 PM.",
    time: "1 hr ago",
    unread: false,
  },
  {
    id: "n5",
    kind: "reminder-failed",
    title: "Voice reminder failed",
    body: "Refill call to Sana Iqbal could not connect. Retry queued.",
    time: "3 hr ago",
    unread: false,
  },
];

export const todayStats = {
  appointments: 24,
  waiting: 4,
  completed: 11,
  emergency: 2,
  pendingLabs: 6,
  followUps: 9,
};

export const consultationTrend = [
  { day: "Mon", patients: 22, avgMinutes: 13 },
  { day: "Tue", patients: 26, avgMinutes: 12 },
  { day: "Wed", patients: 19, avgMinutes: 15 },
  { day: "Thu", patients: 28, avgMinutes: 11 },
  { day: "Fri", patients: 24, avgMinutes: 12 },
  { day: "Sat", patients: 31, avgMinutes: 10 },
];

export const monthlyConsultations = [
  { month: "Feb", value: 402 },
  { month: "Mar", value: 458 },
  { month: "Apr", value: 431 },
  { month: "May", value: 489 },
  { month: "Jun", value: 512 },
  { month: "Jul", value: 546 },
];

export const outcomeSplit = [
  { name: "Follow-up advised", value: 42 },
  { name: "Resolved", value: 33 },
  { name: "Referred", value: 15 },
  { name: "Admitted", value: 10 },
];

export const labCatalog = [
  "CBC",
  "Blood Sugar (F/PP)",
  "HbA1c",
  "ECG",
  "Lipid Profile",
  "Liver Function Test",
  "Kidney Function Test",
  "Urine Routine",
  "X-Ray Chest",
  "MRI Brain",
  "CT Scan Abdomen",
  "Thyroid Profile",
];

export const medicineCatalog = [
  { name: "Metformin", strengths: ["500mg", "850mg", "1000mg"], class: "Biguanide" },
  { name: "Telmisartan", strengths: ["20mg", "40mg", "80mg"], class: "ARB" },
  { name: "Atorvastatin", strengths: ["10mg", "20mg", "40mg"], class: "Statin" },
  { name: "Amoxicillin", strengths: ["250mg", "500mg"], class: "Penicillin antibiotic" },
  { name: "Pantoprazole", strengths: ["20mg", "40mg"], class: "PPI" },
  { name: "Levothyroxine", strengths: ["25mcg", "50mcg", "100mcg"], class: "Thyroid hormone" },
  { name: "Clopidogrel", strengths: ["75mg"], class: "Antiplatelet" },
  { name: "Propranolol", strengths: ["10mg", "20mg", "40mg"], class: "Beta blocker" },
  { name: "Salbutamol", strengths: ["100mcg inhaler"], class: "SABA" },
  { name: "Insulin Glargine", strengths: ["100 IU/mL"], class: "Basal insulin" },
];

export const findPatient = (id: string): Patient => {
  const found = patients.find((p) => p.id === id || p.id === `p${id}`);
  if (found) return found;

  const numericId = parseInt(id, 10);
  const name =
    id === "15778" || numericId === 15778
      ? "Ramesh Kumar"
      : id === "15779" || numericId === 15779
      ? "Priya Sharma"
      : id === "15780" || numericId === 15780
      ? "Amit Patel"
      : `Patient #${id}`;

  const gender = id === "15779" || numericId === 15779 ? "Female" : "Male";
  const bloodGroup = id === "15780" || numericId === 15780 ? "A+" : "O+";
  const upid = id === "15778" ? "PAT-101" : id === "15779" ? "PAT-102" : id === "15780" ? "PAT-103" : `PAT-${id}`;

  return {
    id: id,
    upid: upid,
    name: name,
    age: 38,
    gender: gender as "Male" | "Female",
    photo: face(name),
    mobile: "+91 98765 43210",
    bloodGroup: bloodGroup,
    height: 174,
    weight: 78,
    bmi: 25.8,
    allergies: ["Penicillin"],
    emergencyContact: { name: "Family", relation: "Spouse", phone: "+91 98765 43211" },
    insurance: { provider: "Star Health", policy: "POL-99283", validTill: "2027-12-31" },
    alerts: ["Routine Evaluation Needed"],
    chronic: ["Type 2 Diabetes Mellitus"],
    currentMedications: ["Metformin 500mg"],
    vitals: {
      bloodPressure: "128/82",
      pulse: 74,
      temperature: 98.4,
      weight: 78,
      height: 174,
      bmi: 25.8,
      bloodSugar: 124,
      spo2: 98,
      respiration: 16,
    },
    timeline: [],
    riskScore: 25,
  };
};

export const queueWithPatients = () =>
  queue
    .map((q) => ({ ...q, patient: findPatient(q.patientId)! }))
    .filter((q) => Boolean(q.patient));
