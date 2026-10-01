import { useState } from "react";
import { FlaskConical, Search, AlertCircle, Loader2 } from "lucide-react";

import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useLabTests } from "@/services/lab-api";

export function LabCatalogList() {
  const [searchTerm, setSearchTerm] = useState("");
  const { data: labTests, isLoading, isError, error, refetch } = useLabTests();

  if (isLoading) {
    return (
      <div className="surface-card flex flex-col items-center justify-center rounded-2xl py-12 text-muted-foreground">
        <Loader2 className="size-6 animate-spin text-primary mb-2" />
        <p className="text-xs font-semibold">Loading hospital laboratory catalog...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="surface-card flex flex-col items-center justify-center rounded-2xl p-6 text-center text-destructive">
        <AlertCircle className="size-6 mb-2" />
        <p className="text-xs font-bold">Unable to load laboratory catalog.</p>
        <p className="text-[11px] text-muted-foreground mt-1">{(error as any)?.message || "Server request failed."}</p>
        <Button size="sm" variant="outline" onClick={() => refetch()} className="mt-3 rounded-xl text-xs">
          Retry
        </Button>
      </div>
    );
  }

  const tests = labTests || [];
  const filtered = tests.filter(
    (t) =>
      t.testName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      t.testCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (t.category && t.category.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
          <Input
            placeholder="Search test name, code or category..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9 h-10 rounded-xl text-xs"
          />
        </div>
        <Badge variant="outline" className="rounded-xl px-3 py-2 text-xs shrink-0">
          {filtered.length} Tests Active
        </Badge>
      </div>

      {filtered.length === 0 ? (
        <div className="surface-card flex flex-col items-center justify-center rounded-2xl py-10 p-6 text-center">
          <p className="text-xs font-bold text-foreground">No laboratory tests found matching search</p>
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((test) => (
            <div key={test.id} className="surface-card rounded-2xl p-4 space-y-2.5">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <h4 className="font-bold text-sm text-foreground">{test.testName}</h4>
                  <Badge variant="outline" className="rounded-md text-[10px] mt-1">
                    {test.testCode}
                  </Badge>
                </div>
                <Badge className="bg-success/15 text-success border-0 rounded-full text-[10px]">
                  ACTIVE
                </Badge>
              </div>

              {test.description && (
                <p className="text-xs text-muted-foreground line-clamp-2">{test.description}</p>
              )}

              <div className="grid grid-cols-2 gap-2 text-[11px] pt-2 border-t text-muted-foreground">
                <div>
                  <span className="font-semibold text-foreground">Category: </span>
                  {test.category || "General"}
                </div>
                <div>
                  <span className="font-semibold text-foreground">Sample: </span>
                  {test.sampleType || "N/A"}
                </div>
                {test.unit && (
                  <div>
                    <span className="font-semibold text-foreground">Unit: </span>
                    {test.unit}
                  </div>
                )}
                {test.referenceRange && (
                  <div>
                    <span className="font-semibold text-foreground">Reference: </span>
                    {test.referenceRange}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
