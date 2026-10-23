import { createFileRoute, Outlet } from "@tanstack/react-router";

export const Route = createFileRoute("/patient")({
  component: PatientRootLayout,
});

function PatientRootLayout() {
  return <Outlet />;
}
