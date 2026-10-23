import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { toast } from "sonner";
import { isAuthenticated, isDoctor } from "@/lib/auth";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  Legend,
} from "recharts";
import {
  Activity,
  AlertCircle,
  CalendarCheck,
  ClipboardList,
  FlaskConical,
  Loader2,
  Stethoscope,
  TrendingUp,
  Users,
  Wallet,
} from "lucide-react";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { StatCard } from "@/components/dashboard/stat-card";
import {
  useDoctorAnalyticsSummary,
  useDoctorAnalyticsTrends,
} from "@/services/analytics-api";
import type { DailyTrendPoint, ConsultationOutcomeSummary } from "@/services/analytics-api";

export const Route = createFileRoute("/analytics")({
  head: () => ({
    meta: [
      { title: "Doctor Analytics — Meridian Health OS" },
      {
        name: "description",
        content:
          "Live consultation volume, patient counts, revenue trends and consultation outcomes derived from real clinical data.",
      },
      { property: "og:title", content: "Doctor Analytics — Meridian Health OS" },
      { property: "og:description", content: "Practice performance analytics for clinicians." },
    ],
  }),
  component: Analytics,
});

/* ─── constants ─────────────────────────────────────────────────────────────── */

const PIE_COLORS = [
  "var(--chart-1)",
  "var(--chart-2)",
  "var(--chart-3)",
  "var(--chart-4)",
  "var(--chart-5)",
];

const DATE_RANGES = [
  { label: "7 days", days: 6 },
  { label: "14 days", days: 13 },
  { label: "30 days", days: 29 },
  { label: "90 days", days: 89 },
];

/* ─── helpers ───────────────────────────────────────────────────────────────── */

function toISODate(d: Date) {
  return d.toISOString().split("T")[0];
}

function formatDateLabel(dateStr: string) {
  const d = new Date(dateStr + "T00:00:00");
  return d.toLocaleDateString("en-IN", { month: "short", day: "numeric" });
}

function formatCurrency(n: number): string {
  if (n >= 100_000) return `₹${(n / 100_000).toFixed(1)}L`;
  if (n >= 1_000) return `₹${(n / 1_000).toFixed(1)}K`;
  return `₹${n}`;
}

/* ─── sub-components ────────────────────────────────────────────────────────── */

function SectionSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <div className="surface-card rounded-2xl p-5 animate-pulse space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="h-4 rounded bg-muted" style={{ width: `${70 + (i % 3) * 10}%` }} />
      ))}
    </div>
  );
}

function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-destructive/30 bg-destructive/8 px-5 py-4 text-sm text-destructive">
      <AlertCircle className="size-4 shrink-0" />
      <span>{message}</span>
    </div>
  );
}

function QueueBadge({ label, count, color }: { label: string; count: number; color: string }) {
  return (
    <div className="flex flex-col items-center gap-1 rounded-xl border bg-card/50 px-4 py-3">
      <span className="num text-xl font-bold" style={{ color }}>
        {count}
      </span>
      <span className="text-[11px] uppercase tracking-wide text-muted-foreground">{label}</span>
    </div>
  );
}

function OutcomeLegend({ data }: { data: ConsultationOutcomeSummary[] }) {
  const total = data.reduce((s, d) => s + d.count, 0);
  return (
    <ul className="mt-3 grid grid-cols-2 gap-2 text-xs">
      {data.map((o, i) => {
        const pct = total > 0 ? Math.round((o.count / total) * 100) : 0;
        return (
          <li key={o.status} className="flex items-center gap-2">
            <span
              className="size-2.5 shrink-0 rounded-full"
              style={{ background: PIE_COLORS[i % PIE_COLORS.length] }}
            />
            <span className="min-w-0 truncate capitalize text-muted-foreground">
              {o.status.replace(/_/g, " ").toLowerCase()}
            </span>
            <span className="num ml-auto font-semibold">{pct}%</span>
          </li>
        );
      })}
    </ul>
  );
}

/* ─── Main component ────────────────────────────────────────────────────────── */

function Analytics() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate({ to: "/login" });
      return;
    }
    if (!isDoctor()) {
      toast.error("Unauthorized: Doctor Analytics is restricted");
      navigate({ to: "/receptionist" });
      return;
    }
  }, [navigate]);

  const [selectedRange, setSelectedRange] = useState(1); // default 14 days

  const toDate = toISODate(new Date());
  const fromDate = toISODate(
    new Date(Date.now() - (DATE_RANGES[selectedRange]?.days ?? 13) * 86_400_000)
  );

  const summaryQuery = useDoctorAnalyticsSummary();
  const trendsQuery = useDoctorAnalyticsTrends(fromDate, toDate);

  const summary = summaryQuery.data;
  const trends: DailyTrendPoint[] = trendsQuery.data ?? [];

  // Build pie data from real outcomes
  const outcomeData =
    summary?.outcomesSummary && summary.outcomesSummary.length > 0
      ? summary.outcomesSummary.map((o) => ({
          name: o.status.replace(/_/g, " ").toLowerCase(),
          value: o.count,
        }))
      : [];

  const hasOutcomes = outcomeData.length > 0;

  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader title="Analytics" subtitle="Live practice performance from your clinical data" />

        {/* ── Summary Error ─────────────────────────────────────────────── */}
        {summaryQuery.isError && (
          <ErrorBanner message="Could not load summary metrics. Please try again." />
        )}

        {/* ── Top KPI Cards ─────────────────────────────────────────────── */}
        {summaryQuery.isLoading ? (
          <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="surface-card h-32 rounded-2xl animate-pulse" />
            ))}
          </div>
        ) : summary ? (
          <>
            {/* Row 1 — Today's activity */}
            <div>
              <p className="mb-3 text-[11px] font-bold uppercase tracking-widest text-muted-foreground">
                Today
              </p>
              <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
                <StatCard
                  index={0}
                  label="Today's Appointments"
                  value={summary.todayAppointmentsCount}
                  icon={CalendarCheck}
                  tone="primary"
                />
                <StatCard
                  index={1}
                  label="Today's Consultations"
                  value={summary.todayConsultationsCount}
                  icon={Stethoscope}
                  tone="info"
                />
                <StatCard
                  index={2}
                  label="Today's Patients"
                  value={summary.todayPatientsCount}
                  icon={Users}
                  tone="success"
                />
                <StatCard
                  index={3}
                  label="Queue Waiting"
                  value={summary.waitingQueueCount}
                  icon={Activity}
                  tone="warning"
                  hint={`${summary.inConsultationQueueCount} in consult`}
                />
              </div>
            </div>

            {/* Row 2 — All-time totals */}
            <div>
              <p className="mb-3 text-[11px] font-bold uppercase tracking-widest text-muted-foreground">
                All Time
              </p>
              <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
                <StatCard
                  index={0}
                  label="Completed Consultations"
                  value={summary.completedConsultationsCount}
                  icon={Stethoscope}
                  tone="success"
                />
                <StatCard
                  index={1}
                  label="Prescriptions Issued"
                  value={summary.prescriptionsIssuedCount}
                  icon={ClipboardList}
                  tone="violet"
                />
                <StatCard
                  index={2}
                  label="Lab Orders"
                  value={summary.labOrdersCount}
                  icon={FlaskConical}
                  tone="info"
                  hint={`${summary.pendingLabOrdersCount} pending`}
                />
                <StatCard
                  index={3}
                  label="Revenue Collected"
                  value={formatCurrency(summary.totalRevenueCollected)}
                  icon={Wallet}
                  tone="primary"
                  hint={`${formatCurrency(summary.totalRevenueBilled)} billed`}
                />
              </div>
            </div>

            {/* Queue Summary */}
            <div className="surface-card rounded-2xl p-4 sm:p-5">
              <h2 className="text-sm font-bold">Today's Queue Breakdown</h2>
              <div className="mt-4 flex flex-wrap justify-around gap-3">
                <QueueBadge
                  label="Waiting"
                  count={summary.waitingQueueCount}
                  color="var(--warning)"
                />
                <QueueBadge
                  label="In Consultation"
                  count={summary.inConsultationQueueCount}
                  color="var(--chart-1)"
                />
                <QueueBadge
                  label="Completed"
                  count={summary.completedQueueCount}
                  color="var(--success)"
                />
                <QueueBadge
                  label="No-shows"
                  count={summary.noShowAppointmentsCount}
                  color="var(--destructive)"
                />
                <QueueBadge
                  label="Cancelled"
                  count={summary.cancelledAppointmentsCount}
                  color="var(--muted-foreground)"
                />
              </div>
            </div>
          </>
        ) : null}

        {/* ── Date-range picker ──────────────────────────────────────────── */}
        <div className="flex items-center gap-2 flex-wrap">
          <TrendingUp className="size-4 text-muted-foreground" />
          <span className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
            Trend Period
          </span>
          {DATE_RANGES.map((r, i) => (
            <button
              key={r.label}
              onClick={() => setSelectedRange(i)}
              className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
                selectedRange === i
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted text-muted-foreground hover:bg-muted/80"
              }`}
            >
              {r.label}
            </button>
          ))}
          {trendsQuery.isLoading && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
        </div>

        {/* ── Trends Error ───────────────────────────────────────────────── */}
        {trendsQuery.isError && (
          <ErrorBanner message="Could not load trend data. Please try again." />
        )}

        {/* ── Trend Charts ──────────────────────────────────────────────── */}
        <div className="grid gap-5 xl:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)]">
          {/* Daily Appointments / Consultations */}
          <div className="surface-card rounded-2xl p-4 sm:p-5">
            <h2 className="text-sm font-bold">Daily Activity Trend</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              Appointments & consultations over the selected period
            </p>
            {trendsQuery.isLoading ? (
              <div className="mt-4 h-64 animate-pulse rounded-xl bg-muted" />
            ) : (
              <div className="mt-4 h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={trends}>
                    <defs>
                      <linearGradient id="fillAppts" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="var(--chart-1)" stopOpacity={0.35} />
                        <stop offset="100%" stopColor="var(--chart-1)" stopOpacity={0} />
                      </linearGradient>
                      <linearGradient id="fillConsults" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="var(--chart-2)" stopOpacity={0.35} />
                        <stop offset="100%" stopColor="var(--chart-2)" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke="var(--border)"
                      vertical={false}
                    />
                    <XAxis
                      dataKey="date"
                      tickLine={false}
                      axisLine={false}
                      fontSize={11}
                      tickFormatter={formatDateLabel}
                      interval="preserveStartEnd"
                    />
                    <YAxis tickLine={false} axisLine={false} fontSize={11} width={28} />
                    <Tooltip
                      contentStyle={{
                        borderRadius: 12,
                        border: "1px solid var(--border)",
                        background: "var(--popover)",
                        fontSize: 12,
                      }}
                      labelFormatter={(v) => formatDateLabel(String(v))}
                    />
                    <Legend
                      iconType="circle"
                      iconSize={8}
                      wrapperStyle={{ fontSize: 11, paddingTop: 8 }}
                    />
                    <Area
                      type="monotone"
                      dataKey="appointments"
                      name="Appointments"
                      stroke="var(--chart-1)"
                      strokeWidth={2.5}
                      fill="url(#fillAppts)"
                    />
                    <Area
                      type="monotone"
                      dataKey="consultations"
                      name="Consultations"
                      stroke="var(--chart-2)"
                      strokeWidth={2.5}
                      fill="url(#fillConsults)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>

          {/* Consultation Outcomes Pie */}
          <div className="surface-card rounded-2xl p-4 sm:p-5">
            <h2 className="text-sm font-bold">Consultation Outcomes</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              Breakdown by status across all consultations
            </p>
            {summaryQuery.isLoading ? (
              <div className="mt-4 h-64 animate-pulse rounded-xl bg-muted" />
            ) : hasOutcomes ? (
              <>
                <div className="mt-4 h-52">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={outcomeData}
                        dataKey="value"
                        nameKey="name"
                        innerRadius={52}
                        outerRadius={84}
                        paddingAngle={3}
                        stroke="none"
                      >
                        {outcomeData.map((_, i) => (
                          <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip
                        contentStyle={{
                          borderRadius: 12,
                          border: "1px solid var(--border)",
                          background: "var(--popover)",
                          fontSize: 12,
                        }}
                        formatter={(v: number, name: string) => [v, name]}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                {summary?.outcomesSummary && (
                  <OutcomeLegend data={summary.outcomesSummary} />
                )}
              </>
            ) : (
              <div className="mt-4 flex h-52 items-center justify-center text-sm text-muted-foreground">
                No consultation outcome data yet.
              </div>
            )}
          </div>
        </div>

        {/* ── Revenue Trend ─────────────────────────────────────────────── */}
        <div className="surface-card rounded-2xl p-4 sm:p-5">
          <h2 className="text-sm font-bold">Daily Revenue Collected</h2>
          <p className="mt-0.5 text-xs text-muted-foreground">
            Paid invoice amounts per day over the selected period
          </p>
          {trendsQuery.isLoading ? (
            <div className="mt-4 h-56 animate-pulse rounded-xl bg-muted" />
          ) : (
            <div className="mt-4 h-56">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={trends}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="var(--border)"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="date"
                    tickLine={false}
                    axisLine={false}
                    fontSize={11}
                    tickFormatter={formatDateLabel}
                    interval="preserveStartEnd"
                  />
                  <YAxis
                    tickLine={false}
                    axisLine={false}
                    fontSize={11}
                    width={48}
                    tickFormatter={(v: number) => formatCurrency(v)}
                  />
                  <Tooltip
                    cursor={{ fill: "var(--muted)" }}
                    contentStyle={{
                      borderRadius: 12,
                      border: "1px solid var(--border)",
                      background: "var(--popover)",
                      fontSize: 12,
                    }}
                    labelFormatter={(v) => formatDateLabel(String(v))}
                    formatter={(v: number) => [formatCurrency(v), "Revenue"]}
                  />
                  <Bar
                    dataKey="revenue"
                    name="Revenue"
                    fill="var(--chart-1)"
                    radius={[8, 8, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        {/* ── Patient Trend ─────────────────────────────────────────────── */}
        <div className="surface-card rounded-2xl p-4 sm:p-5">
          <h2 className="text-sm font-bold">Daily Unique Patients</h2>
          <p className="mt-0.5 text-xs text-muted-foreground">
            Count of distinct patients seen per day
          </p>
          {trendsQuery.isLoading ? (
            <div className="mt-4 h-48 animate-pulse rounded-xl bg-muted" />
          ) : (
            <div className="mt-4 h-48">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={trends}>
                  <defs>
                    <linearGradient id="fillPatients2" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="var(--chart-3)" stopOpacity={0.4} />
                      <stop offset="100%" stopColor="var(--chart-3)" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                  <XAxis
                    dataKey="date"
                    tickLine={false}
                    axisLine={false}
                    fontSize={11}
                    tickFormatter={formatDateLabel}
                    interval="preserveStartEnd"
                  />
                  <YAxis tickLine={false} axisLine={false} fontSize={11} width={28} />
                  <Tooltip
                    contentStyle={{
                      borderRadius: 12,
                      border: "1px solid var(--border)",
                      background: "var(--popover)",
                      fontSize: 12,
                    }}
                    labelFormatter={(v) => formatDateLabel(String(v))}
                    formatter={(v: number) => [v, "Unique Patients"]}
                  />
                  <Area
                    type="monotone"
                    dataKey="patients"
                    name="Unique Patients"
                    stroke="var(--chart-3)"
                    strokeWidth={2.5}
                    fill="url(#fillPatients2)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        {/* ── Lab & Prescription Summary ────────────────────────────────── */}
        {summary && (
          <div className="grid gap-5 sm:grid-cols-2">
            <div className="surface-card rounded-2xl p-4 sm:p-5">
              <h2 className="text-sm font-bold">Lab Orders Summary</h2>
              <div className="mt-4 space-y-3">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Total Orders</span>
                  <span className="num font-bold">{summary.labOrdersCount}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Completed</span>
                  <span className="num font-bold text-success">{summary.completedLabOrdersCount}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Pending / Processing</span>
                  <span className="num font-bold text-warning">{summary.pendingLabOrdersCount}</span>
                </div>
                {summary.labOrdersCount > 0 && (
                  <div className="mt-2">
                    <div className="mb-1 flex justify-between text-[11px] text-muted-foreground">
                      <span>Completion Rate</span>
                      <span>
                        {Math.round((summary.completedLabOrdersCount / summary.labOrdersCount) * 100)}%
                      </span>
                    </div>
                    <div className="h-2 rounded-full bg-muted overflow-hidden">
                      <div
                        className="h-full rounded-full bg-success transition-all duration-700"
                        style={{
                          width: `${Math.round(
                            (summary.completedLabOrdersCount / summary.labOrdersCount) * 100
                          )}%`,
                        }}
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="surface-card rounded-2xl p-4 sm:p-5">
              <h2 className="text-sm font-bold">Revenue Summary</h2>
              <div className="mt-4 space-y-3">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Total Billed</span>
                  <span className="num font-bold">{formatCurrency(summary.totalRevenueBilled)}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Total Collected</span>
                  <span className="num font-bold text-success">
                    {formatCurrency(summary.totalRevenueCollected)}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Outstanding</span>
                  <span className="num font-bold text-warning">
                    {formatCurrency(summary.totalRevenueBilled - summary.totalRevenueCollected)}
                  </span>
                </div>
                {summary.totalRevenueBilled > 0 && (
                  <div className="mt-2">
                    <div className="mb-1 flex justify-between text-[11px] text-muted-foreground">
                      <span>Collection Rate</span>
                      <span>
                        {Math.round(
                          (summary.totalRevenueCollected / summary.totalRevenueBilled) * 100
                        )}%
                      </span>
                    </div>
                    <div className="h-2 rounded-full bg-muted overflow-hidden">
                      <div
                        className="h-full rounded-full bg-success transition-all duration-700"
                        style={{
                          width: `${Math.round(
                            (summary.totalRevenueCollected / summary.totalRevenueBilled) * 100
                          )}%`,
                        }}
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </AppShell>
  );
}
