import { useState } from "react";
import { FlaskConical, Clock, CheckCircle2, AlertTriangle, FileText, Loader2, Eye } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { usePatientLabOrders, usePatientLabResults, LabOrderResponse, LabResultResponse } from "@/services/lab-api";
import { LabOrderDetailsModal } from "./lab-order-details-modal";
import { LabResultDetailsModal } from "./lab-result-details-modal";

interface PatientLabHistoryProps {
  patientId: number;
}

export function PatientLabHistory({ patientId }: PatientLabHistoryProps) {
  const [selectedOrder, setSelectedOrder] = useState<LabOrderResponse | null>(null);
  const [selectedResult, setSelectedResult] = useState<LabResultResponse | null>(null);

  const { data: ordersData, isLoading: loadingOrders } = usePatientLabOrders(patientId);
  const { data: resultsData, isLoading: loadingResults } = usePatientLabResults(patientId);

  const orders = ordersData?.content || [];
  const results = resultsData?.content || [];

  return (
    <div className="surface-card rounded-2xl p-4 sm:p-5 space-y-4">
      <div className="flex items-center gap-2 border-b pb-3">
        <span className="grid size-8 place-items-center rounded-xl bg-primary/10 text-primary">
          <FlaskConical className="size-4" />
        </span>
        <div>
          <h3 className="text-sm font-bold text-foreground">Patient Laboratory History</h3>
          <p className="text-[11px] text-muted-foreground">Previous lab orders and diagnostic test results</p>
        </div>
      </div>

      <Tabs defaultValue="orders">
        <TabsList className="w-full justify-start rounded-xl">
          <TabsTrigger value="orders" className="rounded-lg text-xs">
            Lab Orders ({orders.length})
          </TabsTrigger>
          <TabsTrigger value="results" className="rounded-lg text-xs">
            Test Results ({results.length})
          </TabsTrigger>
        </TabsList>

        {/* Patient Lab Orders */}
        <TabsContent value="orders" className="mt-3 space-y-3">
          {loadingOrders ? (
            <div className="flex items-center justify-center py-8 text-xs text-muted-foreground">
              <Loader2 className="mr-2 size-4 animate-spin text-primary" /> Loading patient lab orders...
            </div>
          ) : orders.length === 0 ? (
            <div className="rounded-xl border border-dashed py-8 text-center text-xs text-muted-foreground">
              No previous lab orders found for this patient.
            </div>
          ) : (
            <div className="grid gap-2.5 sm:grid-cols-2">
              {orders.map((o) => (
                <div
                  key={o.id}
                  onClick={() => setSelectedOrder(o)}
                  className="group cursor-pointer rounded-xl border p-3 transition-all hover:border-primary/40 bg-background"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-bold text-xs text-foreground group-hover:text-primary">{o.orderNumber}</p>
                      <p className="text-[11px] text-muted-foreground mt-0.5">
                        {new Date(o.orderedAt || o.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    <Badge variant="outline" className="rounded-full text-[10px]">
                      {o.status}
                    </Badge>
                  </div>
                  <div className="mt-2 text-[11px] text-muted-foreground border-t pt-2 flex items-center justify-between">
                    <span>{o.items?.length || 0} requested test{(o.items?.length || 0) === 1 ? "" : "s"}</span>
                    <span className="text-primary font-semibold flex items-center gap-1">
                      View <Eye className="size-3" />
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </TabsContent>

        {/* Patient Lab Results */}
        <TabsContent value="results" className="mt-3 space-y-3">
          {loadingResults ? (
            <div className="flex items-center justify-center py-8 text-xs text-muted-foreground">
              <Loader2 className="mr-2 size-4 animate-spin text-primary" /> Loading patient lab results...
            </div>
          ) : results.length === 0 ? (
            <div className="rounded-xl border border-dashed py-8 text-center text-xs text-muted-foreground">
              No completed lab results found for this patient.
            </div>
          ) : (
            <div className="grid gap-2.5 sm:grid-cols-2">
              {results.map((r) => (
                <div
                  key={r.id}
                  onClick={() => setSelectedResult(r)}
                  className={`group cursor-pointer rounded-xl border p-3 transition-all hover:border-primary/40 bg-background ${
                    r.abnormalFlag ? "border-destructive/30 bg-destructive/5" : ""
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="font-bold text-xs text-foreground">{r.testNameSnapshot}</span>
                        {r.abnormalFlag && (
                          <Badge variant="destructive" className="rounded-full text-[9px] px-1.5">
                            ABNORMAL
                          </Badge>
                        )}
                      </div>
                      <p className="text-[11px] text-muted-foreground mt-0.5">
                        {r.resultedAt ? new Date(r.resultedAt).toLocaleDateString() : "Date N/A"}
                      </p>
                    </div>
                    <Badge variant="secondary" className="rounded-full text-[10px]">
                      {r.resultStatus}
                    </Badge>
                  </div>
                  <div className="mt-2 flex items-baseline justify-between border-t pt-2">
                    <span className={`text-sm font-bold ${r.abnormalFlag ? "text-destructive" : "text-foreground"}`}>
                      {r.resultValue} {r.resultUnit || ""}
                    </span>
                    <span className="text-primary text-[11px] font-semibold flex items-center gap-1">
                      Details <Eye className="size-3" />
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>

      {/* Order Details Modal */}
      <LabOrderDetailsModal
        order={selectedOrder}
        open={Boolean(selectedOrder)}
        onClose={() => setSelectedOrder(null)}
      />

      {/* Result Details Modal */}
      <LabResultDetailsModal
        result={selectedResult}
        open={Boolean(selectedResult)}
        onClose={() => setSelectedResult(null)}
      />
    </div>
  );
}
