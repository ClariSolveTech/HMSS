import { createFileRoute, Outlet, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";
import { AppShell } from "@/components/layout/app-shell";
import { getAuthUser, isAuthenticated, hasRole } from "@/lib/auth";

export const Route = createFileRoute("/receptionist")({
  head: () => ({
    meta: [
      { title: "Receptionist Dashboard — Meridian Health OS" },
      { name: "description", content: "Front desk reception, live queue token dispatch, patient registration, and appointment desk." },
    ],
  }),
  component: ReceptionistLayout,
});

function ReceptionistLayout() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate({ to: "/login" });
      return;
    }
    const hasRecepRole =
      hasRole("ROLE_RECEPTIONIST") ||
      hasRole("ROLE_HOSPITAL_ADMIN") ||
      hasRole("ROLE_PLATFORM_ADMIN");
    if (!hasRecepRole) {
      navigate({ to: "/" });
    }
  }, [navigate]);

  return (
    <AppShell>
      <Outlet />
    </AppShell>
  );
}
