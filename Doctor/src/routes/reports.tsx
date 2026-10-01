import { createFileRoute } from "@tanstack/react-router";
import { Download, FileBarChart, FileBadge, Share2 } from "lucide-react";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/reports")({
  head: () => ({
    meta: [
      { title: "Reports — Meridian Health OS" },
      {
        name: "description",
        content: "Generate clinical, discharge, certificate and department reports in one click.",
      },
      { property: "og:title", content: "Reports — Meridian Health OS" },
      { property: "og:description", content: "Clinical, discharge and certificate report generation." },
    ],
  }),
  component: Reports,
});

const reports = [
  { title: "Daily OPD Summary", desc: "Consultations, diagnoses and revenue for the day", icon: FileBarChart },
  { title: "Medical Certificate", desc: "Fitness / sick-leave certificate with e-signature", icon: FileBadge },
  { title: "Discharge Summary", desc: "Structured summary for admitted patients", icon: FileBarChart },
  { title: "Chronic Care Register", desc: "Diabetes and hypertension cohort tracking", icon: FileBarChart },
  { title: "Referral Letter", desc: "Specialist referral with clinical context", icon: FileBadge },
  { title: "Lab Utilisation Report", desc: "Ordered vs. completed investigations", icon: FileBarChart },
];

function Reports() {
  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader title="Reports" subtitle="Generate and share clinical documents instantly" />
        <div className="grid gap-3 md:grid-cols-2 2xl:grid-cols-3">
          {reports.map((r) => (
            <div key={r.title} className="surface-card rounded-2xl p-5">
              <span className="grid size-10 place-items-center rounded-xl bg-primary/10 text-primary">
                <r.icon className="size-5" />
              </span>
              <p className="mt-3 text-sm font-bold">{r.title}</p>
              <p className="mt-1 text-xs text-muted-foreground">{r.desc}</p>
              <div className="mt-4 flex gap-2">
                <Button size="sm" className="rounded-xl">
                  <Download className="size-3.5" /> Generate
                </Button>
                <Button size="sm" variant="outline" className="rounded-xl">
                  <Share2 className="size-3.5" /> Share
                </Button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </AppShell>
  );
}
