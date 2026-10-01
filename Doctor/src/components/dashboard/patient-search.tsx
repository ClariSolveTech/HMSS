import { Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import { QrCode, Search, Phone, IdCard, Hash, ArrowRight } from "lucide-react";

import { patients, queue } from "@/lib/healthcare-data";
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

export function PatientSearch({ autoFocus = false }: { autoFocus?: boolean }) {
  const [q, setQ] = useState("");

  const results = useMemo(() => {
    const term = q.trim().toLowerCase();
    if (!term) return [];
    return patients.filter((p) => {
      const appt = queue.find((x) => x.patientId === p.id)?.appointmentNo ?? "";
      return [p.name, p.upid, p.mobile, appt, p.id].join(" ").toLowerCase().includes(term);
    });
  }, [q]);

  return (
    <div className="surface-card rounded-2xl p-4 sm:p-5">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          autoFocus={autoFocus}
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search by Universal Patient ID, mobile, name or appointment number…"
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
            {results.length === 0 && (
              <li className="rounded-xl bg-muted/60 px-4 py-6 text-center text-sm text-muted-foreground">
                No patient matches “{q}”.
              </li>
            )}
            {results.map((p) => (
              <li key={p.id}>
                <Link
                  to="/consultation/$patientId"
                  params={{ patientId: p.id }}
                  className="flex items-center gap-3 rounded-xl border bg-card px-3 py-2.5 transition-colors hover:border-primary/40 hover:bg-primary-soft"
                >
                  <img src={p.photo} alt="" className="size-10 shrink-0 rounded-lg border" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold">{p.name}</p>
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
            ))}
          </motion.ul>
        )}
      </AnimatePresence>
    </div>
  );
}
