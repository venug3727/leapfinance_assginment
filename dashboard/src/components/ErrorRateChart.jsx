"use client";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { format } from "date-fns";

export default function ErrorRateChart({ data }) {
  const chartData =
    data?.dataPoints?.map((point) => ({
      timestamp: format(new Date(point.timestamp), "HH:mm"),
      errorRate: point.value.toFixed(2),
    })) || [];

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        Error Rate Over Time
      </h3>

      {chartData.length === 0 ? (
        <p className="text-gray-500 text-center py-8">No data available</p>
      ) : (
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis
                dataKey="timestamp"
                tick={{ fontSize: 12 }}
                stroke="#9ca3af"
              />
              <YAxis
                tick={{ fontSize: 12 }}
                stroke="#9ca3af"
                tickFormatter={(value) => `${value}%`}
              />
              <Tooltip
                formatter={(value) => [`${value}%`, "Error Rate"]}
                labelStyle={{ color: "#374151" }}
                contentStyle={{
                  borderRadius: "8px",
                  border: "1px solid #e5e7eb",
                  boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
                }}
              />
              <Line
                type="monotone"
                dataKey="errorRate"
                stroke="#ef4444"
                strokeWidth={2}
                dot={false}
                activeDot={{ r: 4, fill: "#ef4444" }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}
