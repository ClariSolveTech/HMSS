import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  Bell,
  CheckCheck,
  Check,
  Trash2,
  Loader2,
  AlertCircle,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  Filter,
} from "lucide-react";
import { toast } from "sonner";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cn } from "@/lib/utils";
import {
  useNotifications,
  useUnreadNotifications,
  useUnreadNotificationCount,
  useMarkNotificationAsRead,
  useMarkAllNotificationsAsRead,
  useDeleteNotification,
  type NotificationResponse,
} from "@/services/notifications-api";
import {
  getNotificationIcon,
  getPriorityBadgeClass,
  getReferenceRoute,
  formatNotificationTime,
} from "@/components/notifications/notification-utils";

export const Route = createFileRoute("/notifications")({
  head: () => ({
    meta: [
      { title: "Notifications — Meridian Health OS" },
      {
        name: "description",
        content: "Notification Center for medical alerts, lab results, appointment updates, and system messages.",
      },
    ],
  }),
  component: NotificationCenterPage,
});

function NotificationCenterPage() {
  const [filter, setFilter] = useState<"all" | "unread">("all");
  const [page, setPage] = useState(0);
  const pageSize = 15;

  const { data: countData } = useUnreadNotificationCount();
  const unreadCount = countData?.unreadCount ?? 0;

  const allQuery = useNotifications(page, pageSize);
  const unreadQuery = useUnreadNotifications(page, pageSize);

  const activeQuery = filter === "all" ? allQuery : unreadQuery;
  const { data, isLoading, isError, error, refetch } = activeQuery;

  const notifications: NotificationResponse[] = data?.content || [];
  const totalPages = data?.totalPages || 1;
  const totalElements = data?.totalElements || 0;

  const markAsRead = useMarkNotificationAsRead();
  const markAllAsRead = useMarkAllNotificationsAsRead();
  const deleteNotification = useDeleteNotification();

  const handleMarkAsRead = async (id: number) => {
    try {
      await markAsRead.mutateAsync(id);
      toast.success("Notification marked as read");
    } catch (err: any) {
      toast.error(err?.message || "Failed to mark notification as read");
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllAsRead.mutateAsync();
      toast.success("All notifications marked as read");
    } catch (err: any) {
      toast.error(err?.message || "Failed to mark all notifications as read");
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteNotification.mutateAsync(id);
      toast.success("Notification deleted");
    } catch (err: any) {
      toast.error(err?.message || "Failed to delete notification");
    }
  };

  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6 max-w-5xl mx-auto">
        <PageHeader
          title="Notification Center"
          subtitle={
            unreadCount > 0
              ? `You have ${unreadCount} unread notification${unreadCount === 1 ? "" : "s"}`
              : "All caught up! No unread notifications."
          }
          actions={
            <div className="flex items-center gap-2">
              {unreadCount > 0 && (
                <Button
                  variant="outline"
                  size="sm"
                  className="rounded-xl"
                  onClick={handleMarkAllRead}
                  disabled={markAllAsRead.isPending}
                >
                  {markAllAsRead.isPending ? (
                    <Loader2 className="size-3.5 animate-spin mr-1.5" />
                  ) : (
                    <CheckCheck className="size-3.5 mr-1.5" />
                  )}
                  Mark all as read
                </Button>
              )}
            </div>
          }
        />

        {/* Filters & Tabs */}
        <div className="flex flex-wrap items-center justify-between gap-4 border-b pb-4">
          <Tabs value={filter} onValueChange={(v) => { setFilter(v as any); setPage(0); }}>
            <TabsList className="rounded-xl bg-muted/60 p-1">
              <TabsTrigger value="all" className="rounded-lg text-xs font-semibold px-4">
                All ({filter === "all" ? totalElements : allQuery.data?.totalElements ?? 0})
              </TabsTrigger>
              <TabsTrigger value="unread" className="rounded-lg text-xs font-semibold px-4">
                Unread ({unreadCount})
              </TabsTrigger>
            </TabsList>
          </Tabs>

          <span className="text-xs text-muted-foreground flex items-center gap-1">
            <Filter className="size-3.5" />
            Showing page {page + 1} of {totalPages}
          </span>
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="surface-card rounded-2xl p-12 flex flex-col items-center justify-center min-h-[250px]">
            <Loader2 className="size-8 animate-spin text-primary mb-3" />
            <p className="text-sm font-semibold text-foreground">Fetching notifications...</p>
            <p className="text-xs text-muted-foreground mt-1">Connecting to server</p>
          </div>
        )}

        {/* Error State */}
        {isError && (
          <div className="surface-card flex flex-col items-center justify-center rounded-2xl p-8 text-center border-destructive/20 bg-destructive/5">
            <AlertCircle className="size-10 text-destructive mb-2" />
            <h3 className="text-base font-bold text-foreground">Unable to load notifications</h3>
            <p className="mt-1 text-xs text-muted-foreground max-w-sm">
              {(error as any)?.message || "Failed to communicate with notification service."}
            </p>
            <Button size="sm" className="mt-4 rounded-xl" onClick={() => refetch()}>
              Try Again
            </Button>
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !isError && notifications.length === 0 && (
          <div className="surface-card flex flex-col items-center justify-center rounded-2xl p-12 text-center">
            <Bell className="size-12 text-muted-foreground/30 mb-3" />
            <h3 className="text-base font-bold text-foreground">
              {filter === "unread" ? "No unread notifications" : "No notifications yet"}
            </h3>
            <p className="mt-1 text-xs text-muted-foreground max-w-xs">
              {filter === "unread"
                ? "You have read all your notifications."
                : "Updates regarding appointments, lab results, and prescriptions will appear here."}
            </p>
          </div>
        )}

        {/* Notifications List */}
        {!isLoading && !isError && notifications.length > 0 && (
          <div className="space-y-3">
            {notifications.map((n) => {
              const isUnread = n.status === "UNREAD";
              const route = getReferenceRoute(n.referenceType);

              return (
                <div
                  key={n.id}
                  className={cn(
                    "surface-card rounded-2xl p-4 sm:p-5 transition-all duration-200 border flex flex-col sm:flex-row sm:items-start gap-4",
                    isUnread
                      ? "border-primary/30 bg-primary/5 shadow-xs"
                      : "border-border/60 hover:border-border"
                  )}
                >
                  <div className="flex items-center gap-3 shrink-0">
                    <div className="grid size-10 place-items-center rounded-xl border bg-background shadow-xs">
                      {getNotificationIcon(n.type)}
                    </div>
                  </div>

                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h4 className={cn("text-sm", isUnread ? "font-bold text-foreground" : "font-semibold text-foreground/90")}>
                        {n.title}
                      </h4>
                      {isUnread && (
                        <Badge className="rounded-full bg-primary/20 text-primary border-primary/30 text-[10px] font-bold">
                          NEW
                        </Badge>
                      )}
                      {n.priority && n.priority !== "NORMAL" && (
                        <Badge className={cn("rounded-full border text-[10px] uppercase font-bold", getPriorityBadgeClass(n.priority))}>
                          {n.priority}
                        </Badge>
                      )}
                    </div>

                    <p className="text-xs text-muted-foreground leading-relaxed whitespace-pre-line">
                      {n.message}
                    </p>

                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 pt-1 text-[11px] text-muted-foreground/80">
                      <span>Created {formatNotificationTime(n.createdAt)}</span>
                      {n.readAt && (
                        <span>· Read {formatNotificationTime(n.readAt)}</span>
                      )}
                      {n.referenceType && (
                        <span>· Ref: {n.referenceType} #{n.referenceId}</span>
                      )}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 shrink-0 self-end sm:self-start pt-2 sm:pt-0 border-t sm:border-t-0 border-border/40 w-full sm:w-auto justify-end">
                    {route && (
                      <Link to={route as any} className="shrink-0">
                        <Button size="sm" variant="outline" className="h-8 rounded-xl text-xs gap-1">
                          View <ExternalLink className="size-3" />
                        </Button>
                      </Link>
                    )}

                    {isUnread && (
                      <Button
                        size="sm"
                        variant="ghost"
                        className="h-8 rounded-xl text-xs gap-1 text-primary hover:text-primary hover:bg-primary/10"
                        onClick={() => handleMarkAsRead(n.id)}
                        disabled={markAsRead.isPending}
                      >
                        <Check className="size-3.5" /> Mark read
                      </Button>
                    )}

                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-8 rounded-xl text-xs text-muted-foreground hover:text-destructive hover:bg-destructive/10 px-2"
                      onClick={() => handleDelete(n.id)}
                      disabled={deleteNotification.isPending}
                      title="Delete notification"
                    >
                      <Trash2 className="size-3.5" />
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Pagination Controls */}
        {!isLoading && !isError && totalPages > 1 && (
          <div className="flex items-center justify-between border-t pt-4">
            <span className="text-xs text-muted-foreground">
              Page {page + 1} of {totalPages} ({totalElements} items)
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                className="rounded-xl h-8 px-3 text-xs"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                <ChevronLeft className="size-3.5 mr-1" /> Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                className="rounded-xl h-8 px-3 text-xs"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
              >
                Next <ChevronRight className="size-3.5 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </AppShell>
  );
}
