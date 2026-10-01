import { useState } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { Bell, CheckCheck, Loader2, ExternalLink } from "lucide-react";
import { toast } from "sonner";

import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  useNotifications,
  useUnreadNotificationCount,
  useMarkNotificationAsRead,
  useMarkAllNotificationsAsRead,
  type NotificationResponse,
} from "@/services/notifications-api";
import {
  getNotificationIcon,
  getReferenceRoute,
  formatNotificationTime,
} from "./notification-utils";

export function NotificationDropdown() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  const { data: countData } = useUnreadNotificationCount();
  const unreadCount = countData?.unreadCount ?? 0;

  const { data: notificationsPage, isLoading, isError } = useNotifications(0, 10);
  const notifications: NotificationResponse[] = notificationsPage?.content || [];

  const markAsRead = useMarkNotificationAsRead();
  const markAllAsRead = useMarkAllNotificationsAsRead();

  const handleNotificationClick = async (n: NotificationResponse) => {
    if (n.status === "UNREAD") {
      try {
        await markAsRead.mutateAsync(n.id);
      } catch (err: any) {
        toast.error(err?.message || "Failed to mark notification as read");
      }
    }

    const route = getReferenceRoute(n.referenceType);
    if (route) {
      setOpen(false);
      navigate({ to: route as any });
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

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative rounded-xl" aria-label="Notifications">
          <Bell className="size-[18px]" />
          {unreadCount > 0 && (
            <span className="absolute right-1 top-1 grid min-w-4.5 h-4.5 place-items-center rounded-full bg-destructive px-1 text-[10px] font-bold leading-none text-destructive-foreground">
              {unreadCount > 99 ? "99+" : unreadCount}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[min(24rem,calc(100vw-2rem))] p-0 shadow-xl rounded-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between border-b px-4 py-3 bg-muted/30">
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-foreground">Notifications</h3>
            {unreadCount > 0 && (
              <span className="rounded-full bg-primary/15 px-2 py-0.5 text-[11px] font-semibold text-primary">
                {unreadCount} unread
              </span>
            )}
          </div>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="h-7 text-xs text-muted-foreground hover:text-foreground px-2 rounded-lg"
              onClick={handleMarkAllRead}
              disabled={markAllAsRead.isPending}
            >
              {markAllAsRead.isPending ? (
                <Loader2 className="size-3 animate-spin mr-1" />
              ) : (
                <CheckCheck className="size-3.5 mr-1" />
              )}
              Mark all read
            </Button>
          )}
        </div>

        {/* Content Area */}
        <ScrollArea className="max-h-84">
          {isLoading && (
            <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
              <Loader2 className="size-6 animate-spin mb-2 text-primary" />
              <p className="text-xs">Loading notifications...</p>
            </div>
          )}

          {isError && (
            <div className="py-6 px-4 text-center text-xs text-muted-foreground">
              Unable to load notifications. Please try again.
            </div>
          )}

          {!isLoading && !isError && notifications.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center px-4">
              <Bell className="size-8 text-muted-foreground/40 mb-2" />
              <p className="text-sm font-semibold text-foreground">No notifications</p>
              <p className="text-xs text-muted-foreground mt-0.5">You're all caught up!</p>
            </div>
          )}

          {!isLoading && !isError && notifications.length > 0 && (
            <ul className="divide-y divide-border/60">
              {notifications.map((n) => {
                const isUnread = n.status === "UNREAD";
                const route = getReferenceRoute(n.referenceType);

                return (
                  <li
                    key={n.id}
                    onClick={() => handleNotificationClick(n)}
                    className={cn(
                      "group relative flex gap-3 px-4 py-3 cursor-pointer transition-colors hover:bg-accent/60",
                      isUnread ? "bg-primary/5" : "bg-transparent"
                    )}
                  >
                    <div className="mt-0.5 grid size-8 shrink-0 place-items-center rounded-lg bg-background border shadow-xs">
                      {getNotificationIcon(n.type)}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between gap-1">
                        <p className={cn("text-xs truncate", isUnread ? "font-bold text-foreground" : "font-medium text-foreground/80")}>
                          {n.title}
                        </p>
                        <span className="text-[10px] text-muted-foreground shrink-0">
                          {formatNotificationTime(n.createdAt)}
                        </span>
                      </div>
                      <p className="text-xs text-muted-foreground line-clamp-2 mt-0.5">
                        {n.message}
                      </p>
                      {route && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-medium text-primary mt-1 group-hover:underline">
                          View details <ExternalLink className="size-2.5" />
                        </span>
                      )}
                    </div>
                    {isUnread && (
                      <span className="mt-1.5 size-2 shrink-0 rounded-full bg-primary" />
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </ScrollArea>

        {/* Footer */}
        <div className="border-t px-4 py-2.5 bg-muted/20 text-center">
          <Link
            to="/notifications"
            onClick={() => setOpen(false)}
            className="text-xs font-semibold text-primary hover:underline inline-flex items-center gap-1"
          >
            View all notifications →
          </Link>
        </div>
      </PopoverContent>
    </Popover>
  );
}
