"use client";

import { AuthProvider } from "@/lib/auth";

export default function LoginLayout({ children }) {
  return <AuthProvider>{children}</AuthProvider>;
}
