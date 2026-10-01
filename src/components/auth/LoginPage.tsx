import { useState } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { motion, AnimatePresence } from "framer-motion";
import {
  Stethoscope,
  Users,
  ShieldCheck,
  HeartPulse,
  Eye,
  EyeOff,
  ArrowLeft,
  ArrowRight,
  Lock,
  Mail,
  CheckCircle2,
} from "lucide-react";

/* ── Types ─────────────────────────────────────────────────── */

type Role = "doctor" | "staff" | "admin";

interface RoleCard {
  id: Role;
  label: string;
  subtitle: string;
  icon: React.ElementType;
  gradient: string;
  iconBg: string;
  description: string;
}

/* ── Role definitions ───────────────────────────────────────── */

const ROLES: RoleCard[] = [
  {
    id: "doctor",
    label: "Doctor",
    subtitle: "Clinical Portal",
    icon: Stethoscope,
    gradient: "from-blue-500 to-blue-700",
    iconBg: "bg-blue-50 text-blue-600",
    description: "Access patient records, prescriptions & consultations",
  },
  {
    id: "staff",
    label: "Staff",
    subtitle: "Reception & Nursing",
    icon: Users,
    gradient: "from-emerald-500 to-teal-600",
    iconBg: "bg-emerald-50 text-emerald-600",
    description: "Manage appointments, queue & patient check-ins",
  },
  {
    id: "admin",
    label: "Admin",
    subtitle: "Control Panel",
    icon: ShieldCheck,
    gradient: "from-violet-500 to-purple-700",
    iconBg: "bg-violet-50 text-violet-600",
    description: "Full system access, analytics & hospital settings",
  },
];

/* ── Animated background orbs ──────────────────────────────── */

function BackgroundOrbs() {
  return (
    <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
      <div className="absolute -top-40 -right-40 h-[600px] w-[600px] rounded-full bg-blue-100/60 blur-[100px]" />
      <div className="absolute -bottom-40 -left-40 h-[500px] w-[500px] rounded-full bg-violet-100/50 blur-[100px]" />
      <div className="absolute top-1/2 left-1/2 h-[300px] w-[300px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-teal-100/40 blur-[80px]" />
    </div>
  );
}

/* ── Role Selector Card ─────────────────────────────────────── */

function RoleCardItem({
  role,
  selected,
  onSelect,
  index,
}: {
  role: RoleCard;
  selected: boolean;
  onSelect: () => void;
  index: number;
}) {
  const Icon = role.icon;
  return (
    <motion.button
      id={`role-${role.id}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.1 + index * 0.07, duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
      onClick={onSelect}
      className={`group relative w-full overflow-hidden rounded-2xl border p-5 text-left transition-all duration-300 ${
        selected
          ? "border-transparent shadow-lg ring-2 ring-blue-500/70"
          : "border-gray-200 bg-white/70 hover:border-gray-300 hover:shadow-md"
      }`}
      style={{ backdropFilter: "blur(12px)" }}
    >
      {/* Gradient fill when selected */}
      <AnimatePresence>
        {selected && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className={`absolute inset-0 bg-gradient-to-br ${role.gradient} opacity-[0.08]`}
          />
        )}
      </AnimatePresence>

      <div className="relative flex items-start gap-4">
        {/* Icon */}
        <div
          className={`grid h-11 w-11 shrink-0 place-items-center rounded-xl transition-all duration-300 ${
            selected ? `bg-gradient-to-br ${role.gradient} text-white shadow-md` : role.iconBg
          }`}
        >
          <Icon className="h-5 w-5" />
        </div>

        {/* Text */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm font-bold text-gray-900">{role.label}</div>
              <div className="text-xs text-gray-500">{role.subtitle}</div>
            </div>
            {selected && (
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", stiffness: 400, damping: 20 }}
              >
                <CheckCircle2 className="h-5 w-5 text-blue-600" />
              </motion.div>
            )}
          </div>
          <p className="mt-1.5 text-xs leading-relaxed text-gray-500">{role.description}</p>
        </div>
      </div>
    </motion.button>
  );
}

/* ── Main Login Page ────────────────────────────────────────── */

export function LoginPage() {
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRole) { setError("Please select your role first."); return; }
    if (!email.trim()) { setError("Please enter your email."); return; }
    if (!password) { setError("Please enter your password."); return; }
    setError("");
    setLoading(true);
    // Simulate async login
    await new Promise((r) => setTimeout(r, 1200));
    setLoading(false);
    setSuccess(true);
    // Navigate to dashboard after short delay (extend when real auth is added)
    setTimeout(() => navigate({ to: "/" }), 1800);
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center px-4 py-12" style={{ background: "linear-gradient(135deg, #f0f4ff 0%, #fafaff 50%, #f0fff4 100%)" }}>
      <BackgroundOrbs />

      {/* Back to home */}
      <Link
        to="/"
        className="absolute left-6 top-6 flex items-center gap-2 rounded-full border border-gray-200 bg-white/80 px-4 py-2 text-sm font-medium text-gray-600 shadow-sm backdrop-blur transition hover:bg-white hover:text-gray-900"
      >
        <ArrowLeft className="h-4 w-4" /> Back to Home
      </Link>

      <div className="w-full max-w-md">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="mb-8 text-center"
        >
          <a href="/" className="inline-flex items-center justify-center gap-2 mb-4">
            <div className="grid h-10 w-10 place-items-center rounded-xl bg-gradient-to-br from-blue-500 to-blue-700 shadow-lg">
              <HeartPulse className="h-5 w-5 text-white" />
            </div>
            <span className="text-xl font-black tracking-tight text-gray-900">Caregraph</span>
          </a>
          <h1 className="text-2xl font-black tracking-tight text-gray-900">Welcome back</h1>
          <p className="mt-1 text-sm text-gray-500">Sign in to your Caregraph account</p>
        </motion.div>

        {/* Card */}
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05, duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="rounded-3xl border border-white/60 bg-white/75 p-8 shadow-xl"
          style={{ backdropFilter: "blur(20px)" }}
        >
          <AnimatePresence mode="wait">
            {success ? (
              /* ── Success state ── */
              <motion.div
                key="success"
                initial={{ opacity: 0, scale: 0.92 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex flex-col items-center gap-4 py-6 text-center"
              >
                <div className="grid h-16 w-16 place-items-center rounded-full bg-emerald-100">
                  <CheckCircle2 className="h-8 w-8 text-emerald-600" />
                </div>
                <div>
                  <div className="text-lg font-bold text-gray-900">Signed in!</div>
                  <div className="mt-1 text-sm text-gray-500">Redirecting to your dashboard…</div>
                </div>
              </motion.div>
            ) : (
              <motion.div key="form">
                {/* Step 1 — Role selection */}
                <div className="mb-6">
                  <div className="mb-3 flex items-center gap-2">
                    <span className="flex h-5 w-5 items-center justify-center rounded-full bg-blue-600 text-[10px] font-bold text-white">1</span>
                    <span className="text-sm font-semibold text-gray-700">Select your role</span>
                  </div>
                  <div className="flex flex-col gap-3">
                    {ROLES.map((role, i) => (
                      <RoleCardItem
                        key={role.id}
                        role={role}
                        index={i}
                        selected={selectedRole === role.id}
                        onSelect={() => { setSelectedRole(role.id); setError(""); }}
                      />
                    ))}
                  </div>
                </div>

                {/* Step 2 — Credentials */}
                <AnimatePresence>
                  {selectedRole && (
                    <motion.div
                      key="creds"
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: "auto" }}
                      exit={{ opacity: 0, height: 0 }}
                      transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
                      className="overflow-hidden"
                    >
                      <form onSubmit={handleSubmit} className="mt-2">
                        <div className="mb-4 flex items-center gap-2">
                          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-blue-600 text-[10px] font-bold text-white">2</span>
                          <span className="text-sm font-semibold text-gray-700">Enter your credentials</span>
                        </div>

                        {/* Email */}
                        <div className="mb-3">
                          <label htmlFor="login-email" className="mb-1.5 block text-xs font-semibold text-gray-600">
                            Email address
                          </label>
                          <div className="relative">
                            <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                            <input
                              id="login-email"
                              type="email"
                              autoComplete="email"
                              value={email}
                              onChange={(e) => setEmail(e.target.value)}
                              placeholder="you@hospital.com"
                              className="w-full rounded-xl border border-gray-200 bg-white py-2.5 pl-10 pr-4 text-sm text-gray-900 placeholder-gray-400 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                            />
                          </div>
                        </div>

                        {/* Password */}
                        <div className="mb-5">
                          <div className="mb-1.5 flex items-center justify-between">
                            <label htmlFor="login-password" className="text-xs font-semibold text-gray-600">
                              Password
                            </label>
                            <a href="#" className="text-xs font-medium text-blue-600 hover:underline">
                              Forgot password?
                            </a>
                          </div>
                          <div className="relative">
                            <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                            <input
                              id="login-password"
                              type={showPassword ? "text" : "password"}
                              autoComplete="current-password"
                              value={password}
                              onChange={(e) => setPassword(e.target.value)}
                              placeholder="••••••••"
                              className="w-full rounded-xl border border-gray-200 bg-white py-2.5 pl-10 pr-11 text-sm text-gray-900 placeholder-gray-400 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                            />
                            <button
                              type="button"
                              id="toggle-password-visibility"
                              onClick={() => setShowPassword((v) => !v)}
                              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 transition hover:text-gray-600"
                            >
                              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                            </button>
                          </div>
                        </div>

                        {/* Error */}
                        <AnimatePresence>
                          {error && (
                            <motion.div
                              initial={{ opacity: 0, y: -6 }}
                              animate={{ opacity: 1, y: 0 }}
                              exit={{ opacity: 0, y: -6 }}
                              className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-600"
                            >
                              {error}
                            </motion.div>
                          )}
                        </AnimatePresence>

                        {/* Submit */}
                        <button
                          id="sign-in-btn"
                          type="submit"
                          disabled={loading}
                          className="group flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-500 to-blue-700 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-blue-500/30 transition hover:shadow-blue-500/50 disabled:opacity-70"
                        >
                          {loading ? (
                            <>
                              <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                              </svg>
                              Signing in…
                            </>
                          ) : (
                            <>
                              Sign in as {ROLES.find((r) => r.id === selectedRole)?.label}
                              <ArrowRight className="h-4 w-4 transition group-hover:translate-x-0.5" />
                            </>
                          )}
                        </button>
                      </form>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>

        {/* Footer */}
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="mt-5 text-center text-xs text-gray-400"
        >
          HIPAA-aligned &nbsp;·&nbsp; SOC 2 in progress &nbsp;·&nbsp; Encrypted at rest
        </motion.p>
      </div>
    </div>
  );
}
