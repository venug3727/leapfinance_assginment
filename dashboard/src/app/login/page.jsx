"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import {
  Activity,
  AlertCircle,
  Shield,
  Zap,
  BarChart3,
  Bell,
} from "lucide-react";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [currentFeature, setCurrentFeature] = useState(0);
  const router = useRouter();
  const { login } = useAuth();

  const features = [
    {
      icon: BarChart3,
      title: "Real-time Analytics",
      desc: "Monitor API performance live",
    },
    {
      icon: Bell,
      title: "Smart Alerts",
      desc: "Get notified of issues instantly",
    },
    {
      icon: Zap,
      title: "Health Scores",
      desc: "AI-powered endpoint health tracking",
    },
    {
      icon: Shield,
      title: "Incident Management",
      desc: "Track and resolve issues fast",
    },
  ];

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentFeature((prev) => (prev + 1) % features.length);
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    const result = await login(username, password);

    if (result.success) {
      router.push("/dashboard");
    } else {
      setError(result.error);
    }

    setIsLoading(false);
  };

  return (
    <div className="min-h-screen flex">
      {/* Left Side - Features Showcase */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-primary-600 via-primary-700 to-indigo-800 p-12 flex-col justify-between">
        <div>
          <div className="flex items-center gap-3 text-white">
            <Activity className="w-10 h-10" />
            <span className="text-3xl font-bold">InfoPulse</span>
          </div>
          <p className="text-primary-100 mt-2 text-lg">
            API Monitoring & Observability Platform
          </p>
        </div>

        <div className="space-y-8">
          <div className="bg-white/10 backdrop-blur-sm rounded-2xl p-8 border border-white/20">
            <div className="flex items-center gap-4 mb-4">
              {(() => {
                const Icon = features[currentFeature].icon;
                return <Icon className="w-12 h-12 text-white" />;
              })()}
              <div>
                <h3 className="text-2xl font-bold text-white">
                  {features[currentFeature].title}
                </h3>
                <p className="text-primary-100">
                  {features[currentFeature].desc}
                </p>
              </div>
            </div>
            <div className="flex gap-2 mt-6">
              {features.map((_, idx) => (
                <div
                  key={idx}
                  className={`h-1.5 rounded-full transition-all duration-300 ${
                    idx === currentFeature ? "w-8 bg-white" : "w-2 bg-white/30"
                  }`}
                />
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/20">
              <div className="text-3xl font-bold text-white">99.9%</div>
              <div className="text-primary-200 text-sm">Uptime Tracking</div>
            </div>
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/20">
              <div className="text-3xl font-bold text-white">&lt;5ms</div>
              <div className="text-primary-200 text-sm">Real-time Updates</div>
            </div>
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/20">
              <div className="text-3xl font-bold text-white">24/7</div>
              <div className="text-primary-200 text-sm">Monitoring</div>
            </div>
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/20">
              <div className="text-3xl font-bold text-white">100+</div>
              <div className="text-primary-200 text-sm">Metrics Tracked</div>
            </div>
          </div>
        </div>

        <div className="text-primary-200 text-sm">
          Built with Spring Boot, Next.js, MongoDB & WebSocket
        </div>
      </div>

      {/* Right Side - Login Form */}
      <div className="flex-1 flex items-center justify-center p-8 bg-gray-50">
        <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md border border-gray-100">
          <div className="text-center mb-8 lg:hidden">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-primary-100 rounded-full mb-4">
              <Activity className="w-8 h-8 text-primary-600" />
            </div>
            <h1 className="text-2xl font-bold text-gray-900">InfoPulse</h1>
            <p className="text-gray-500 mt-1">API Monitoring Dashboard</p>
          </div>

          <div className="hidden lg:block mb-8">
            <h2 className="text-2xl font-bold text-gray-900">Welcome back</h2>
            <p className="text-gray-500 mt-1">
              Sign in to access your dashboard
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="flex items-center gap-2 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                <AlertCircle className="w-4 h-4" />
                {error}
              </div>
            )}

            <div>
              <label
                htmlFor="username"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Username
              </label>
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition"
                placeholder="Enter your username"
                required
              />
            </div>

            <div>
              <label
                htmlFor="password"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Password
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none transition"
                placeholder="Enter your password"
                required
              />
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3 bg-primary-600 text-white font-medium rounded-lg hover:bg-primary-700 focus:ring-4 focus:ring-primary-200 disabled:opacity-50 disabled:cursor-not-allowed transition"
            >
              {isLoading ? "Signing in..." : "Sign In"}
            </button>
          </form>

          <div className="mt-6 p-4 bg-gradient-to-r from-primary-50 to-indigo-50 rounded-lg border border-primary-100">
            <p className="text-sm text-gray-600 text-center">
              <strong className="text-primary-700">Demo Credentials:</strong>
              <br />
              Username:{" "}
              <code className="bg-white px-2 py-0.5 rounded text-primary-600 font-mono">
                admin
              </code>
              <br />
              Password:{" "}
              <code className="bg-white px-2 py-0.5 rounded text-primary-600 font-mono">
                admin123
              </code>
            </p>
          </div>

          <p className="mt-6 text-center text-xs text-gray-400">
            © 2024 InfoPulse by Leap Finance • v1.0.0
          </p>
        </div>
      </div>
    </div>
  );
}
