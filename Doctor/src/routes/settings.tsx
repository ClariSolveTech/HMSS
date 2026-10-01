import { createFileRoute } from "@tanstack/react-router";
import { Bell, Building2, Mic, Shield, Sparkles, UserCog } from "lucide-react";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { doctor } from "@/lib/healthcare-data";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Separator } from "@/components/ui/separator";

export const Route = createFileRoute("/settings")({
  head: () => ({
    meta: [
      { title: "Settings — Meridian Health OS" },
      {
        name: "description",
        content: "Manage clinician profile, notification rules, AI copilot behaviour and voice dictation.",
      },
      { property: "og:title", content: "Settings — Meridian Health OS" },
      { property: "og:description", content: "Clinician profile, notifications and AI preferences." },
    ],
  }),
  component: SettingsPage,
});

const toggles = [
  { icon: Bell, label: "Emergency patient alerts", desc: "Push + sound for triaged emergencies", on: true },
  { icon: Bell, label: "Lab report ready", desc: "Notify when investigations are reported", on: true },
  { icon: Mic, label: "Voice dictation", desc: "Transcribe clinical notes into the EMR", on: true },
  { icon: Sparkles, label: "AI copilot suggestions", desc: "Summaries and risk alerts (never auto-prescribes)", on: true },
  { icon: Shield, label: "Two-factor authentication", desc: "Required for prescription e-signing", on: true },
];

function SettingsPage() {
  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader title="Settings" subtitle="Profile, notifications and AI preferences" />

        <div className="grid gap-5 lg:grid-cols-2">
          <div className="surface-card rounded-2xl p-5">
            <h2 className="flex items-center gap-2 text-sm font-bold">
              <UserCog className="size-4 text-primary" /> Clinician profile
            </h2>
            <div className="mt-4 flex items-center gap-3">
              <img src={doctor.photo} alt="" className="size-14 rounded-2xl border" />
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold">{doctor.name}</p>
                <p className="truncate text-xs text-muted-foreground">{doctor.specialty}</p>
              </div>
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label className="text-xs">Registration number</Label>
                <Input defaultValue={doctor.regNo} className="num rounded-xl" />
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs">Consulting room</Label>
                <Input defaultValue="Room 4, Block B" className="rounded-xl" />
              </div>
              <div className="space-y-1.5 sm:col-span-2">
                <Label className="text-xs">Default consultation duration</Label>
                <Input defaultValue="12 minutes" className="rounded-xl" />
              </div>
            </div>
            <Separator className="my-4" />
            <p className="flex items-center gap-2 text-xs text-muted-foreground">
              <Building2 className="size-3.5" /> {doctor.hospital}
            </p>
          </div>

          <div className="surface-card rounded-2xl p-5">
            <h2 className="text-sm font-bold">Preferences</h2>
            <ul className="mt-4 divide-y">
              {toggles.map((t) => (
                <li key={t.label} className="flex items-center gap-3 py-3">
                  <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary">
                    <t.icon className="size-4" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{t.label}</p>
                    <p className="truncate text-xs text-muted-foreground">{t.desc}</p>
                  </div>
                  <Switch defaultChecked={t.on} />
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
