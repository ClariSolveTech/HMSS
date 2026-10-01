# Aura Health Suite

You are a Senior Product Designer, Healthcare Domain Expert, UI/UX Architect, and Full Stack Engineer.

Build a world-class Doctor Dashboard for an AI-Powered Healthcare Operating System.

This is NOT a traditional Hospital Management System.

The Doctor Dashboard should help doctors complete an entire patient consultation from a single workspace.

The UI should look like a premium SaaS product inspired by Stripe, Linear, Notion, Apple, Google, and Vercel.

The dashboard should be responsive for Desktop, Tablet, and Mobile.

Use React.js + TypeScript + Tailwind CSS + Framer Motion + Lucide Icons.

Design should be modern, clean, minimal, premium, and fast.

--------------------------------------------------

LAYOUT

--------------------------------------------------

Top Navigation Bar

• Hospital Logo

• Doctor Profile

• Notifications

• AI Assistant

• Settings

• Logout

Left Sidebar

Dashboard

Today's Patients

Appointments

Current Queue

Patient Search

Consultation History

Prescriptions

Laboratory

Reports

Analytics

Settings

--------------------------------------------------

DOCTOR HOME DASHBOARD

--------------------------------------------------

At the top display beautiful statistic cards.

Today's Appointments

Patients Waiting

Patients Completed

Emergency Cases

Pending Lab Reports

Follow-ups Scheduled

Below statistics show

Today's Appointment Timeline

Current Token

Next Patient

Average Consultation Time

Queue Status

--------------------------------------------------

LIVE QUEUE PANEL

--------------------------------------------------

Display live queue.

Current Token

Patient Name

Age

Gender

Visit Type

Estimated Waiting Time

Priority

Doctor can click

Start Consultation

Skip

Call Next

Emergency Override

--------------------------------------------------

PATIENT SEARCH

--------------------------------------------------

Allow searching by

Universal Patient ID

Mobile Number

Patient Name

QR Code

Appointment Number

Show instant search suggestions.

--------------------------------------------------

CONSULTATION SCREEN

--------------------------------------------------

When doctor opens a patient, the entire screen changes into a consultation workspace.

Left Side

Patient Profile

Patient Photo

Universal Patient ID

Age

Gender

Blood Group

Height

Weight

BMI

Allergies

Emergency Contact

Insurance

Medical Alerts

Chronic Diseases

Current Medications

Right Side

Consultation Form

--------------------------------------------------

PATIENT TIMELINE

--------------------------------------------------

Beautiful vertical timeline.

Every visit should display

Date

Doctor

Diagnosis

Prescription

Lab Reports

Scans

Vitals

Bills

AI Summary

Doctor Notes

Timeline should allow expanding previous visits.

--------------------------------------------------

VITALS

--------------------------------------------------

Display

Blood Pressure

Pulse Rate

Temperature

Weight

Height

BMI

Blood Sugar

Oxygen Saturation

Respiration Rate

Editable if nurse has not entered.

--------------------------------------------------

DIAGNOSIS

--------------------------------------------------

Doctor can add

Primary Diagnosis

Secondary Diagnosis

Symptoms

Clinical Findings

Observations

Diagnosis Codes

Notes

--------------------------------------------------

DIGITAL PRESCRIPTION

--------------------------------------------------

This is the most important module.

Doctor should search medicines quickly.

Medicine Name

Dosage

Frequency

Morning

Afternoon

Night

Before Food

After Food

Duration

Special Instructions

Medicine should be added as rows.

Example

Metformin

500mg

Morning

Night

After Food

90 Days

After saving prescription automatically generate

Medicine Reminder Schedule

Voice Call Schedule

Refill Reminder

--------------------------------------------------

LAB TESTS

--------------------------------------------------

Doctor can request tests.

CBC

Blood Sugar

HbA1c

ECG

Lipid Profile

Liver Function

Kidney Function

Urine Test

X-Ray

MRI

CT Scan

Custom Tests

--------------------------------------------------

FOLLOW-UP

--------------------------------------------------

Doctor selects

Next Visit Date

Reason

Priority

Follow-up Notes

When saved

Automatically schedule

Voice Call Reminder

SMS Reminder

Notification Reminder

Reminder

7 Days Before

3 Days Before

1 Day Before

--------------------------------------------------

VOICE DICTATION

--------------------------------------------------

Doctor should have microphone button.

Voice converted into notes.

Store notes inside EMR.

--------------------------------------------------

AI ASSISTANT PANEL

--------------------------------------------------

Collapsible right sidebar.

AI should provide

Patient Summary

Previous Diagnoses

Medicine History

Drug Allergy Alerts

Drug Interaction Alerts

Chronic Disease Summary

Health Trend

Suggested Follow-up

Risk Alerts

The AI should assist the doctor but never prescribe medicines automatically.

--------------------------------------------------

CONSULTATION ACTIONS

--------------------------------------------------

Buttons

Save Draft

Complete Consultation

Print Prescription

Send Prescription

Order Lab

Refer Specialist

Hospital Admission

Generate Medical Certificate

--------------------------------------------------

QUEUE MANAGEMENT

--------------------------------------------------

Doctor Status

Available

Consulting

Break

Lunch

Emergency

Offline

Reception dashboard should update automatically.

--------------------------------------------------

EMERGENCY ALERT

--------------------------------------------------

Display red banner if

Critical Patient

High BP

Low Oxygen

Heart Disease

Diabetes Risk

Pregnancy Risk

Allergy Warning

--------------------------------------------------

NOTIFICATIONS

--------------------------------------------------

New Appointment

Lab Reports Ready

Emergency Patient

Patient Waiting

Reminder Failed

--------------------------------------------------

DOCTOR ANALYTICS

--------------------------------------------------

Today's Patients

Average Consultation Time

Follow-up Rate

Patient Satisfaction

Monthly Consultations

--------------------------------------------------

UI DESIGN

--------------------------------------------------

Rounded cards

Glassmorphism

Soft shadows

Blue healthcare theme

Beautiful icons

Professional typography

Large spacing

Minimal interface

Premium animations

Skeleton loading

Smooth transitions

Hover effects

--------------------------------------------------

RESPONSIVE DESIGN

--------------------------------------------------

Desktop

Tablet

Mobile

Fully responsive.

--------------------------------------------------

FUTURE AI READY

--------------------------------------------------

Design APIs and architecture so future ML models can be integrated without changing UI.

Possible future AI features

Disease Prediction

Prescription Suggestions

Patient Risk Score

No-show Prediction

Lab Report Interpretation

Medicine Adherence Prediction

--------------------------------------------------

DELIVERABLE

--------------------------------------------------

Generate a complete Doctor Dashboard with all screens, reusable React components, responsive layouts, dummy healthcare data, modern UI, navigation, and production-ready frontend architecture.

The UI should feel like software used by Apollo Hospitals or Mayo Clinic but modernized with AI.

Do not generate a basic dashboard.

Generate an enterprise-grade product.

This project was built with [Lovable](https://lovable.dev).

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/8f40f017-97bf-4a5a-a0bb-685b1d71ea2b).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
