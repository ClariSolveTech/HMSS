import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { FlaskConical, PlusCircle, List, FileCheck, Search, Database } from "lucide-react";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { LabCatalogList } from "@/components/laboratory/lab-catalog-list";
import { LabOrderForm } from "@/components/laboratory/lab-order-form";
import { LabOrderList } from "@/components/laboratory/lab-order-list";
import { LabResultList } from "@/components/laboratory/lab-result-list";

export const Route = createFileRoute("/laboratory")({
  head: () => ({
    meta: [
      { title: "Laboratory — Meridian Health OS" },
      {
        name: "description",
        content: "Track ordered lab tests, sample status, turnaround time and abnormal-value alerts.",
      },
      { property: "og:title", content: "Laboratory — Meridian Health OS" },
      { property: "og:description", content: "Lab orders, sample tracking and abnormal-value alerts." },
    ],
  }),
  component: LaboratoryPage,
});

function LaboratoryPage() {
  const [activeTab, setActiveTab] = useState("orders");

  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader
          title="Laboratory Management"
          subtitle="Hospital lab test catalog, diagnostic orders, and verified lab results"
        />

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="w-full justify-start overflow-x-auto rounded-xl bg-muted/50 p-1">
            <TabsTrigger value="orders" className="rounded-lg text-xs font-semibold gap-2">
              <List className="size-3.5" /> Doctor Orders
            </TabsTrigger>
            <TabsTrigger value="create" className="rounded-lg text-xs font-semibold gap-2">
              <PlusCircle className="size-3.5 text-primary" /> Create Lab Order
            </TabsTrigger>
            <TabsTrigger value="results" className="rounded-lg text-xs font-semibold gap-2">
              <FileCheck className="size-3.5 text-success" /> Lab Results & Review
            </TabsTrigger>
            <TabsTrigger value="catalog" className="rounded-lg text-xs font-semibold gap-2">
              <Database className="size-3.5" /> Hospital Test Catalog
            </TabsTrigger>
          </TabsList>

          <TabsContent value="orders" className="space-y-4">
            <LabOrderList />
          </TabsContent>

          <TabsContent value="create" className="space-y-4">
            <LabOrderForm onSuccess={() => setActiveTab("orders")} />
          </TabsContent>

          <TabsContent value="results" className="space-y-4">
            <LabResultList />
          </TabsContent>

          <TabsContent value="catalog" className="space-y-4">
            <LabCatalogList />
          </TabsContent>
        </Tabs>
      </div>
    </AppShell>
  );
}
