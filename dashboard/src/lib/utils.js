import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

export function formatDate(date) {
  return new Date(date).toLocaleString();
}

export function formatLatency(ms) {
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}

export function getStatusColor(status) {
  if (status >= 500) return "text-red-600 bg-red-100";
  if (status >= 400) return "text-yellow-600 bg-yellow-100";
  if (status >= 300) return "text-blue-600 bg-blue-100";
  return "text-green-600 bg-green-100";
}

export function getMethodColor(method) {
  const colors = {
    GET: "text-green-700 bg-green-100",
    POST: "text-blue-700 bg-blue-100",
    PUT: "text-yellow-700 bg-yellow-100",
    PATCH: "text-orange-700 bg-orange-100",
    DELETE: "text-red-700 bg-red-100",
  };
  return colors[method] || "text-gray-700 bg-gray-100";
}
