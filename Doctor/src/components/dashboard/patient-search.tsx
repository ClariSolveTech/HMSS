import { Link } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion, AnimatePresence } from "motion/react";
import { QrCode, Search, Phone, IdCard, Hash, ArrowRight, Loader2 } from "lucide-react";

import { patients as mockPatients } from "@/lib/healthcare-data";
import { patientApi, type PatientResponse } from "@/services/patient-api";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

const modes = [
  { icon: IdCard, label: "Universal ID" },
  { icon: Phone, label: "Mobile" },
  { icon: Search, label: "Name" },
  { icon: Hash, label: "Appointment No." },
  { icon: QrCode, label: "QR Code" },
];

function formatBloodGroup(bg?: string): string {
  if (!bg) return "O+";
  return bg
    .replace("_POSITIVE", "+")
    .replace("_NEGATIVE", "-")
    .replace("POSITIVE", "+")
    .replace("NEGATIVE", "-");
}

function formatGender(gender?: string): string {
  if (!gender) return "Unknown";
  return gender.charAt(0).toUpperCase() + gender.slice(1).toLowerCase();
}

function computeAge(dob?: string): number {
  if (!dob) return 35;
  const birthDate = new Date(dob);
  if (isNaN(birthDate.getTime())) return 35;
  const age = new Date().getFullYear() - birthDate.getFullYear();
  return age > 0 ? age : 1;
}

export function PatientSearch({ autoFocus = false }: { autoFocus?: boolean }) {
  const [q, setQ] = useState("");
  const [debouncedQ, setDebouncedQ] = useState("");

  // Debounce user input to prevent duplicate requests
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQ(q.trim());
    }, 250);
    return () => clearTimeout(handler);
  }, [q]);

  // Query real persisted patients from PostgreSQL backend
  const { data: apiData, isLoading } = useQuery({
    queryKey: ["doctor-patient-search", debouncedQ],
    queryFn: () => patientApi.getPatients({ search: debouncedQ, size: 8 }),
    enabled: debouncedQ.length > 0,
  });

  const apiPatients = apiData?.content || [];

  // Map and combine results: prioritize real backend patients, fall back to mock patients if not found
  const results = useMemo(() => {
    if (!debouncedQ) return [];

    if (apiPatients.length > 0) {
      return apiPatients.map((p) => ({
        id: String(p.id),
        name: `${p.firstName} ${p.lastName}`.trim(),
        upid: p.patientCode || `PAT-${p.id}`,
        mobile: p.mobileNumber || "—",
        age: computeAge(p.dateOfBirth),
        gender: formatGender(p.gender),
        bloodGroup: formatBloodGroup(p.bloodGroup),
        photo: `https://api.dicebear.com/9.x/notionists/svg?seed=${encodeURIComponent(
          p.firstName || "Patient"
        )}&backgroundColor=dbeafe,e0f2fe,ede9fe`,
        isReal: true,
      }));
    }

    // Fallback search over demo seed patients if API returned zero matches
    const term = debouncedQ.toLowerCase();
    return mockPatients
      .filter((p) =>
        [p.name, p.upid, p.mobile, p.id].join(" ").toLowerCase().includes(term)
      )
      .slice(0, 6)
      .map((p) => ({
        id: p.id,
        name: p.name,
        upid: p.upid,
        mobile: p.mobile,
        age: p.age,
        gender: p.gender,
        bloodGroup: p.bloodGroup,
        photo: p.photo,
        isReal: false,
      }));
  }, [debouncedQ, apiPatients]);

  return (
    <div className="surface-card rounded-2xl p-4 sm:p-5">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          autoFocus={autoFocus}
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search by Universal Patient ID, mobile, name or patient code…"
          className="h-12 rounded-xl pl-10 pr-12 text-sm"
        />
        <Button
          variant="ghost"
          size="icon"
          className="absolute right-1.5 top-1/2 -translate-y-1/2 rounded-lg text-primary"
        >
          <QrCode className="size-[18px]" />
          <span className="sr-only">Scan QR code</span>
        </Button>
      </div>

      <div className="mt-3 flex flex-wrap gap-1.5">
        {modes.map((m) => (
          <Badge
            key={m.label}
            variant="secondary"
            className="rounded-full text-[11px] font-medium text-muted-foreground"
          >
            <m.icon className="size-3" />
            {m.label}
          </Badge>
        ))}
      </div>

      <AnimatePresence>
        {q.trim() && (
          <motion.ul
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            className="mt-4 space-y-1.5 overflow-hidden"
          >
            {isLoading ? (
              <li className="flex items-center justify-center gap-2 rounded-xl bg-muted/40 px-4 py-5 text-xs text-muted-foreground">
                <Loader2 className="size-4 animate-spin text-primary" />
                Searching hospital records...
              </li>
            ) : results.length === 0 ? (
              <li className="rounded-xl bg-muted/60 px-4 py-6 text-center text-sm text-muted-foreground">
                No patient matches “{q}”.
              </li>
            ) : (
              results.map((p) => (
                <li key={p.id}>
                  <Link
                    to="/consultation/$patientId"
                    params={{ patientId: p.id }}
                    className="flex items-center gap-3 rounded-xl border bg-card px-3 py-2.5 transition-colors hover:border-primary/40 hover:bg-primary-soft"
                  >
                    <img
                      src={p.photo}
                      alt={p.name}
                      className="size-10 shrink-0 rounded-lg border object-cover"
                    />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <p className="truncate text-sm font-semibold">{p.name}</p>
                        {p.isReal && (
                          <Badge
                            variant="outline"
                            className="text-[9px] rounded-full border-emerald-500/30 bg-emerald-500/10 text-emerald-600 px-1.5 py-0"
                          >
                            Verified
                          </Badge>
                        )}
                      </div>
                      <p className="num truncate text-xs text-muted-foreground">
                        {p.upid} · {p.mobile}
                      </p>
                    </div>
                    <span className="hidden shrink-0 text-xs text-muted-foreground sm:block">
                      {p.age}y · {p.gender} · {p.bloodGroup}
                    </span>
                    <ArrowRight className="size-4 shrink-0 text-muted-foreground" />
                  </Link>
                </li>
              ))
            )}
          </motion.ul>
        )}
      </AnimatePresence>
    </div>
  );
}
