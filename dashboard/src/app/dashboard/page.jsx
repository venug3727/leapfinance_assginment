"use client";

import { useState, useEffect } from "react";
import useSWR from "swr";
import Link from "next/link";
import { dashboardApi, alertApi, healthScoreApi } from "@/lib/api";
import { formatLatency } from "@/lib/utils";
import StatsCard from "@/components/StatsCard";
import TopSlowEndpoints from "@/components/TopSlowEndpoints";
import ErrorRateChart from "@/components/ErrorRateChart";
import RecentAlerts from "@/components/RecentAlerts";
import {
  Activity,
  AlertTriangle,
  XCircle,
  Gauge,
  Clock,
  Bell,
  Radio,
  HeartPulse,
  ArrowRight,
} from "lucide-react";

const fetcher = async (key) => {
  if (key === "summary") {
    const res = await dashboardApi.getSummary();
    return res.data.data;
  }
  if (key === "slowEndpoints") {
    const res = await dashboardApi.getTopSlowEndpoints(5);
    return res.data.data;
  }
  if (key === "errorRate") {
    const res = await dashboardApi.getErrorRateGraph();
    return res.data.data;
  }
  if (key === "alerts") {
    const res = await alertApi.getUnacknowledgedAlerts(0, 5);
    return res.data.data;
  }
  if (key === "health") {
    const res = await healthScoreApi.getSystemHealth();
    return res.data.data;
  }
};

export default function DashboardPage() {
  const { data: summary, error: summaryError } = useSWR("summary", fetcher, {
    refreshInterval: 5000, // Refresh every 5 seconds for real-time updates
  });

  const { data: slowEndpoints } = useSWR("slowEndpoints", fetcher, {
    refreshInterval: 10000, // Refresh every 10 seconds
  });

  const { data: errorRateData } = useSWR("errorRate", fetcher, {
    refreshInterval: 10000, // Refresh every 10 seconds
  });

  const { data: alertsData, mutate: mutateAlerts } = useSWR("alerts", fetcher, {
    refreshInterval: 5000, // Refresh every 5 seconds for real-time alerts
  });

  const { data: healthData } = useSWR("health", fetcher, {
    refreshInterval: 5000,
  });

  if (summaryError) {
    return (
      <div className="p-6 bg-red-50 rounded-lg">
        <p className="text-red-700">
          Failed to load dashboard data. Please try again.
        </p>
      </div>
    );
  }

  const getHealthColor = (score) => {
    if (score >= 90)
      return {
        bg: "from-green-500 to-green-600",
        text: "text-green-700",
        status: "EXCELLENT",
      };
    if (score >= 75)
      return {
        bg: "from-blue-500 to-blue-600",
        text: "text-blue-700",
        status: "GOOD",
      };
    if (score >= 50)
      return {
        bg: "from-yellow-500 to-yellow-600",
        text: "text-yellow-700",
        status: "WARNING",
      };
    return {
      bg: "from-red-500 to-red-600",
      text: "text-red-700",
      status: "CRITICAL",
    };
  };

  const healthColor = getHealthColor(healthData?.overallScore || 100);

  const stats = [
    {
      title: "Total Requests",
      value: summary?.totalRequests?.toLocaleString() || "0",
      icon: Activity,
      color: "blue",
    },
    {
      title: "Slow APIs",
      value: summary?.slowApiCount?.toLocaleString() || "0",
      icon: Clock,
      color: "yellow",
      subtitle: "> 500ms",
    },
    {
      title: "Broken APIs",
      value: summary?.brokenApiCount?.toLocaleString() || "0",
      icon: XCircle,
      color: "red",
      subtitle: "5xx errors",
    },
    {
      title: "Rate Limit Hits",
      value: summary?.rateLimitViolations?.toLocaleString() || "0",
      icon: Gauge,
      color: "orange",
    },
    {
      title: "Avg Latency",
      value: summary
        ? formatLatency(Math.round(summary.averageLatency))
        : "0ms",
      icon: Activity,
      color: "green",
    },
    {
      title: "Open Incidents",
      value: summary?.openIncidents?.toLocaleString() || "0",
      icon: AlertTriangle,
      color: "purple",
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-500 mt-1">
            Overview of your API monitoring metrics
          </p>
        </div>
        <div className="flex items-center gap-3">
          {/* Health Score Badge */}
          <Link
            href="/dashboard/health"
            className={`flex items-center gap-3 px-4 py-2 bg-gradient-to-r ${healthColor.bg} rounded-lg shadow hover:shadow-md transition-all`}
          >
            <HeartPulse className="w-5 h-5 text-white" />
            <div className="text-white">
              <div className="text-2xl font-bold leading-none">
                {healthData?.overallScore || "--"}
              </div>
              <div className="text-xs opacity-80">Health Score</div>
            </div>
            <ArrowRight className="w-4 h-4 text-white/70" />
          </Link>

          <div className="flex items-center gap-2 px-3 py-1.5 bg-green-50 border border-green-200 rounded-full">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
            </span>
            <span className="text-sm font-medium text-green-700">Live</span>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {stats.map((stat) => (
          <StatsCard key={stat.title} {...stat} />
        ))}
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <TopSlowEndpoints endpoints={slowEndpoints || []} />
        <ErrorRateChart data={errorRateData} />
      </div>

      {/* Recent Alerts */}
      <RecentAlerts
        alerts={alertsData?.content || []}
        onAcknowledge={mutateAlerts}
      />
    </div>
  );
}
