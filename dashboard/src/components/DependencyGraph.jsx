"use client";

import { useCallback, useEffect, useState } from "react";
import ReactFlow, {
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  MarkerType,
  Panel,
} from "reactflow";
import "reactflow/dist/style.css";
import {
  Activity,
  Server,
  Database,
  Globe,
  AlertTriangle,
  CheckCircle,
} from "lucide-react";

// Custom Node Component
const ServiceNode = ({ data }) => {
  const getStatusStyle = () => {
    if (data.healthScore >= 90)
      return {
        border: "border-green-500",
        bg: "bg-green-50",
        icon: <CheckCircle className="w-4 h-4 text-green-500" />,
      };
    if (data.healthScore >= 75)
      return {
        border: "border-blue-500",
        bg: "bg-blue-50",
        icon: <Activity className="w-4 h-4 text-blue-500" />,
      };
    if (data.healthScore >= 50)
      return {
        border: "border-yellow-500",
        bg: "bg-yellow-50",
        icon: <AlertTriangle className="w-4 h-4 text-yellow-500" />,
      };
    return {
      border: "border-red-500",
      bg: "bg-red-50",
      icon: <AlertTriangle className="w-4 h-4 text-red-500" />,
    };
  };

  const style = getStatusStyle();
  const Icon =
    data.type === "database"
      ? Database
      : data.type === "external"
      ? Globe
      : Server;

  return (
    <div
      className={`px-4 py-3 rounded-lg border-2 ${style.border} ${style.bg} shadow-lg min-w-[180px]`}
    >
      <div className="flex items-center gap-2 mb-2">
        <Icon className="w-5 h-5 text-gray-600" />
        <span className="font-semibold text-gray-800">{data.label}</span>
        {style.icon}
      </div>
      <div className="grid grid-cols-2 gap-2 text-xs">
        <div className="bg-white rounded px-2 py-1">
          <span className="text-gray-500">Health</span>
          <p className="font-bold text-gray-800">{data.healthScore}%</p>
        </div>
        <div className="bg-white rounded px-2 py-1">
          <span className="text-gray-500">Requests</span>
          <p className="font-bold text-gray-800">
            {data.requestCount?.toLocaleString() || 0}
          </p>
        </div>
        <div className="bg-white rounded px-2 py-1">
          <span className="text-gray-500">Avg Latency</span>
          <p className="font-bold text-gray-800">
            {data.avgLatency?.toFixed(0) || 0}ms
          </p>
        </div>
        <div className="bg-white rounded px-2 py-1">
          <span className="text-gray-500">Error Rate</span>
          <p className="font-bold text-gray-800">
            {((data.errorRate || 0) * 100).toFixed(1)}%
          </p>
        </div>
      </div>
    </div>
  );
};

// Custom Edge Label
const EdgeLabel = ({ data }) => {
  return (
    <div className="bg-white px-2 py-1 rounded shadow text-xs border">
      <span className="text-gray-600">{data.requestsPerMin}/min</span>
    </div>
  );
};

const nodeTypes = {
  serviceNode: ServiceNode,
};

// Generate nodes and edges from service data
const generateGraph = (services, endpoints) => {
  const nodes = [];
  const edges = [];

  // Create service nodes in a circular layout
  const centerX = 400;
  const centerY = 300;
  const radius = 250;

  // Add central "Gateway" node
  nodes.push({
    id: "gateway",
    type: "serviceNode",
    position: { x: centerX - 90, y: centerY - 50 },
    data: {
      label: "API Gateway",
      type: "gateway",
      healthScore:
        services?.length > 0
          ? Math.round(
              services.reduce((acc, s) => acc + s.healthScore, 0) /
                services.length
            )
          : 100,
      requestCount:
        services?.reduce((acc, s) => acc + (s.requestCount || 0), 0) || 0,
      avgLatency:
        services?.length > 0
          ? services.reduce((acc, s) => acc + (s.avgLatency || 0), 0) /
            services.length
          : 0,
      errorRate:
        services?.length > 0
          ? services.reduce((acc, s) => acc + (s.errorRate || 0), 0) /
            services.length
          : 0,
    },
  });

  // Add service nodes
  if (services && services.length > 0) {
    services.forEach((service, index) => {
      const angle = (2 * Math.PI * index) / services.length - Math.PI / 2;
      const x = centerX + radius * Math.cos(angle) - 90;
      const y = centerY + radius * Math.sin(angle) - 50;

      nodes.push({
        id: service.serviceName,
        type: "serviceNode",
        position: { x, y },
        data: {
          label: service.serviceName,
          type: "service",
          healthScore: service.healthScore,
          requestCount: service.requestCount,
          avgLatency: service.avgLatency || 0,
          errorRate: service.errorRate || 0,
        },
      });

      // Edge from gateway to service
      const edgeColor =
        service.healthScore >= 75
          ? "#22c55e"
          : service.healthScore >= 50
          ? "#eab308"
          : "#ef4444";
      edges.push({
        id: `gateway-${service.serviceName}`,
        source: "gateway",
        target: service.serviceName,
        animated: true,
        style: { stroke: edgeColor, strokeWidth: 2 },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: edgeColor,
        },
        label: `${Math.round((service.requestCount || 0) / 60)}/min`,
        labelStyle: { fontSize: 10, fill: "#666" },
        labelBgStyle: { fill: "white", fillOpacity: 0.8 },
      });
    });
  }

  // Add database node
  nodes.push({
    id: "mongodb",
    type: "serviceNode",
    position: { x: centerX + radius + 100, y: centerY - 50 },
    data: {
      label: "MongoDB",
      type: "database",
      healthScore: 95,
      requestCount:
        services?.reduce((acc, s) => acc + (s.requestCount || 0), 0) || 0,
      avgLatency: 5,
      errorRate: 0.001,
    },
  });

  // Connect services to database
  if (services && services.length > 0) {
    services.forEach((service) => {
      edges.push({
        id: `${service.serviceName}-mongodb`,
        source: service.serviceName,
        target: "mongodb",
        style: { stroke: "#94a3b8", strokeWidth: 1, strokeDasharray: "5,5" },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: "#94a3b8",
        },
      });
    });
  }

  return { nodes, edges };
};

export default function DependencyGraph({ services = [], endpoints = [] }) {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  useEffect(() => {
    const { nodes: newNodes, edges: newEdges } = generateGraph(
      services,
      endpoints
    );
    setNodes(newNodes);
    setEdges(newEdges);
  }, [services, endpoints, setNodes, setEdges]);

  return (
    <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
      <div className="px-6 py-4 border-b bg-gradient-to-r from-indigo-500 to-purple-600">
        <h2 className="text-lg font-semibold text-white">
          Service Dependency Graph
        </h2>
        <p className="text-white/80 text-sm">
          Real-time visualization of API relationships
        </p>
      </div>

      <div className="h-[500px]">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          nodeTypes={nodeTypes}
          fitView
          attributionPosition="bottom-left"
        >
          <Background color="#f1f5f9" gap={20} />
          <Controls />
          <Panel
            position="top-right"
            className="bg-white p-3 rounded-lg shadow border"
          >
            <div className="text-sm font-medium text-gray-700 mb-2">Legend</div>
            <div className="space-y-1 text-xs">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-green-500"></div>
                <span>Healthy (90-100%)</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-blue-500"></div>
                <span>Good (75-89%)</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
                <span>Warning (50-74%)</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-red-500"></div>
                <span>Critical (&lt;50%)</span>
              </div>
            </div>
          </Panel>
        </ReactFlow>
      </div>
    </div>
  );
}
