"use client";

import { alertApi } from "@/lib/api";
import { formatDate, cn } from "@/lib/utils";
import { Bell, AlertTriangle, XCircle, Gauge, Check } from "lucide-react";
import Link from "next/link";

export default function RecentAlerts({ alerts, onAcknowledge }) {
  const handleAcknowledge = async (id) => {
    try {
      await alertApi.acknowledgeAlert(id);
      onAcknowledge();
    } catch (error) {
      console.error("Failed to acknowledge alert:", error);
    }
  };

  const getAlertIcon = (type) => {
    switch (type) {
      case "SLOW_API":
        return <AlertTriangle className="w-4 h-4 text-yellow-500" />;
      case "ERROR_SPIKE":
        return <XCircle className="w-4 h-4 text-red-500" />;
      case "RATE_LIMIT_EXCEEDED":
        return <Gauge className="w-4 h-4 text-orange-500" />;
      default:
        return <Bell className="w-4 h-4 text-gray-500" />;
    }
  };

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900">Recent Alerts</h3>
        <Link
          href="/dashboard/alerts"
          className="text-sm text-primary-600 hover:text-primary-700"
        >
          View all
        </Link>
      </div>

      {alerts.length === 0 ? (
        <p className="text-gray-500 text-center py-8">
          No unacknowledged alerts
        </p>
      ) : (
        <div className="space-y-3">
          {alerts.map((alert) => (
            <div
              key={alert.id}
              className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg"
            >
              {getAlertIcon(alert.alertType)}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900">
                  {alert.alertType.replace("_", " ")}
                </p>
                <p className="text-xs text-gray-500 truncate">
                  {alert.message}
                </p>
                <p className="text-xs text-gray-400 mt-1">
                  {formatDate(alert.timestamp)}
                </p>
              </div>
              <button
                onClick={() => handleAcknowledge(alert.id)}
                className="flex-shrink-0 p-1 text-gray-400 hover:text-green-600 hover:bg-green-50 rounded transition"
                title="Acknowledge"
              >
                <Check className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
