"use client";

import { useState } from "react";
import useSWR from "swr";
import { dashboardApi } from "@/lib/api";
import {
  formatDate,
  formatLatency,
  getStatusColor,
  getMethodColor,
  cn,
} from "@/lib/utils";
import { Search, Filter, RefreshCw } from "lucide-react";

const fetcher = async ([key, filters]) => {
  // Remove empty string values from filters before sending
  const cleanFilters = Object.fromEntries(
    Object.entries(filters).filter(
      ([_, value]) => value !== "" && value !== null && value !== undefined
    )
  );
  const res = await dashboardApi.getLogs(cleanFilters);
  return res.data.data;
};

const servicesFetcher = async () => {
  const res = await dashboardApi.getServices();
  return res.data.data;
};

export default function LogsPage() {
  const [filters, setFilters] = useState({
    serviceName: "",
    endpoint: "",
    method: "",
    statusCode: "",
    isSlow: "",
    isBroken: "",
    page: 0,
    size: 20,
  });

  const [showFilters, setShowFilters] = useState(false);

  const { data: services } = useSWR("services", servicesFetcher);
  const { data, error, isLoading, mutate } = useSWR(
    ["logs", filters],
    fetcher,
    { refreshInterval: 5000 } // Refresh every 5 seconds for real-time updates
  );

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value,
      page: 0, // Reset page on filter change
    }));
  };

  const clearFilters = () => {
    setFilters({
      serviceName: "",
      endpoint: "",
      method: "",
      statusCode: "",
      isSlow: "",
      isBroken: "",
      page: 0,
      size: 20,
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">API Logs</h1>
          <p className="text-gray-500 mt-1">
            View and filter all API request logs
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={cn(
              "flex items-center gap-2 px-4 py-2 rounded-lg border transition",
              showFilters
                ? "bg-primary-50 border-primary-300 text-primary-700"
                : "bg-white border-gray-300 text-gray-700 hover:bg-gray-50"
            )}
          >
            <Filter className="w-4 h-4" />
            Filters
          </button>
          <button
            onClick={() => mutate()}
            className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
          >
            <RefreshCw className="w-4 h-4" />
            Refresh
          </button>
        </div>
      </div>

      {/* Filters Panel */}
      {showFilters && (
        <div className="bg-white rounded-lg border border-gray-200 p-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Service
              </label>
              <select
                value={filters.serviceName}
                onChange={(e) =>
                  handleFilterChange("serviceName", e.target.value)
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
              >
                <option value="">All Services</option>
                {services?.map((service) => (
                  <option key={service} value={service}>
                    {service}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Endpoint
              </label>
              <input
                type="text"
                value={filters.endpoint}
                onChange={(e) => handleFilterChange("endpoint", e.target.value)}
                placeholder="Search endpoint..."
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Method
              </label>
              <select
                value={filters.method}
                onChange={(e) => handleFilterChange("method", e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
              >
                <option value="">All Methods</option>
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="PATCH">PATCH</option>
                <option value="DELETE">DELETE</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Status
              </label>
              <select
                value={filters.statusCode}
                onChange={(e) =>
                  handleFilterChange("statusCode", e.target.value)
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
              >
                <option value="">All Status</option>
                <option value="200">200 OK</option>
                <option value="201">201 Created</option>
                <option value="400">400 Bad Request</option>
                <option value="401">401 Unauthorized</option>
                <option value="404">404 Not Found</option>
                <option value="500">500 Server Error</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Slow APIs
              </label>
              <select
                value={filters.isSlow}
                onChange={(e) => handleFilterChange("isSlow", e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
              >
                <option value="">All</option>
                <option value="true">Slow Only (&gt;500ms)</option>
                <option value="false">Normal Only</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Broken APIs
              </label>
              <select
                value={filters.isBroken}
                onChange={(e) => handleFilterChange("isBroken", e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
              >
                <option value="">All</option>
                <option value="true">Errors Only (5xx)</option>
                <option value="false">Success Only</option>
              </select>
            </div>

            <div className="flex items-end">
              <button
                onClick={clearFilters}
                className="px-4 py-2 text-gray-600 hover:text-gray-800 transition"
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Logs Table */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Timestamp
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Service
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Method
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Endpoint
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Latency
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Flags
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {isLoading ? (
                <tr>
                  <td
                    colSpan="7"
                    className="px-4 py-8 text-center text-gray-500"
                  >
                    Loading logs...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td
                    colSpan="7"
                    className="px-4 py-8 text-center text-red-500"
                  >
                    Failed to load logs
                  </td>
                </tr>
              ) : data?.content?.length === 0 ? (
                <tr>
                  <td
                    colSpan="7"
                    className="px-4 py-8 text-center text-gray-500"
                  >
                    No logs found
                  </td>
                </tr>
              ) : (
                data?.content?.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-500 whitespace-nowrap">
                      {formatDate(log.timestamp)}
                    </td>
                    <td className="px-4 py-3 text-sm font-medium text-gray-900">
                      {log.serviceName}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={cn(
                          "px-2 py-1 text-xs font-medium rounded",
                          getMethodColor(log.method)
                        )}
                      >
                        {log.method}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900 max-w-xs truncate">
                      {log.endpoint}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={cn(
                          "px-2 py-1 text-xs font-medium rounded",
                          getStatusColor(log.statusCode)
                        )}
                      >
                        {log.statusCode}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900">
                      <span
                        className={
                          log.isSlow ? "text-yellow-600 font-medium" : ""
                        }
                      >
                        {formatLatency(log.latency)}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1">
                        {log.isSlow && (
                          <span className="px-2 py-1 text-xs font-medium rounded bg-yellow-100 text-yellow-700">
                            SLOW
                          </span>
                        )}
                        {log.isBroken && (
                          <span className="px-2 py-1 text-xs font-medium rounded bg-red-100 text-red-700">
                            ERROR
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && (
          <div className="px-4 py-3 border-t border-gray-200 flex items-center justify-between">
            <div className="text-sm text-gray-500">
              Showing {data.page * data.size + 1} to{" "}
              {Math.min((data.page + 1) * data.size, data.totalElements)} of{" "}
              {data.totalElements} results
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => handleFilterChange("page", filters.page - 1)}
                disabled={data.isFirst}
                className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Previous
              </button>
              <button
                onClick={() => handleFilterChange("page", filters.page + 1)}
                disabled={data.isLast}
                className="px-3 py-1 border border-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
