"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import {
  Activity,
  LayoutDashboard,
  FileText,
  AlertTriangle,
  Bell,
  Settings,
  HeartPulse,
  GitBranch,
} from "lucide-react";

const navigation = [
  { name: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { name: "Health Monitor", href: "/dashboard/health", icon: HeartPulse },
  { name: "API Logs", href: "/dashboard/logs", icon: FileText },
  { name: "Incidents", href: "/dashboard/incidents", icon: AlertTriangle },
  { name: "Alerts", href: "/dashboard/alerts", icon: Bell },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <div className="fixed inset-y-0 left-0 w-64 bg-gray-900">
      <div className="flex items-center gap-2 px-6 py-4 border-b border-gray-800">
        <Activity className="w-8 h-8 text-primary-500" />
        <span className="text-xl font-bold text-white">InfoPulse</span>
      </div>

      <nav className="mt-6 px-3">
        {navigation.map((item) => {
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                "flex items-center gap-3 px-3 py-2 rounded-lg mb-1 transition",
                isActive
                  ? "bg-primary-600 text-white"
                  : "text-gray-400 hover:bg-gray-800 hover:text-white"
              )}
            >
              <item.icon className="w-5 h-5" />
              {item.name}
            </Link>
          );
        })}
      </nav>

      <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-gray-800">
        <div className="text-xs text-gray-500 text-center">
          InfoPulse v1.0.0
          <br />© 2024 Leap Finance
        </div>
      </div>
    </div>
  );
}
