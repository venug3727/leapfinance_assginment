"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  useRef,
} from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "http://localhost:8080/ws";

const WebSocketContext = createContext(null);

export function WebSocketProvider({ children }) {
  const [connected, setConnected] = useState(false);
  const [lastMessage, setLastMessage] = useState(null);
  const clientRef = useRef(null);
  const subscribersRef = useRef({});
  const reconnectAttemptsRef = useRef(0);
  const maxReconnectAttempts = 10;

  // Message handlers by topic
  const messageHandlers = useRef({
    "/topic/logs": [],
    "/topic/alerts": [],
    "/topic/incidents": [],
    "/topic/health": [],
    "/topic/metrics": [],
  });

  const connect = useCallback(() => {
    if (clientRef.current?.connected) {
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        if (process.env.NODE_ENV === "development") {
          console.log("STOMP:", str);
        }
      },
      onConnect: () => {
        console.log("✅ WebSocket Connected");
        setConnected(true);
        reconnectAttemptsRef.current = 0;

        // Subscribe to all topics
        Object.keys(messageHandlers.current).forEach((topic) => {
          subscribersRef.current[topic] = client.subscribe(topic, (message) => {
            try {
              const data = JSON.parse(message.body);
              setLastMessage({ topic, data, timestamp: new Date() });

              // Call all handlers for this topic
              messageHandlers.current[topic].forEach((handler) =>
                handler(data)
              );
            } catch (error) {
              console.error("Error parsing WebSocket message:", error);
            }
          });
        });
      },
      onDisconnect: () => {
        console.log("❌ WebSocket Disconnected");
        setConnected(false);
      },
      onStompError: (frame) => {
        console.error("STOMP Error:", frame);
        reconnectAttemptsRef.current++;

        if (reconnectAttemptsRef.current >= maxReconnectAttempts) {
          console.error("Max reconnection attempts reached");
          client.deactivate();
        }
      },
    });

    client.activate();
    clientRef.current = client;
  }, []);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      // Unsubscribe from all topics
      Object.values(subscribersRef.current).forEach((subscription) => {
        subscription?.unsubscribe();
      });
      subscribersRef.current = {};

      clientRef.current.deactivate();
      clientRef.current = null;
      setConnected(false);
    }
  }, []);

  const subscribe = useCallback((topic, handler) => {
    if (!messageHandlers.current[topic]) {
      messageHandlers.current[topic] = [];
    }
    messageHandlers.current[topic].push(handler);

    // Return unsubscribe function
    return () => {
      messageHandlers.current[topic] = messageHandlers.current[topic].filter(
        (h) => h !== handler
      );
    };
  }, []);

  // Connect on mount
  useEffect(() => {
    connect();
    return () => disconnect();
  }, [connect, disconnect]);

  const value = {
    connected,
    lastMessage,
    subscribe,
    connect,
    disconnect,
  };

  return (
    <WebSocketContext.Provider value={value}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocket() {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error("useWebSocket must be used within a WebSocketProvider");
  }
  return context;
}

// Hook for subscribing to specific topics
export function useWebSocketTopic(topic, handler) {
  const { subscribe, connected } = useWebSocket();

  useEffect(() => {
    if (!connected || !handler) return;

    const unsubscribe = subscribe(topic, handler);
    return () => unsubscribe();
  }, [topic, handler, subscribe, connected]);
}

// Hook for real-time logs
export function useRealtimeLogs(onNewLog) {
  useWebSocketTopic("/topic/logs", onNewLog);
}

// Hook for real-time alerts
export function useRealtimeAlerts(onNewAlert) {
  useWebSocketTopic("/topic/alerts", onNewAlert);
}

// Hook for real-time health updates
export function useRealtimeHealth(onHealthUpdate) {
  useWebSocketTopic("/topic/health", onHealthUpdate);
}

// Hook for real-time metrics
export function useRealtimeMetrics(onMetricsUpdate) {
  useWebSocketTopic("/topic/metrics", onMetricsUpdate);
}
