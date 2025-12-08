"use client";

import { useState } from "react";
import useSWR from "swr";
import { alertApi } from "@/lib/api";
import { formatDate, cn } from "@/lib/utils";
import { Bell, Check, AlertTriangle, XCircle, Gauge } from "lucide-react";

const fetcher = async ([key, page, size]) => {
  const res = await alertApi.getAlerts(page, size);
  return res.data.data;
};

export default function AlertsPage() {
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState("all"); // all, unacknowledged

  const { data, error, isLoading, mutate } = useSWR(
    ["alerts", page, 20],
    fetcher,
    { refreshInterval: 5000 } // Refresh every 5 seconds for real-time updates
  );

  const handleAcknowledge = async (id) => {
    try {
      await alertApi.acknowledgeAlert(id);
      mutate();
    } catch (error) {
      alert("Failed to acknowledge alert");
    }
  };

  const getAlertIcon = (type) => {
    switch (type) {
      case "SLOW_API":
        return <AlertTriangle className="w-5 h-5 text-yellow-500" />;
      case "ERROR_SPIKE":
        return <XCircle className="w-5 h-5 text-red-500" />;
      case "RATE_LIMIT_EXCEEDED":
        return <Gauge className="w-5 h-5 text-orange-500" />;
      default:
        return <Bell className="w-5 h-5 text-gray-500" />;
    }
  };

  const getAlertColor = (type) => {
    switch (type) {
      case "SLOW_API":
        return "border-l-yellow-500 bg-yellow-50";
      case "ERROR_SPIKE":
        return "border-l-red-500 bg-red-50";
      case "RATE_LIMIT_EXCEEDED":
        return "border-l-orange-500 bg-orange-50";
      default:
        return "border-l-gray-500 bg-gray-50";
    }
  };

  const filteredAlerts = data?.content?.filter((alert) => {
    if (filter === "unacknowledged") return !alert.acknowledged;
    return true;
  });

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Alerts</h1>
          <p className="text-gray-500 mt-1">
            View and acknowledge system alerts
          </p>
        </div>

        <div className="flex gap-2">
          <button
            onClick={() => setFilter("all")}
            className={cn(
              "px-4 py-2 rounded-lg text-sm font-medium transition",
              filter === "all"
                ? "bg-primary-100 text-primary-700"
                : "bg-white border border-gray-300 text-gray-700 hover:bg-gray-50"
            )}
          >
            All
          </button>
          <button
            onClick={() => setFilter("unacknowledged")}
            className={cn(
              "px-4 py-2 rounded-lg text-sm font-medium transition",
              filter === "unacknowledged"
                ? "bg-primary-100 text-primary-700"
                : "bg-white border border-gray-300 text-gray-700 hover:bg-gray-50"
            )}
          >
            Unacknowledged
          </button>
        </div>
      </div>

      {/* Alerts List */}
      <div className="space-y-3">
        {isLoading ? (
          <div className="text-center py-8 text-gray-500">
            Loading alerts...
          </div>
        ) : error ? (
          <div className="text-center py-8 text-red-500">
            Failed to load alerts
          </div>
        ) : filteredAlerts?.length === 0 ? (
          <div className="text-center py-8 text-gray-500">No alerts found</div>
        ) : (
          filteredAlerts?.map((alert) => (
            <div
              key={alert.id}
              className={cn(
                "rounded-lg border-l-4 p-4 transition",
                getAlertColor(alert.alertType),
                alert.acknowledged && "opacity-60"
              )}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  {getAlertIcon(alert.alertType)}
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-gray-900">
                        {alert.alertType.replace("_", " ")}
                      </span>
                      <span className="text-sm text-gray-500">
                        {formatDate(alert.timestamp)}
                      </span>
                      {alert.acknowledged && (
                        <span className="px-2 py-0.5 bg-green-100 text-green-700 text-xs rounded">
                          Acknowledged
                        </span>
                      )}
                    </div>
                    <p className="text-gray-700 mt-1">{alert.message}</p>
                    <div className="text-sm text-gray-500 mt-1">
                      {alert.serviceName} - {alert.method} {alert.endpoint}
                    </div>
                    {alert.acknowledged && (
                      <div className="text-sm text-gray-500 mt-1">
                        Acknowledged by {alert.acknowledgedBy} on{" "}
                        {formatDate(alert.acknowledgedAt)}
                      </div>
                    )}
                  </div>
                </div>

                {!alert.acknowledged && (
                  <button
                    onClick={() => handleAcknowledge(alert.id)}
                    className="px-3 py-1 bg-white border border-gray-300 rounded text-sm hover:bg-gray-50 flex items-center gap-1"
                  >
                    <Check className="w-4 h-4" />
                    Acknowledge
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={data.isFirst}
            className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50"
          >
            Previous
          </button>
          <span className="px-3 py-1 text-sm text-gray-500">
            Page {data.page + 1} of {data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => p + 1)}
            disabled={data.isLast}
            className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
