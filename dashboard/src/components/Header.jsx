"use client";

import { useAuth } from "@/lib/auth";
import { useWebSocket } from "@/lib/websocket";
import { useRouter } from "next/navigation";
import { LogOut, User, Wifi, WifiOff } from "lucide-react";

export default function Header() {
  const { user, logout } = useAuth();
  const { connected } = useWebSocket();
  const router = useRouter();

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-sm text-gray-500">
            API Monitoring & Observability
          </h2>
        </div>

        <div className="flex items-center gap-4">
          {/* WebSocket Status */}
          <div
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
              connected
                ? "bg-green-50 text-green-700 border border-green-200"
                : "bg-red-50 text-red-700 border border-red-200"
            }`}
          >
            {connected ? (
              <>
                <Wifi className="w-3 h-3" />
                <span>Real-time</span>
              </>
            ) : (
              <>
                <WifiOff className="w-3 h-3" />
                <span>Offline</span>
              </>
            )}
          </div>

          <div className="flex items-center gap-2 text-sm text-gray-700">
            <User className="w-4 h-4" />
            <span>{user?.username}</span>
            <span className="px-2 py-0.5 bg-primary-100 text-primary-700 text-xs rounded">
              {user?.role}
            </span>
          </div>

          <button
            onClick={handleLogout}
            className="flex items-center gap-2 px-3 py-2 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition"
          >
            <LogOut className="w-4 h-4" />
            Logout
          </button>
        </div>
      </div>
    </header>
  );
}
