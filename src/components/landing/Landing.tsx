import { useEffect, useRef, useState } from "react";
import { motion, useInView, useScroll, useTransform, AnimatePresence } from "framer-motion";
import {
  Activity, Stethoscope, Pill, PhoneCall, FileText, LineChart, Users, Bell,
  Calendar, FlaskConical, Receipt, ShieldCheck, Cloud, Building2, RefreshCw,
  Brain, Mic, ClipboardList, HeartPulse, Clock, ArrowRight, Play, Check,
  ChevronDown, Menu, X, Sparkles, Droplets, Utensils, TrendingUp, Hospital,
  UserRound, ClipboardCheck, Layers, Twitter, Linkedin, Github, Star,
} from "lucide-react";

/* ----------------------------- Primitives ----------------------------- */

function Section({ id, className = "", children }: { id?: string; className?: string; children: React.ReactNode }) {
  return (
    <section id={id} className={`relative mx-auto w-full max-w-7xl px-6 py-24 md:py-32 ${className}`}>
      {children}
    </section>
  );
}

function Eyebrow({ children }: { children: React.ReactNode }) {
  return (
    <div className="inline-flex items-center gap-2 rounded-full border border-border bg-white/70 px-3 py-1 text-xs font-medium text-primary shadow-soft backdrop-blur">
      <span className="h-1.5 w-1.5 rounded-full bg-secondary" /> {children}
    </div>
  );
}

function Reveal({ children, delay = 0 }: { children: React.ReactNode; delay?: number }) {
  const ref = useRef<HTMLDivElement>(null);
  const inView = useInView(ref, { once: true, margin: "-80px" });
  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 24 }}
      animate={inView ? { opacity: 1, y: 0 } : {}}
      transition={{ duration: 0.7, delay, ease: [0.22, 1, 0.36, 1] }}
    >
      {children}
    </motion.div>
  );
}

function Counter({ to, suffix = "" }: { to: number; suffix?: string }) {
  const ref = useRef<HTMLSpanElement>(null);
  const inView = useInView(ref, { once: true });
  const [n, setN] = useState(0);
  useEffect(() => {
    if (!inView) return;
    const start = performance.now();
    const dur = 1600;
    let raf = 0;
    const tick = (t: number) => {
      const p = Math.min(1, (t - start) / dur);
      const eased = 1 - Math.pow(1 - p, 3);
      setN(Math.floor(eased * to));
      if (p < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [inView, to]);
  return <span ref={ref}>{n.toLocaleString()}{suffix}</span>;
}

/* ----------------------------- Navbar ----------------------------- */

function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const links = [
    ["Home", "#home"], ["Features", "#features"], ["Solutions", "#solutions"],
    ["How it works", "#workflow"], ["Pricing", "#pricing"], ["About", "#about"], ["Contact", "#contact"],
  ] as const;

  return (
    <header className={`fixed inset-x-0 top-0 z-50 transition-all duration-300 ${scrolled ? "backdrop-blur-xl bg-white/80 border-b border-border shadow-soft" : "bg-transparent"}`}>
      <nav className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
        <a href="#home" className="flex items-center gap-2">
          <div className="grid h-9 w-9 place-items-center rounded-xl bg-primary-gradient shadow-elevated">
            <HeartPulse className="h-5 w-5 text-white" />
          </div>
          <span className="text-lg font-bold tracking-tight text-foreground">Caregraph</span>
        </a>
        <div className="hidden items-center gap-1 lg:flex">
          {links.map(([label, href]) => (
            <a key={href} href={href} className="rounded-full px-3 py-2 text-sm font-medium text-foreground/70 transition hover:bg-muted hover:text-foreground">
              {label}
            </a>
          ))}
        </div>
        <div className="hidden items-center gap-2 md:flex">
          <a href="#login" className="rounded-full px-4 py-2 text-sm font-medium text-foreground/80 hover:text-foreground">Login</a>
          <a href="#demo" className="group inline-flex items-center gap-1.5 rounded-full bg-primary-gradient px-4 py-2 text-sm font-semibold text-white shadow-elevated transition hover:shadow-glow">
            Book Demo <ArrowRight className="h-4 w-4 transition group-hover:translate-x-0.5" />
          </a>
        </div>
        <button aria-label="Toggle menu" className="lg:hidden" onClick={() => setOpen(v => !v)}>
          {open ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
        </button>
      </nav>
      <AnimatePresence>
        {open && (
          <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden border-t border-border bg-white/95 backdrop-blur-xl lg:hidden">
            <div className="flex flex-col gap-1 px-6 py-4">
              {links.map(([label, href]) => (
                <a key={href} href={href} onClick={() => setOpen(false)} className="rounded-lg px-3 py-2 text-sm font-medium text-foreground/80 hover:bg-muted">{label}</a>
              ))}
              <a href="#demo" onClick={() => setOpen(false)} className="mt-2 inline-flex items-center justify-center rounded-full bg-primary-gradient px-4 py-2.5 text-sm font-semibold text-white">Book Demo</a>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}

/* ----------------------------- Hero ----------------------------- */

function FloatingCard({ children, className = "", delay = 0, y = [0, -8, 0] }: { children: React.ReactNode; className?: string; delay?: number; y?: number[] }) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9, y: 20 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      transition={{ delay, duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
      className={className}
    >
      <motion.div animate={{ y }} transition={{ duration: 6, repeat: Infinity, ease: "easeInOut", delay }}
        className="glass-card rounded-2xl px-4 py-3 shadow-elevated">
        {children}
      </motion.div>
    </motion.div>
  );
}

function Hero() {
  const { scrollY } = useScroll();
  const y = useTransform(scrollY, [0, 500], [0, 60]);
  return (
    <div id="home" className="relative overflow-hidden pt-32 md:pt-40">
      <div className="absolute inset-0 -z-10 bg-hero-gradient" />
      <div className="absolute inset-x-0 top-0 -z-10 h-[600px] bg-[radial-gradient(60%_50%_at_50%_0%,rgba(37,99,235,0.10),transparent)]" />
      <Section className="pt-0">
        <div className="mx-auto max-w-4xl text-center">
          <Reveal>
            <Eyebrow><Sparkles className="h-3 w-3" /> Introducing Caregraph AI · Series A preview</Eyebrow>
          </Reveal>
          <Reveal delay={0.05}>
            <h1 className="mt-6 text-balance text-5xl font-black leading-[1.05] tracking-tight text-foreground md:text-7xl">
              Healthcare doesn't end after the <span className="text-gradient-primary">consultation.</span>
            </h1>
          </Reveal>
          <Reveal delay={0.15}>
            <p className="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-muted-foreground md:text-xl">
              An AI-powered Healthcare Operating System that helps hospitals manage patients, automate follow-ups, reduce missed medications, and improve outcomes — long after discharge.
            </p>
          </Reveal>
          <Reveal delay={0.25}>
            <div className="mt-9 flex flex-wrap items-center justify-center gap-3">
              <a href="#demo" className="group inline-flex items-center gap-2 rounded-full bg-primary-gradient px-6 py-3.5 text-sm font-semibold text-white shadow-elevated transition hover:shadow-glow">
                Request Demo <ArrowRight className="h-4 w-4 transition group-hover:translate-x-1" />
              </a>
              <a href="#video" className="inline-flex items-center gap-2 rounded-full border border-border bg-white/70 px-6 py-3.5 text-sm font-semibold text-foreground backdrop-blur transition hover:bg-white">
                <Play className="h-4 w-4 text-primary" /> Watch Demo
              </a>
            </div>
          </Reveal>
          <Reveal delay={0.35}>
            <p className="mt-6 text-xs text-muted-foreground">HIPAA-aligned · SOC 2 in progress · Deployed in 150+ hospitals</p>
          </Reveal>
        </div>

        {/* Dashboard mockup + floating cards */}
        <motion.div style={{ y }} className="relative mx-auto mt-20 max-w-5xl">
          <Reveal delay={0.2}>
            <div className="relative rounded-[28px] border border-border bg-white/80 p-3 shadow-elevated backdrop-blur">
              <div className="rounded-2xl bg-gradient-to-br from-white to-muted p-6 md:p-8">
                {/* Fake dashboard */}
                <div className="flex items-center justify-between border-b border-border pb-4">
                  <div className="flex items-center gap-3">
                    <div className="grid h-9 w-9 place-items-center rounded-lg bg-primary-gradient"><HeartPulse className="h-5 w-5 text-white" /></div>
                    <div>
                      <div className="text-sm font-semibold">Sunrise Multi-Specialty Hospital</div>
                      <div className="text-xs text-muted-foreground">Today · 128 appointments · 4 wards</div>
                    </div>
                  </div>
                  <div className="hidden gap-2 md:flex">
                    {["Overview", "Patients", "Queue", "AI"].map((t, i) => (
                      <span key={t} className={`rounded-full px-3 py-1 text-xs font-medium ${i === 0 ? "bg-primary text-primary-foreground" : "bg-muted text-foreground/70"}`}>{t}</span>
                    ))}
                  </div>
                </div>
                <div className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
                  {[
                    { l: "Active patients", v: "1,284", d: "+12%" },
                    { l: "Adherence", v: "94%", d: "+6%" },
                    { l: "Avg wait", v: "8m", d: "-22%" },
                    { l: "AI calls today", v: "312", d: "+41%" },
                  ].map((s) => (
                    <div key={s.l} className="rounded-xl border border-border bg-white p-4 shadow-soft">
                      <div className="text-xs text-muted-foreground">{s.l}</div>
                      <div className="mt-1 text-2xl font-bold">{s.v}</div>
                      <div className="mt-1 text-xs font-medium text-secondary">{s.d}</div>
                    </div>
                  ))}
                </div>
                <div className="mt-6 grid gap-4 md:grid-cols-3">
                  <div className="rounded-xl border border-border bg-white p-4 shadow-soft md:col-span-2">
                    <div className="mb-3 flex items-center justify-between">
                      <div className="text-sm font-semibold">Patient adherence · last 30 days</div>
                      <div className="text-xs text-muted-foreground">Powered by AI</div>
                    </div>
                    <svg viewBox="0 0 400 120" className="h-32 w-full">
                      <defs>
                        <linearGradient id="g1" x1="0" x2="0" y1="0" y2="1">
                          <stop offset="0%" stopColor="oklch(0.62 0.19 255)" stopOpacity="0.35" />
                          <stop offset="100%" stopColor="oklch(0.62 0.19 255)" stopOpacity="0" />
                        </linearGradient>
                      </defs>
                      <path d="M0,80 C40,60 80,90 120,70 C160,50 200,30 240,45 C280,60 320,25 400,20 L400,120 L0,120 Z" fill="url(#g1)" />
                      <path d="M0,80 C40,60 80,90 120,70 C160,50 200,30 240,45 C280,60 320,25 400,20" fill="none" stroke="oklch(0.56 0.20 260)" strokeWidth="2.5" strokeLinecap="round" />
                    </svg>
                  </div>
                  <div className="rounded-xl border border-border bg-white p-4 shadow-soft">
                    <div className="text-sm font-semibold">Live queue</div>
                    {[["A-14", "Dr. Rao", "Now"], ["A-15", "Dr. Iyer", "2 min"], ["A-16", "Dr. Rao", "8 min"]].map(([t, d, w]) => (
                      <div key={t} className="mt-3 flex items-center justify-between rounded-lg bg-muted px-3 py-2">
                        <div className="text-sm font-semibold">{t}</div>
                        <div className="text-xs text-muted-foreground">{d}</div>
                        <div className="text-xs font-medium text-primary">{w}</div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </Reveal>

          {/* Floating tiles */}
          <FloatingCard delay={0.6} className="absolute -left-4 top-24 hidden md:block">
            <div className="flex items-center gap-3">
              <div className="grid h-9 w-9 place-items-center rounded-lg bg-primary/10"><Pill className="h-4 w-4 text-primary" /></div>
              <div>
                <div className="text-xs text-muted-foreground">AI Reminder</div>
                <div className="text-sm font-semibold">Metformin 500mg · 9:00</div>
              </div>
            </div>
          </FloatingCard>
          <FloatingCard delay={0.8} className="absolute -right-4 top-8 hidden md:block">
            <div className="flex items-center gap-3">
              <div className="grid h-9 w-9 place-items-center rounded-lg bg-secondary/15"><PhoneCall className="h-4 w-4 text-secondary" /></div>
              <div>
                <div className="text-xs text-muted-foreground">Voice call</div>
                <div className="text-sm font-semibold">Follow-up · Mrs. Nair</div>
              </div>
            </div>
          </FloatingCard>
          <FloatingCard delay={1.0} className="absolute -right-8 bottom-16 hidden md:block">
            <div className="flex items-center gap-3">
              <div className="grid h-9 w-9 place-items-center rounded-lg bg-accent/10"><Calendar className="h-4 w-4 text-accent" /></div>
              <div>
                <div className="text-xs text-muted-foreground">Appointment</div>
                <div className="text-sm font-semibold">Dr. Rao · 3:20 PM</div>
              </div>
            </div>
          </FloatingCard>
          <FloatingCard delay={1.2} className="absolute -left-8 bottom-8 hidden md:block">
            <div className="flex items-center gap-3">
              <div className="grid h-9 w-9 place-items-center rounded-lg bg-primary/10"><LineChart className="h-4 w-4 text-primary" /></div>
              <div>
                <div className="text-xs text-muted-foreground">Analytics</div>
                <div className="text-sm font-semibold">Adherence +6%</div>
              </div>
            </div>
          </FloatingCard>
        </motion.div>
      </Section>
    </div>
  );
}

/* ----------------------------- Trust ----------------------------- */

function Trust() {
  const items = ["Apollo Meds", "Sunrise Hospital", "CareFirst Clinics", "Nova Diagnostics", "Medora Group", "Vitalis Health"];
  return (
    <div className="border-y border-border bg-muted/60">
      <div className="mx-auto max-w-7xl px-6 py-10">
        <p className="text-center text-xs font-semibold uppercase tracking-widest text-muted-foreground">
          Trusted by hospitals, doctors, clinics and patients
        </p>
        <div className="mt-6 grid grid-cols-2 items-center gap-6 opacity-70 md:grid-cols-6">
          {items.map((n) => (
            <div key={n} className="flex items-center justify-center gap-2 text-sm font-semibold text-foreground/70">
              <Hospital className="h-4 w-4" /> {n}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ----------------------------- Features ----------------------------- */

const FEATURES = [
  { icon: Bell, title: "AI Medicine Reminders", desc: "Personalized reminders that adapt to each patient's schedule and improve adherence." },
  { icon: PhoneCall, title: "Voice Call Reminders", desc: "Human-quality AI voice calls for follow-ups, medication and post-op check-ins." },
  { icon: FileText, title: "Digital Prescriptions", desc: "Structured e-prescriptions with drug interaction checks and one-tap refills." },
  { icon: Activity, title: "Patient Timeline", desc: "A unified longitudinal view of every consult, lab, medication and outcome." },
  { icon: ClipboardList, title: "Electronic Medical Records", desc: "Fast, searchable EMR designed for real clinical workflows — not paperwork." },
  { icon: Stethoscope, title: "Doctor Dashboard", desc: "Everything a physician needs in one focused view. Zero clutter, all signal." },
  { icon: UserRound, title: "Reception Dashboard", desc: "Check-ins, walk-ins, and appointments handled in seconds, not minutes." },
  { icon: Layers, title: "Queue Management", desc: "Smart routing across doctors, rooms and specialties to cut waiting times." },
  { icon: Clock, title: "Live Token Tracking", desc: "Patients know exactly when it's their turn — on screen, phone or SMS." },
  { icon: Calendar, title: "Appointment Booking", desc: "Online, WhatsApp and in-person booking with automatic reminders." },
  { icon: FlaskConical, title: "Laboratory Management", desc: "Sample-to-report tracking with instant delivery to doctor and patient." },
  { icon: Receipt, title: "Billing & Invoicing", desc: "Transparent, itemized billing with insurance and multi-payment support." },
  { icon: LineChart, title: "Analytics", desc: "Operational KPIs and clinical outcomes surfaced in real time." },
  { icon: ShieldCheck, title: "Role Based Access", desc: "Granular permissions for every doctor, nurse, admin and staff member." },
  { icon: Cloud, title: "Cloud Platform", desc: "Zero-install, always-updated infrastructure with 99.9% uptime." },
  { icon: Building2, title: "Multi Hospital Support", desc: "Manage branches, clinics and franchises from a single control plane." },
  { icon: RefreshCw, title: "Offline Synchronization", desc: "Keep working when the internet drops — everything syncs automatically." },
];

function Features() {
  return (
    <Section id="features">
      <div className="mx-auto max-w-2xl text-center">
        <Reveal><Eyebrow>Features</Eyebrow></Reveal>
        <Reveal delay={0.05}>
          <h2 className="mt-5 text-4xl font-black tracking-tight text-foreground md:text-5xl">One operating system for the entire hospital.</h2>
        </Reveal>
        <Reveal delay={0.1}>
          <p className="mt-4 text-lg text-muted-foreground">From front desk to follow-up — powered by AI, designed for people.</p>
        </Reveal>
      </div>
      <div className="mt-16 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {FEATURES.map((f, i) => (
          <Reveal key={f.title} delay={(i % 3) * 0.05}>
            <div className="group relative h-full overflow-hidden rounded-2xl border border-border bg-white p-6 shadow-soft transition-all duration-500 hover:-translate-y-1 hover:shadow-elevated">
              <div className="absolute inset-x-0 -top-px h-px bg-gradient-to-r from-transparent via-primary/40 to-transparent opacity-0 transition group-hover:opacity-100" />
              <div className="grid h-11 w-11 place-items-center rounded-xl bg-primary/10 text-primary transition group-hover:bg-primary-gradient group-hover:text-white">
                <f.icon className="h-5 w-5" />
              </div>
              <h3 className="mt-5 text-base font-semibold text-foreground">{f.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{f.desc}</p>
              <a href="#" className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-primary opacity-0 transition group-hover:opacity-100">
                Learn more <ArrowRight className="h-3.5 w-3.5" />
              </a>
            </div>
          </Reveal>
        ))}
      </div>
    </Section>
  );
}

/* ----------------------------- AI Section ----------------------------- */

function AISection() {
  const items = [
    { icon: Pill, label: "Medicine reminders" },
    { icon: Bell, label: "Follow-up reminders" },
    { icon: PhoneCall, label: "Voice calls" },
    { icon: Utensils, label: "Diet reminders" },
    { icon: Droplets, label: "Water reminders" },
    { icon: HeartPulse, label: "Health recommendations" },
    { icon: TrendingUp, label: "ML predictions" },
    { icon: Brain, label: "Adaptive care plans" },
  ];
  return (
    <div id="solutions" className="relative overflow-hidden py-24 md:py-32">
      <div className="absolute inset-0 -z-10 bg-ai-gradient" />
      <div className="absolute inset-0 -z-10 opacity-40 [background-image:radial-gradient(circle_at_1px_1px,rgba(255,255,255,0.15)_1px,transparent_0)] [background-size:24px_24px]" />
      <div className="mx-auto max-w-7xl px-6">
        <div className="grid items-center gap-16 lg:grid-cols-2">
          <div className="text-white">
            <Reveal>
              <div className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs font-medium backdrop-blur">
                <Sparkles className="h-3 w-3" /> Caregraph AI
              </div>
            </Reveal>
            <Reveal delay={0.05}>
              <h2 className="mt-5 text-4xl font-black leading-tight tracking-tight md:text-6xl">
                AI that continues caring<br /> after the patient leaves.
              </h2>
            </Reveal>
            <Reveal delay={0.1}>
              <p className="mt-5 max-w-xl text-lg text-white/80">
                Our AI engine keeps the care loop closed — nudging patients on medications, water, diet, and follow-ups, and calling them with a real human voice when it matters.
              </p>
            </Reveal>
            <Reveal delay={0.2}>
              <div className="mt-8 grid grid-cols-2 gap-3">
                {items.map((it) => (
                  <div key={it.label} className="flex items-center gap-3 rounded-xl border border-white/15 bg-white/10 p-3 backdrop-blur transition hover:bg-white/15">
                    <div className="grid h-9 w-9 place-items-center rounded-lg bg-white/15"><it.icon className="h-4 w-4" /></div>
                    <span className="text-sm font-medium">{it.label}</span>
                  </div>
                ))}
              </div>
            </Reveal>
          </div>

          {/* Illustration */}
          <Reveal delay={0.15}>
            <div className="relative mx-auto aspect-square w-full max-w-md">
              <div className="absolute inset-0 rounded-full bg-white/10 blur-3xl" />
              <motion.div animate={{ rotate: 360 }} transition={{ duration: 40, repeat: Infinity, ease: "linear" }}
                className="absolute inset-4 rounded-full border border-white/20" />
              <motion.div animate={{ rotate: -360 }} transition={{ duration: 60, repeat: Infinity, ease: "linear" }}
                className="absolute inset-12 rounded-full border border-white/20" />
              <div className="absolute inset-0 grid place-items-center">
                <div className="grid h-32 w-32 place-items-center rounded-full bg-white/15 backdrop-blur-xl">
                  <Brain className="h-14 w-14 text-white" />
                </div>
              </div>
              {[
                { i: Pill, pos: "top-4 left-1/2 -translate-x-1/2" },
                { i: PhoneCall, pos: "right-4 top-1/2 -translate-y-1/2" },
                { i: Droplets, pos: "bottom-4 left-1/2 -translate-x-1/2" },
                { i: HeartPulse, pos: "left-4 top-1/2 -translate-y-1/2" },
                { i: Mic, pos: "top-12 right-12" },
                { i: Utensils, pos: "bottom-12 left-12" },
              ].map(({ i: Icon, pos }, k) => (
                <motion.div key={k} animate={{ y: [0, -8, 0] }} transition={{ duration: 4 + k, repeat: Infinity, ease: "easeInOut" }}
                  className={`absolute ${pos} grid h-12 w-12 place-items-center rounded-2xl border border-white/20 bg-white/15 text-white backdrop-blur-xl`}>
                  <Icon className="h-5 w-5" />
                </motion.div>
              ))}
            </div>
          </Reveal>
        </div>
      </div>
    </div>
  );
}

/* ----------------------------- Workflow ----------------------------- */

const FLOW = [
  { icon: Calendar, title: "Patient Books Appointment", desc: "Online, WhatsApp, phone or walk-in." },
  { icon: ClipboardCheck, title: "Reception Check-In", desc: "One-tap check-in and instant token." },
  { icon: Stethoscope, title: "Doctor Consultation", desc: "Focused clinical view, no clutter." },
  { icon: FileText, title: "Digital Prescription", desc: "Structured, safe, sent instantly." },
  { icon: Pill, title: "Medicine Reminder", desc: "Personalized nudges begin at home." },
  { icon: PhoneCall, title: "Voice Call Reminder", desc: "AI voice calls for missed doses." },
  { icon: Bell, title: "Follow-up Reminder", desc: "Right nudge, right patient, right time." },
  { icon: Activity, title: "Health Timeline", desc: "A living record of every outcome." },
  { icon: Brain, title: "AI Predictions", desc: "Anticipate risk before it becomes a crisis." },
];

function Workflow() {
  return (
    <Section id="workflow">
      <div className="mx-auto max-w-2xl text-center">
        <Reveal><Eyebrow>How it works</Eyebrow></Reveal>
        <Reveal delay={0.05}><h2 className="mt-5 text-4xl font-black tracking-tight md:text-5xl">A continuous loop of care.</h2></Reveal>
        <Reveal delay={0.1}><p className="mt-4 text-lg text-muted-foreground">From booking to follow-up, every step is designed to keep patients well.</p></Reveal>
      </div>
      <div className="mt-16 grid gap-4 md:grid-cols-3">
        {FLOW.map((s, i) => (
          <Reveal key={s.title} delay={(i % 3) * 0.06}>
            <div className="relative flex h-full items-start gap-4 rounded-2xl border border-border bg-white p-5 shadow-soft transition hover:-translate-y-1 hover:shadow-elevated">
              <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-primary-gradient text-white shadow-elevated">
                <s.icon className="h-5 w-5" />
              </div>
              <div className="min-w-0">
                <div className="text-xs font-semibold text-primary">Step {String(i + 1).padStart(2, "0")}</div>
                <div className="mt-1 text-base font-semibold text-foreground">{s.title}</div>
                <div className="mt-1 text-sm text-muted-foreground">{s.desc}</div>
              </div>
            </div>
          </Reveal>
        ))}
      </div>
    </Section>
  );
}

/* ----------------------------- Why Us ----------------------------- */

function WhyUs() {
  const points = [
    "Reduce missed appointments by up to 40%",
    "Improve medicine adherence with AI reminders",
    "Higher patient satisfaction and retention",
    "Cut receptionist workload in half",
    "AI-powered clinical and operational automation",
    "Secure cloud, always-on, always up to date",
    "Mobile-friendly for staff and patients",
    "Affordable pricing built for Tier-2 hospitals",
  ];
  return (
    <Section id="about">
      <div className="grid items-center gap-16 lg:grid-cols-2">
        <Reveal>
          <div className="relative">
            <div className="rounded-3xl border border-border bg-gradient-to-br from-primary/5 via-white to-secondary/10 p-8 shadow-soft">
              <div className="grid gap-4 sm:grid-cols-2">
                {[
                  { i: HeartPulse, l: "Adherence", v: "94%" },
                  { i: Clock, l: "Wait time", v: "-38%" },
                  { i: PhoneCall, l: "Auto calls / day", v: "12k" },
                  { i: Users, l: "Active users", v: "48k" },
                ].map((s) => (
                  <div key={s.l} className="rounded-2xl border border-border bg-white p-5 shadow-soft">
                    <s.i className="h-5 w-5 text-primary" />
                    <div className="mt-3 text-2xl font-bold">{s.v}</div>
                    <div className="text-xs text-muted-foreground">{s.l}</div>
                  </div>
                ))}
              </div>
              <div className="mt-5 flex items-center gap-3 rounded-2xl border border-border bg-white p-4 shadow-soft">
                <div className="grid h-10 w-10 place-items-center rounded-xl bg-secondary/15 text-secondary"><Brain className="h-5 w-5" /></div>
                <div>
                  <div className="text-sm font-semibold">Caregraph AI predicts readmission risk</div>
                  <div className="text-xs text-muted-foreground">Continuously, for every patient, in real time.</div>
                </div>
              </div>
            </div>
          </div>
        </Reveal>
        <div>
          <Reveal><Eyebrow>Why Caregraph</Eyebrow></Reveal>
          <Reveal delay={0.05}><h2 className="mt-5 text-4xl font-black tracking-tight md:text-5xl">Built for hospitals that actually run 24/7.</h2></Reveal>
          <Reveal delay={0.1}><p className="mt-4 text-lg text-muted-foreground">Every feature is designed with clinicians, receptionists and patients — not accountants.</p></Reveal>
          <ul className="mt-8 grid gap-3 sm:grid-cols-2">
            {points.map((p, i) => (
              <Reveal key={p} delay={0.1 + i * 0.03}>
                <li className="flex items-start gap-3 rounded-xl border border-border bg-white p-4 shadow-soft">
                  <div className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-secondary text-white"><Check className="h-3.5 w-3.5" /></div>
                  <span className="text-sm font-medium text-foreground">{p}</span>
                </li>
              </Reveal>
            ))}
          </ul>
        </div>
      </div>
    </Section>
  );
}

/* ----------------------------- Modules ----------------------------- */

const MODULES = [
  { i: HeartPulse, n: "Patient App" },
  { i: Stethoscope, n: "Doctor Portal" },
  { i: UserRound, n: "Reception" },
  { i: ClipboardList, n: "Nurse" },
  { i: FlaskConical, n: "Laboratory" },
  { i: Receipt, n: "Billing" },
  { i: ShieldCheck, n: "Admin" },
  { i: LineChart, n: "Analytics" },
  { i: Brain, n: "AI Engine" },
  { i: Mic, n: "Voice Engine" },
];

function Modules() {
  return (
    <Section>
      <div className="mx-auto max-w-2xl text-center">
        <Reveal><Eyebrow>Product modules</Eyebrow></Reveal>
        <Reveal delay={0.05}><h2 className="mt-5 text-4xl font-black tracking-tight md:text-5xl">Ten modules. One platform.</h2></Reveal>
      </div>
      <div className="mt-14 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        {MODULES.map((m, i) => (
          <Reveal key={m.n} delay={(i % 5) * 0.04}>
            <div className="group flex flex-col items-center gap-3 rounded-2xl border border-border bg-white p-6 text-center shadow-soft transition hover:-translate-y-1 hover:shadow-elevated">
              <div className="grid h-12 w-12 place-items-center rounded-2xl bg-primary/10 text-primary transition group-hover:bg-primary-gradient group-hover:text-white">
                <m.i className="h-6 w-6" />
              </div>
              <div className="text-sm font-semibold">{m.n}</div>
            </div>
          </Reveal>
        ))}
      </div>
    </Section>
  );
}

/* ----------------------------- Stats ----------------------------- */

function Stats() {
  const s = [
    { n: 10000, s: "+", l: "Patients managed" },
    { n: 150, s: "+", l: "Hospitals" },
    { n: 98, s: "%", l: "Patient satisfaction" },
    { n: 60, s: "%", l: "Reduction in missed follow-ups" },
  ];
  return (
    <div className="border-y border-border bg-muted/60">
      <div className="mx-auto grid max-w-7xl grid-cols-2 gap-8 px-6 py-16 md:grid-cols-4">
        {s.map((x) => (
          <div key={x.l} className="text-center">
            <div className="text-4xl font-black tracking-tight text-foreground md:text-5xl">
              <Counter to={x.n} suffix={x.s} />
            </div>
            <div className="mt-2 text-sm text-muted-foreground">{x.l}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ----------------------------- Testimonials ----------------------------- */

const TESTIS = [
  { name: "Dr. Anjali Rao", role: "Cardiologist, Sunrise Hospital", quote: "Caregraph gave me back two hours a day. My patients get reminders I would never have time to send myself." },
  { name: "Rahul Menon", role: "Hospital Administrator, Medora", quote: "We reduced no-shows by 41% in the first quarter. The AI voice calls alone paid for the platform." },
  { name: "Priya Nair", role: "Receptionist, CareFirst", quote: "Check-ins that took five minutes now take twenty seconds. Patients don't yell at us anymore." },
  { name: "Ismail Kadam", role: "Patient", quote: "It called me to remind me about my medicine and my follow-up. It felt like the hospital never forgot about me." },
];

function Testimonials() {
  const [i, setI] = useState(0);
  useEffect(() => {
    const t = setInterval(() => setI((v) => (v + 1) % TESTIS.length), 6000);
    return () => clearInterval(t);
  }, []);
  const t = TESTIS[i];
  return (
    <Section>
      <div className="mx-auto max-w-2xl text-center">
        <Reveal><Eyebrow>Testimonials</Eyebrow></Reveal>
        <Reveal delay={0.05}><h2 className="mt-5 text-4xl font-black tracking-tight md:text-5xl">Loved across every seat in the hospital.</h2></Reveal>
      </div>
      <div className="mx-auto mt-14 max-w-4xl">
        <div className="relative rounded-3xl border border-border bg-white p-8 shadow-soft md:p-12">
          <AnimatePresence mode="wait">
            <motion.div key={i} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -12 }} transition={{ duration: 0.4 }}>
              <div className="flex items-center gap-1 text-secondary">
                {[...Array(5)].map((_, k) => <Star key={k} className="h-4 w-4 fill-current" />)}
              </div>
              <blockquote className="mt-5 text-xl font-medium leading-relaxed text-foreground md:text-2xl">
                “{t.quote}”
              </blockquote>
              <div className="mt-6 flex items-center gap-3">
                <div className="grid h-11 w-11 place-items-center rounded-full bg-primary-gradient text-sm font-bold text-white">
                  {t.name.split(" ").map(x => x[0]).slice(0, 2).join("")}
                </div>
                <div>
                  <div className="text-sm font-semibold">{t.name}</div>
                  <div className="text-xs text-muted-foreground">{t.role}</div>
                </div>
              </div>
            </motion.div>
          </AnimatePresence>
          <div className="mt-8 flex items-center justify-center gap-2">
            {TESTIS.map((_, k) => (
              <button key={k} aria-label={`Testimonial ${k + 1}`} onClick={() => setI(k)}
                className={`h-1.5 rounded-full transition-all ${k === i ? "w-8 bg-primary" : "w-2 bg-border"}`} />
            ))}
          </div>
        </div>
      </div>
    </Section>
  );
}

/* ----------------------------- Pricing ----------------------------- */

const PLANS = [
  { name: "Starter", price: "₹14,999", per: "/mo", desc: "For small clinics getting started.", featured: false,
    features: ["Up to 3 doctors", "Reception + Doctor portal", "Patient app", "500 AI reminders / mo", "Email support"] },
  { name: "Professional", price: "₹39,999", per: "/mo", desc: "For growing multi-specialty hospitals.", featured: true,
    features: ["Up to 20 doctors", "All modules included", "AI voice calls + follow-ups", "10,000 reminders / mo", "Priority support", "Analytics dashboard"] },
  { name: "Enterprise", price: "Custom", per: "", desc: "For hospital groups and chains.", featured: false,
    features: ["Unlimited doctors", "Multi-hospital control plane", "Custom AI workflows", "SLA + dedicated CSM", "SSO + audit logs", "On-prem or private cloud"] },
];

function Pricing() {
  return (
    <Section id="pricing">
      <div className="mx-auto max-w-2xl text-center">
        <Reveal><Eyebrow>Pricing</Eyebrow></Reveal>
        <Reveal delay={0.05}><h2 className="mt-5 text-4xl font-black tracking-tight md:text-5xl">Simple, transparent pricing.</h2></Reveal>
        <Reveal delay={0.1}><p className="mt-4 text-lg text-muted-foreground">Every plan includes updates, security patches and cloud hosting.</p></Reveal>
      </div>
      <div className="mt-16 grid gap-6 lg:grid-cols-3">
        {PLANS.map((p, i) => (
          <Reveal key={p.name} delay={i * 0.06}>
            <div className={`relative flex h-full flex-col rounded-3xl border p-8 shadow-soft transition hover:-translate-y-1 hover:shadow-elevated ${p.featured ? "border-primary bg-primary-gradient text-white shadow-elevated" : "border-border bg-white"}`}>
              {p.featured && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-white px-3 py-1 text-xs font-semibold text-primary shadow-soft">Most popular</div>
              )}
              <div className={`text-sm font-semibold ${p.featured ? "text-white/80" : "text-primary"}`}>{p.name}</div>
              <div className="mt-4 flex items-baseline gap-1">
                <span className="text-4xl font-black tracking-tight">{p.price}</span>
                <span className={`text-sm ${p.featured ? "text-white/70" : "text-muted-foreground"}`}>{p.per}</span>
              </div>
              <p className={`mt-2 text-sm ${p.featured ? "text-white/80" : "text-muted-foreground"}`}>{p.desc}</p>
              <ul className="mt-6 space-y-3">
                {p.features.map((f) => (
                  <li key={f} className="flex items-start gap-2 text-sm">
                    <Check className={`mt-0.5 h-4 w-4 shrink-0 ${p.featured ? "text-white" : "text-secondary"}`} />
                    <span>{f}</span>
                  </li>
                ))}
              </ul>
              <a href="#demo" className={`mt-8 inline-flex items-center justify-center gap-2 rounded-full px-5 py-3 text-sm font-semibold transition ${p.featured ? "bg-white text-primary hover:shadow-glow" : "bg-primary-gradient text-white hover:shadow-elevated"}`}>
                Request Demo <ArrowRight className="h-4 w-4" />
              </a>
            </div>
          </Reveal>
        ))}
      </div>
    </Section>
  );
}

/* ----------------------------- FAQ ----------------------------- */

const FAQ = [
  { q: "How does Caregraph secure patient data?", a: "All data is encrypted in transit and at rest. We are HIPAA-aligned, SOC 2 in progress, and support role-based access, audit logs and single sign-on for enterprise plans." },
  { q: "Where is the data stored?", a: "By default, data is stored on region-specific cloud infrastructure. Enterprise customers can choose private cloud or on-prem deployments." },
  { q: "How does the AI voice call system work?", a: "Our AI voice engine places human-quality calls for reminders and follow-ups. Patients can respond naturally and outcomes are logged back into the timeline." },
  { q: "Do you offer support during onboarding?", a: "Yes. Every hospital gets a dedicated implementation manager, staff training and 30 days of hyper-care support." },
  { q: "Can Caregraph integrate with our existing HIS or lab systems?", a: "Yes. We ship connectors for common HIS, LIS and pharmacy systems, and expose secure APIs for anything custom." },
  { q: "What does pricing include?", a: "All plans include cloud hosting, security patches, updates and standard support. AI voice call minutes are metered separately on Professional and above." },
];

function Faq() {
  const [open, setOpen] = useState<number | null>(0);
  return (
    <Section>
      <div className="mx-auto max-w-2xl text-center">
        <Reveal><Eyebrow>FAQ</Eyebrow></Reveal>
        <Reveal delay={0.05}><h2 className="mt-5 text-4xl font-black tracking-tight md:text-5xl">Everything you might ask.</h2></Reveal>
      </div>
      <div className="mx-auto mt-12 max-w-3xl divide-y divide-border rounded-3xl border border-border bg-white shadow-soft">
        {FAQ.map((f, i) => {
          const isOpen = open === i;
          return (
            <div key={f.q}>
              <button onClick={() => setOpen(isOpen ? null : i)}
                className="flex w-full items-center justify-between gap-6 px-6 py-5 text-left">
                <span className="text-base font-semibold text-foreground">{f.q}</span>
                <ChevronDown className={`h-4 w-4 shrink-0 text-muted-foreground transition ${isOpen ? "rotate-180" : ""}`} />
              </button>
              <AnimatePresence initial={false}>
                {isOpen && (
                  <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.3 }} className="overflow-hidden">
                    <p className="px-6 pb-5 text-sm leading-relaxed text-muted-foreground">{f.a}</p>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          );
        })}
      </div>
    </Section>
  );
}

/* ----------------------------- Final CTA + Footer ----------------------------- */

function FinalCTA() {
  return (
    <Section id="demo">
      <div className="relative overflow-hidden rounded-[32px] border border-border bg-primary-gradient p-10 text-center text-white shadow-elevated md:p-20">
        <div className="absolute inset-0 opacity-30 [background-image:radial-gradient(circle_at_1px_1px,rgba(255,255,255,0.25)_1px,transparent_0)] [background-size:24px_24px]" />
        <div className="relative">
          <Reveal><h2 className="text-4xl font-black tracking-tight md:text-6xl">Ready to transform patient care?</h2></Reveal>
          <Reveal delay={0.05}><p className="mx-auto mt-4 max-w-2xl text-lg text-white/85">Join 150+ hospitals modernizing operations and continuing care after discharge with Caregraph.</p></Reveal>
          <Reveal delay={0.1}>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
              <a href="#book" className="inline-flex items-center gap-2 rounded-full bg-white px-6 py-3.5 text-sm font-semibold text-primary shadow-elevated transition hover:shadow-glow">
                Book Free Demo <ArrowRight className="h-4 w-4" />
              </a>
              <a href="#contact" className="inline-flex items-center gap-2 rounded-full border border-white/30 bg-white/10 px-6 py-3.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/20">
                Contact Sales
              </a>
            </div>
          </Reveal>
        </div>
      </div>
    </Section>
  );
}

function Footer() {
  const cols = [
    { t: "Product", l: ["Features", "Solutions", "Pricing", "Modules", "Changelog"] },
    { t: "Resources", l: ["Docs", "Case studies", "Guides", "API reference", "Status"] },
    { t: "Company", l: ["About", "Careers", "Press", "Partners", "Contact"] },
    { t: "Legal", l: ["Privacy Policy", "Terms of Service", "Data Processing", "Security"] },
  ];
  return (
    <footer id="contact" className="border-t border-border bg-muted/60">
      <div className="mx-auto max-w-7xl px-6 py-16">
        <div className="grid gap-12 lg:grid-cols-[1.4fr_2fr_1.2fr]">
          <div>
            <a href="#home" className="flex items-center gap-2">
              <div className="grid h-9 w-9 place-items-center rounded-xl bg-primary-gradient shadow-elevated"><HeartPulse className="h-5 w-5 text-white" /></div>
              <span className="text-lg font-bold">Caregraph</span>
            </a>
            <p className="mt-4 max-w-xs text-sm text-muted-foreground">
              The AI-powered Healthcare Operating System for modern hospitals.
            </p>
            <div className="mt-5 flex gap-3">
              {[Twitter, Linkedin, Github].map((I, k) => (
                <a key={k} href="#" aria-label="Social" className="grid h-9 w-9 place-items-center rounded-full border border-border bg-white text-foreground/70 transition hover:text-primary">
                  <I className="h-4 w-4" />
                </a>
              ))}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-8 sm:grid-cols-4">
            {cols.map((c) => (
              <div key={c.t}>
                <div className="text-xs font-semibold uppercase tracking-widest text-foreground">{c.t}</div>
                <ul className="mt-4 space-y-2">
                  {c.l.map((x) => (
                    <li key={x}><a href="#" className="text-sm text-muted-foreground transition hover:text-primary">{x}</a></li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
          <div>
            <div className="text-xs font-semibold uppercase tracking-widest text-foreground">Newsletter</div>
            <p className="mt-4 text-sm text-muted-foreground">Monthly insights on AI, healthcare and product. No spam.</p>
            <form onSubmit={(e) => e.preventDefault()} className="mt-4 flex overflow-hidden rounded-full border border-border bg-white shadow-soft">
              <input type="email" required placeholder="you@hospital.com" aria-label="Email"
                className="w-full bg-transparent px-4 py-2.5 text-sm outline-none placeholder:text-muted-foreground" />
              <button className="shrink-0 bg-primary-gradient px-4 text-sm font-semibold text-white">Subscribe</button>
            </form>
          </div>
        </div>
        <div className="mt-12 flex flex-col items-center justify-between gap-3 border-t border-border pt-6 text-xs text-muted-foreground md:flex-row">
          <div>© {new Date().getFullYear()} Caregraph Health Systems. All rights reserved.</div>
          <div className="flex gap-4">
            <a href="#" className="hover:text-primary">Privacy</a>
            <a href="#" className="hover:text-primary">Terms</a>
            <a href="#" className="hover:text-primary">Status</a>
          </div>
        </div>
      </div>
    </footer>
  );
}

/* ----------------------------- Page ----------------------------- */

export function Landing() {
  return (
    <main className="min-h-screen bg-background text-foreground">
      <Navbar />
      <Hero />
      <Trust />
      <Features />
      <AISection />
      <Workflow />
      <WhyUs />
      <Modules />
      <Stats />
      <Testimonials />
      <Pricing />
      <Faq />
      <FinalCTA />
      <Footer />
    </main>
  );
}