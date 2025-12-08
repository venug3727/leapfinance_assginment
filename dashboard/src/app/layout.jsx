import "./globals.css";

export const metadata = {
  title: "InfoPulse - API Monitoring Dashboard",
  description: "API Monitoring & Observability Platform by Leap Finance",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-gray-50">{children}</body>
    </html>
  );
}
