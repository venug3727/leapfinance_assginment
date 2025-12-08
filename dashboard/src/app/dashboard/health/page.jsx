"use client";

import { useState, useEffect, useCallback } from "react";
import useSWR from "swr";
import { healthScoreApi } from "@/lib/api";
import HealthScore from "@/components/HealthScore";
import DependencyGraph from "@/components/DependencyGraph";
import { Activity, TrendingUp, RefreshCw } from "lucide-react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
} from "recharts";
import { format } from "date-fns";

const fetcher = async (key) => {
  if (key === "systemHealth") {
    const res = await healthScoreApi.getSystemHealth();
    return res.data.data;
  }
  if (key === "healthTrend") {
    const res = await healthScoreApi.getTrend(24, 30);
    return res.data.data;
  }
  if (key === "endpointScores") {
    const res = await healthScoreApi.getEndpointScores();
    return res.data.data;
  }
};

// Health Trend Chart Component
function HealthTrendChart({ data }) {
  if (!data || data.length === 0) {
    return (
      <div className="bg-white rounded-xl shadow-sm border p-6">
        <div className="h-64 flex items-center justify-center text-gray-400">
          No trend data available
        </div>
      </div>
    );
  }

  const chartData = data.map((point) => ({
    timestamp: new Date(point.timestamp).getTime(),
    score: point.score,
    status: point.status,
  }));

  const getStatusColor = (status) => {
    switch (status) {
      case "EXCELLENT":
        return "#22c55e";
      case "GOOD":
        return "#3b82f6";
      case "WARNING":
        return "#eab308";
      case "CRITICAL":
        return "#ef4444";
      default:
        return "#6b7280";
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
      <div className="px-6 py-4 border-b">
        <div className="flex items-center gap-2">
          <TrendingUp className="w-5 h-5 text-indigo-600" />
          <h2 className="text-lg font-semibold text-gray-800">
            Health Score Trend
          </h2>
        </div>
        <p className="text-gray-500 text-sm mt-1">Last 24 hours</p>
      </div>
      <div className="p-6">
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis
              dataKey="timestamp"
              tickFormatter={(ts) => format(new Date(ts), "HH:mm")}
              stroke="#9ca3af"
              fontSize={12}
            />
            <YAxis
              domain={[0, 100]}
              stroke="#9ca3af"
              fontSize={12}
              tickFormatter={(value) => `${value}%`}
            />
            <Tooltip
              labelFormatter={(ts) => format(new Date(ts), "MMM dd, HH:mm")}
              formatter={(value) => [`${value}%`, "Health Score"]}
              contentStyle={{
                backgroundColor: "white",
                border: "1px solid #e5e7eb",
                borderRadius: "8px",
                boxShadow: "0 4px 6px -1px rgba(0,0,0,0.1)",
              }}
            />
            <ReferenceLine
              y={90}
              stroke="#22c55e"
              strokeDasharray="3 3"
              label={{
                value: "Excellent",
                position: "right",
                fill: "#22c55e",
                fontSize: 10,
              }}
            />
            <ReferenceLine
              y={75}
              stroke="#3b82f6"
              strokeDasharray="3 3"
              label={{
                value: "Good",
                position: "right",
                fill: "#3b82f6",
                fontSize: 10,
              }}
            />
            <ReferenceLine
              y={50}
              stroke="#eab308"
              strokeDasharray="3 3"
              label={{
                value: "Warning",
                position: "right",
                fill: "#eab308",
                fontSize: 10,
              }}
            />
            <Line
              type="monotone"
              dataKey="score"
              stroke="#6366f1"
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 6, fill: "#6366f1" }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

// Endpoint Health Table
function EndpointHealthTable({ endpoints }) {
  if (!endpoints || endpoints.length === 0) {
    return null;
  }

  const getStatusBadge = (status) => {
    const styles = {
      EXCELLENT: "bg-green-100 text-green-700",
      GOOD: "bg-blue-100 text-blue-700",
      WARNING: "bg-yellow-100 text-yellow-700",
      CRITICAL: "bg-red-100 text-red-700",
    };
    return styles[status] || "bg-gray-100 text-gray-700";
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
      <div className="px-6 py-4 border-b">
        <h2 className="text-lg font-semibold text-gray-800">All Endpoints</h2>
        <p className="text-gray-500 text-sm mt-1">
          Detailed health scores for each endpoint
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Endpoint
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Service
              </th>
              <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">
                Health
              </th>
              <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">
                Status
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                Avg Latency
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                P95 Latency
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                Error Rate
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                Requests
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {endpoints.map((endpoint, index) => (
              <tr key={index} className="hover:bg-gray-50">
                <td className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    <span
                      className={`px-2 py-0.5 text-xs font-medium rounded ${
                        endpoint.method === "GET"
                          ? "bg-blue-100 text-blue-700"
                          : endpoint.method === "POST"
                          ? "bg-green-100 text-green-700"
                          : endpoint.method === "PUT"
                          ? "bg-yellow-100 text-yellow-700"
                          : "bg-red-100 text-red-700"
                      }`}
                    >
                      {endpoint.method}
                    </span>
                    <span className="font-medium text-gray-900 text-sm">
                      {endpoint.endpoint}
                    </span>
                  </div>
                </td>
                <td className="px-4 py-3 text-sm text-gray-600">
                  {endpoint.serviceName}
                </td>
                <td className="px-4 py-3 text-center">
                  <span className="text-lg font-bold text-gray-900">
                    {endpoint.healthScore}
                  </span>
                </td>
                <td className="px-4 py-3 text-center">
                  <span
                    className={`px-2 py-1 text-xs font-medium rounded-full ${getStatusBadge(
                      endpoint.status
                    )}`}
                  >
                    {endpoint.status}
                  </span>
                </td>
                <td className="px-4 py-3 text-right text-sm text-gray-600">
                  {endpoint.avgLatency?.toFixed(0)}ms
                </td>
                <td className="px-4 py-3 text-right text-sm text-gray-600">
                  {endpoint.p95Latency}ms
                </td>
                <td className="px-4 py-3 text-right text-sm text-gray-600">
                  {((endpoint.errorRate || 0) * 100).toFixed(1)}%
                </td>
                <td className="px-4 py-3 text-right text-sm text-gray-600">
                  {endpoint.requestCount?.toLocaleString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function HealthPage() {
  const {
    data: healthData,
    error: healthError,
    mutate: mutateHealth,
  } = useSWR("systemHealth", fetcher, { refreshInterval: 5000 });

  const { data: trendData, error: trendError } = useSWR(
    "healthTrend",
    fetcher,
    { refreshInterval: 30000 }
  );

  const { data: endpointData, error: endpointError } = useSWR(
    "endpointScores",
    fetcher,
    { refreshInterval: 10000 }
  );

  const [isRefreshing, setIsRefreshing] = useState(false);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await mutateHealth();
    setTimeout(() => setIsRefreshing(false), 500);
  };

  if (healthError) {
    return (
      <div className="p-6 bg-red-50 rounded-lg">
        <p className="text-red-700">
          Failed to load health data. Please try again.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            API Health Monitor
          </h1>
          <p className="text-gray-500 mt-1">
            Comprehensive health scores and service dependencies
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleRefresh}
            className="flex items-center gap-2 px-4 py-2 bg-white border rounded-lg hover:bg-gray-50 transition-colors"
          >
            <RefreshCw
              className={`w-4 h-4 ${isRefreshing ? "animate-spin" : ""}`}
            />
            <span>Refresh</span>
          </button>
          <div className="flex items-center gap-2 px-3 py-1.5 bg-green-50 border border-green-200 rounded-full">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
            </span>
            <span className="text-sm font-medium text-green-700">Live</span>
          </div>
        </div>
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Health Score Card */}
        <HealthScore healthData={healthData} />

        {/* Health Trend Chart */}
        <HealthTrendChart data={trendData} />
      </div>

      {/* Dependency Graph */}
      <DependencyGraph
        services={healthData?.serviceScores || []}
        endpoints={endpointData || []}
      />

      {/* Endpoint Health Table */}
      <EndpointHealthTable endpoints={endpointData} />
    </div>
  );
}
