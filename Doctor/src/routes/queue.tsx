import { createFileRoute } from "@tanstack/react-router";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { QueuePanel } from "@/components/dashboard/queue-panel";

export const Route = createFileRoute("/queue")({
  head: () => ({
    meta: [
      { title: "Live Patient Queue — Meridian Health OS" },
      {
        name: "description",
        content:
          "Real-time OPD queue with tokens, priorities, waiting times, emergency override and one-tap consultation start.",
      },
      { property: "og:title", content: "Live Patient Queue — Meridian Health OS" },
      {
        property: "og:description",
        content: "Manage tokens, priorities and waiting times with live reception sync.",
      },
    ],
  }),
  component: () => (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader
          title="Current Queue"
          subtitle="Tokens sync live with the reception desk and patient app"
        />
        <QueuePanel />
      </div>
    </AppShell>
  ),
});
