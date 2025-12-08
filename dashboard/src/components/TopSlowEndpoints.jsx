"use client";

import { formatLatency } from "@/lib/utils";

export default function TopSlowEndpoints({ endpoints }) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        Top 5 Slowest Endpoints
      </h3>

      {endpoints.length === 0 ? (
        <p className="text-gray-500 text-center py-8">No data available</p>
      ) : (
        <div className="space-y-4">
          {endpoints.map((endpoint, index) => (
            <div
              key={`${endpoint.endpoint}-${index}`}
              className="flex items-center gap-4"
            >
              <div className="flex-shrink-0 w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center text-sm font-medium text-gray-600">
                {index + 1}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">
                  {endpoint.endpoint}
                </p>
                <p className="text-xs text-gray-500">
                  {endpoint.serviceName} • {endpoint.requestCount} requests
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-yellow-600">
                  {formatLatency(Math.round(endpoint.avgLatency))}
                </p>
                <p className="text-xs text-gray-500">
                  max: {formatLatency(endpoint.maxLatency)}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
