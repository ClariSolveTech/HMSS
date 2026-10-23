import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";
import { toast } from "sonner";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { QueuePanel } from "@/components/dashboard/queue-panel";
import { isAuthenticated, isDoctor } from "@/lib/auth";

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
  component: DoctorQueuePage,
});

function DoctorQueuePage() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate({ to: "/login" });
      return;
    }
    if (!isDoctor()) {
      toast.error("Unauthorized: Doctor queue is restricted");
      navigate({ to: "/receptionist" });
      return;
    }
  }, [navigate]);

  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader
          title="Current Queue"
          subtitle="Tokens sync live with the reception desk and patient app"
        />
        <QueuePanel />
      </div>
    </AppShell>
  );
}
