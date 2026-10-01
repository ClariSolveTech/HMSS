import React from "react";
import { Pill, FileCheck, AlertCircle, Calendar, User, Clock } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { PrescriptionResponse } from "@/services/prescription-api";

interface PrescriptionViewProps {
  prescription: PrescriptionResponse;
}

export function PrescriptionView({ prescription }: PrescriptionViewProps) {
  const getBadgeVariant = (status: string) => {
    switch (status) {
      case "ISSUED":
        return "default";
      case "CANCELLED":
        return "destructive";
      default:
        return "secondary";
    }
  };

  return (
    <div className="surface-card rounded-2xl p-4 sm:p-5 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b pb-3">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold flex items-center gap-2">
              <Pill className="size-4 text-primary" /> {prescription.prescriptionCode}
            </h3>
            <Badge variant={getBadgeVariant(prescription.status)} className="rounded-full text-[10px]">
              {prescription.status}
            </Badge>
          </div>
          <p className="text-xs text-muted-foreground mt-0.5">
            Created: {new Date(prescription.createdAt).toLocaleString()}
            {prescription.issuedAt && ` · Issued: ${new Date(prescription.issuedAt).toLocaleString()}`}
          </p>
        </div>
      </div>

      {prescription.notes && (
        <div className="rounded-xl bg-muted/60 p-3 text-xs">
          <p className="font-semibold text-muted-foreground">Clinical Notes / Instructions:</p>
          <p className="mt-1 text-foreground">{prescription.notes}</p>
        </div>
      )}

      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b bg-muted/40 font-semibold text-muted-foreground">
              <th className="py-2 px-3">Medicine</th>
              <th className="py-2 px-3">Dosage</th>
              <th className="py-2 px-3">Frequency</th>
              <th className="py-2 px-3">Duration</th>
              <th className="py-2 px-3">Route</th>
              <th className="py-2 px-3">Instructions</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {prescription.items.map((item) => (
              <tr key={item.id || item.medicineName} className="hover:bg-muted/20">
                <td className="py-2.5 px-3 font-bold text-foreground">{item.medicineName}</td>
                <td className="py-2.5 px-3">{item.dosage}</td>
                <td className="py-2.5 px-3">{item.frequency}</td>
                <td className="py-2.5 px-3">{item.duration}</td>
                <td className="py-2.5 px-3">{item.route || "ORAL"}</td>
                <td className="py-2.5 px-3 text-muted-foreground">{item.instructions || "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
