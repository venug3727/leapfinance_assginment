"use client";

import { useState, useEffect } from "react";
import {
  Activity,
  TrendingUp,
  TrendingDown,
  Minus,
  AlertCircle,
  CheckCircle,
  AlertTriangle,
  XCircle,
} from "lucide-react";

const getStatusColor = (status) => {
  switch (status) {
    case "EXCELLENT":
      return {
        bg: "bg-green-100",
        text: "text-green-700",
        ring: "ring-green-500",
        gradient: "from-green-500 to-green-600",
      };
    case "GOOD":
      return {
        bg: "bg-blue-100",
        text: "text-blue-700",
        ring: "ring-blue-500",
        gradient: "from-blue-500 to-blue-600",
      };
    case "WARNING":
      return {
        bg: "bg-yellow-100",
        text: "text-yellow-700",
        ring: "ring-yellow-500",
        gradient: "from-yellow-500 to-yellow-600",
      };
    case "CRITICAL":
      return {
        bg: "bg-red-100",
        text: "text-red-700",
        ring: "ring-red-500",
        gradient: "from-red-500 to-red-600",
      };
    default:
      return {
        bg: "bg-gray-100",
        text: "text-gray-700",
        ring: "ring-gray-500",
        gradient: "from-gray-500 to-gray-600",
      };
  }
};

const getStatusIcon = (status) => {
  switch (status) {
    case "EXCELLENT":
      return <CheckCircle className="w-5 h-5 text-green-500" />;
    case "GOOD":
      return <TrendingUp className="w-5 h-5 text-blue-500" />;
    case "WARNING":
      return <AlertTriangle className="w-5 h-5 text-yellow-500" />;
    case "CRITICAL":
      return <XCircle className="w-5 h-5 text-red-500" />;
    default:
      return <Activity className="w-5 h-5 text-gray-500" />;
  }
};

// Circular Progress Component
const CircularProgress = ({ score, size = 120, strokeWidth = 10, status }) => {
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const offset = circumference - (score / 100) * circumference;
  const colors = getStatusColor(status);

  return (
    <div className="relative inline-flex items-center justify-center">
      <svg width={size} height={size} className="transform -rotate-90">
        {/* Background circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={strokeWidth}
          className="text-gray-200"
        />
        {/* Progress circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="url(#gradient)"
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className="transition-all duration-1000 ease-out"
        />
        <defs>
          <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop
              offset="0%"
              className={`${colors.text}`}
              stopColor="currentColor"
            />
            <stop
              offset="100%"
              className={`${colors.text}`}
              stopColor="currentColor"
              stopOpacity="0.7"
            />
          </linearGradient>
        </defs>
      </svg>
      {/* Score text */}
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className={`text-3xl font-bold ${colors.text}`}>{score}</span>
        <span className="text-xs text-gray-500 uppercase tracking-wider">
          Score
        </span>
      </div>
    </div>
  );
};

// Score Breakdown Bar
const ScoreBar = ({ label, score, color }) => {
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-sm">
        <span className="text-gray-600">{label}</span>
        <span className="font-medium">{score}%</span>
      </div>
      <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
        <div
          className={`h-full ${color} transition-all duration-1000 ease-out rounded-full`}
          style={{ width: `${score}%` }}
        />
      </div>
    </div>
  );
};

// Service Health Card
const ServiceHealthCard = ({ service }) => {
  const colors = getStatusColor(service.status);

  return (
    <div className={`p-4 rounded-lg border ${colors.bg} border-opacity-50`}>
      <div className="flex items-center justify-between mb-2">
        <h4 className="font-medium text-gray-900">{service.serviceName}</h4>
        <span className={`text-2xl font-bold ${colors.text}`}>
          {service.healthScore}
        </span>
      </div>
      <div className="flex items-center gap-4 text-sm text-gray-600">
        <span>{service.endpointCount} endpoints</span>
        <span>{service.requestCount.toLocaleString()} requests</span>
      </div>
      {(service.criticalEndpoints > 0 || service.warningEndpoints > 0) && (
        <div className="flex items-center gap-3 mt-2 pt-2 border-t border-gray-200">
          {service.criticalEndpoints > 0 && (
            <span className="flex items-center gap-1 text-xs text-red-600">
              <XCircle className="w-3 h-3" />
              {service.criticalEndpoints} critical
            </span>
          )}
          {service.warningEndpoints > 0 && (
            <span className="flex items-center gap-1 text-xs text-yellow-600">
              <AlertTriangle className="w-3 h-3" />
              {service.warningEndpoints} warning
            </span>
          )}
        </div>
      )}
    </div>
  );
};

// Main Health Score Component
export default function HealthScore({ healthData }) {
  if (!healthData) {
    return (
      <div className="bg-white rounded-xl shadow-sm border p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-gray-200 rounded w-1/3 mb-4"></div>
          <div className="flex justify-center mb-4">
            <div className="w-32 h-32 bg-gray-200 rounded-full"></div>
          </div>
        </div>
      </div>
    );
  }

  const {
    overallScore,
    availabilityScore,
    latencyScore,
    errorScore,
    status,
    serviceScores,
    endpointScores,
  } = healthData;
  const colors = getStatusColor(status);

  return (
    <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
      {/* Header with gradient */}
      <div className={`bg-gradient-to-r ${colors.gradient} px-6 py-4`}>
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-white">
              System Health Score
            </h2>
            <p className="text-white/80 text-sm">
              Real-time API health monitoring
            </p>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 bg-white/20 rounded-full">
            {getStatusIcon(status)}
            <span className="text-white font-medium">{status}</span>
          </div>
        </div>
      </div>

      <div className="p-6">
        {/* Main Score */}
        <div className="flex flex-col lg:flex-row items-center gap-8 mb-6">
          <CircularProgress
            score={overallScore}
            status={status}
            size={140}
            strokeWidth={12}
          />

          {/* Score Breakdown */}
          <div className="flex-1 w-full space-y-4">
            <ScoreBar
              label="Availability"
              score={availabilityScore}
              color="bg-green-500"
            />
            <ScoreBar
              label="Latency"
              score={latencyScore}
              color="bg-blue-500"
            />
            <ScoreBar
              label="Error Rate"
              score={errorScore}
              color="bg-purple-500"
            />
          </div>
        </div>

        {/* Service Scores */}
        {serviceScores && serviceScores.length > 0 && (
          <div className="mt-6 pt-6 border-t">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">
              Service Health
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {serviceScores.slice(0, 4).map((service, index) => (
                <ServiceHealthCard key={index} service={service} />
              ))}
            </div>
          </div>
        )}

        {/* Worst Performing Endpoints */}
        {endpointScores && endpointScores.length > 0 && (
          <div className="mt-6 pt-6 border-t">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">
              Endpoints Needing Attention
            </h3>
            <div className="space-y-2">
              {endpointScores.slice(0, 5).map((endpoint, index) => {
                const endpointColors = getStatusColor(endpoint.status);
                return (
                  <div
                    key={index}
                    className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                  >
                    <div className="flex items-center gap-3">
                      {getStatusIcon(endpoint.status)}
                      <div>
                        <p className="font-medium text-sm text-gray-900">
                          {endpoint.endpoint}
                        </p>
                        <p className="text-xs text-gray-500">
                          {endpoint.serviceName} • {endpoint.method}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className={`text-lg font-bold ${endpointColors.text}`}>
                        {endpoint.healthScore}
                      </p>
                      <p className="text-xs text-gray-500">
                        {endpoint.avgLatency?.toFixed(0)}ms avg
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
