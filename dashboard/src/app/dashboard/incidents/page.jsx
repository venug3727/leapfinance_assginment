"use client";

import { useState } from "react";
import useSWR from "swr";
import { incidentApi } from "@/lib/api";
import { formatDate, cn } from "@/lib/utils";
import { AlertTriangle, CheckCircle, Clock, XCircle } from "lucide-react";

const fetcher = async ([key, filters]) => {
  const res = await incidentApi.getIncidents(filters);
  return res.data.data;
};

export default function IncidentsPage() {
  const [filters, setFilters] = useState({
    status: "",
    incidentType: "",
    page: 0,
    size: 20,
  });

  const [resolving, setResolving] = useState(null);
  const [resolutionNotes, setResolutionNotes] = useState("");

  const { data, error, isLoading, mutate } = useSWR(
    ["incidents", filters],
    fetcher,
    { refreshInterval: 5000 } // Refresh every 5 seconds for real-time updates
  );

  const handleResolve = async (incident) => {
    try {
      await incidentApi.resolveIncident(
        incident.id,
        incident.version,
        resolutionNotes
      );
      setResolving(null);
      setResolutionNotes("");
      mutate();
    } catch (error) {
      if (error.response?.status === 409) {
        alert(
          "This incident was modified by another user. Please refresh and try again."
        );
        mutate();
      } else {
        alert("Failed to resolve incident");
      }
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case "OPEN":
        return <AlertTriangle className="w-4 h-4 text-yellow-500" />;
      case "ACKNOWLEDGED":
        return <Clock className="w-4 h-4 text-blue-500" />;
      case "RESOLVED":
        return <CheckCircle className="w-4 h-4 text-green-500" />;
      default:
        return null;
    }
  };

  const getTypeColor = (type) => {
    switch (type) {
      case "SLOW_API":
        return "bg-yellow-100 text-yellow-700";
      case "BROKEN_API":
        return "bg-red-100 text-red-700";
      case "RATE_LIMIT_HIT":
        return "bg-orange-100 text-orange-700";
      default:
        return "bg-gray-100 text-gray-700";
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Incidents</h1>
        <p className="text-gray-500 mt-1">Manage and resolve API incidents</p>
      </div>

      {/* Filters */}
      <div className="flex gap-4">
        <select
          value={filters.status}
          onChange={(e) =>
            setFilters({ ...filters, status: e.target.value, page: 0 })
          }
          className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
        >
          <option value="">All Status</option>
          <option value="OPEN">Open</option>
          <option value="ACKNOWLEDGED">Acknowledged</option>
          <option value="RESOLVED">Resolved</option>
        </select>

        <select
          value={filters.incidentType}
          onChange={(e) =>
            setFilters({ ...filters, incidentType: e.target.value, page: 0 })
          }
          className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
        >
          <option value="">All Types</option>
          <option value="SLOW_API">Slow API</option>
          <option value="BROKEN_API">Broken API</option>
          <option value="RATE_LIMIT_HIT">Rate Limit Hit</option>
        </select>
      </div>

      {/* Incidents List */}
      <div className="space-y-4">
        {isLoading ? (
          <div className="text-center py-8 text-gray-500">
            Loading incidents...
          </div>
        ) : error ? (
          <div className="text-center py-8 text-red-500">
            Failed to load incidents
          </div>
        ) : data?.content?.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            No incidents found
          </div>
        ) : (
          data?.content?.map((incident) => (
            <div
              key={incident.id}
              className="bg-white rounded-lg border border-gray-200 p-4"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    {getStatusIcon(incident.status)}
                    <span
                      className={cn(
                        "px-2 py-1 text-xs font-medium rounded",
                        getTypeColor(incident.incidentType)
                      )}
                    >
                      {incident.incidentType.replace("_", " ")}
                    </span>
                    <span className="text-sm text-gray-500">
                      {incident.occurrenceCount} occurrences
                    </span>
                  </div>

                  <h3 className="font-medium text-gray-900">
                    {incident.serviceName} - {incident.endpoint}
                  </h3>

                  <div className="mt-2 text-sm text-gray-500">
                    <span>Method: {incident.method}</span>
                    {incident.avgLatency && (
                      <span className="ml-4">
                        Avg Latency: {incident.avgLatency}ms
                      </span>
                    )}
                  </div>

                  <div className="mt-2 text-sm text-gray-500">
                    First seen: {formatDate(incident.firstSeenAt)} | Last seen:{" "}
                    {formatDate(incident.lastSeenAt)}
                  </div>

                  {incident.sampleErrorMessage && (
                    <div className="mt-2 p-2 bg-red-50 rounded text-sm text-red-700">
                      {incident.sampleErrorMessage}
                    </div>
                  )}

                  {incident.status === "RESOLVED" && (
                    <div className="mt-2 p-2 bg-green-50 rounded text-sm text-green-700">
                      Resolved by {incident.resolvedBy} on{" "}
                      {formatDate(incident.resolvedAt)}
                      {incident.resolutionNotes && (
                        <div className="mt-1">{incident.resolutionNotes}</div>
                      )}
                    </div>
                  )}
                </div>

                {incident.status !== "RESOLVED" && (
                  <div className="ml-4">
                    {resolving === incident.id ? (
                      <div className="w-64 space-y-2">
                        <textarea
                          value={resolutionNotes}
                          onChange={(e) => setResolutionNotes(e.target.value)}
                          placeholder="Resolution notes (optional)"
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                          rows={2}
                        />
                        <div className="flex gap-2">
                          <button
                            onClick={() => handleResolve(incident)}
                            className="px-3 py-1 bg-green-600 text-white text-sm rounded hover:bg-green-700"
                          >
                            Confirm
                          </button>
                          <button
                            onClick={() => {
                              setResolving(null);
                              setResolutionNotes("");
                            }}
                            className="px-3 py-1 bg-gray-200 text-gray-700 text-sm rounded hover:bg-gray-300"
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    ) : (
                      <button
                        onClick={() => setResolving(incident.id)}
                        className="px-4 py-2 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 flex items-center gap-2"
                      >
                        <CheckCircle className="w-4 h-4" />
                        Resolve
                      </button>
                    )}
                  </div>
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
            onClick={() => setFilters({ ...filters, page: filters.page - 1 })}
            disabled={data.isFirst}
            className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50"
          >
            Previous
          </button>
          <span className="px-3 py-1 text-sm text-gray-500">
            Page {data.page + 1} of {data.totalPages}
          </span>
          <button
            onClick={() => setFilters({ ...filters, page: filters.page + 1 })}
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
