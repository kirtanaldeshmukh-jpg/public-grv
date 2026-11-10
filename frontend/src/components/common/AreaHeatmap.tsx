import React from 'react';
import { MapContainer, TileLayer, Circle, Popup, Tooltip } from 'react-leaflet';
import { AreaOverdue } from '../../types';

interface AreaHeatmapProps {
  areas: AreaOverdue[];
  height?: string;
  className?: string;
}

export const AreaHeatmap: React.FC<AreaHeatmapProps> = ({
  areas,
  height = '380px',
  className = '',
}) => {
  // Center map on Bengaluru urban centroid
  const defaultCenter: [number, number] = [12.9716, 77.5946];

  const getCircleColor = (area: AreaOverdue) => {
    if (area.overdueCount > 0) {
      return {
        color: '#dc2626',
        fillColor: '#ef4444',
        fillOpacity: 0.35,
      };
    }
    if (area.criticalCount > 0) {
      return {
        color: '#ea580c',
        fillColor: '#f97316',
        fillOpacity: 0.3,
      };
    }
    if (area.complaintCount > 0) {
      return {
        color: '#0d9488',
        fillColor: '#14b8a6',
        fillOpacity: 0.25,
      };
    }
    return {
      color: '#64748b',
      fillColor: '#94a3b8',
      fillOpacity: 0.15,
    };
  };

  return (
    <div
      className={`relative rounded-2xl overflow-hidden border border-gray-200 shadow-xs ${className}`}
      style={{ height }}
    >
      <MapContainer
        center={defaultCenter}
        zoom={11}
        scrollWheelZoom={false}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {areas.map((area, idx) => {
          const style = getCircleColor(area);
          const position: [number, number] = [area.latitude, area.longitude];
          const radius = area.radiusMeters || 800;

          return (
            <Circle
              key={`${area.areaName}-${idx}`}
              center={position}
              radius={radius}
              pathOptions={{
                color: style.color,
                fillColor: style.fillColor,
                fillOpacity: style.fillOpacity,
                weight: 2,
              }}
            >
              <Tooltip direction="top" offset={[0, -10]} opacity={0.95}>
                <div className="text-xs font-sans">
                  <strong className="text-gray-900 block font-bold">{area.areaName}</strong>
                  <span className="text-gray-600 block">
                    {area.complaintCount} Grievance{area.complaintCount === 1 ? '' : 's'}
                  </span>
                  {area.overdueCount > 0 && (
                    <span className="text-red-600 font-bold block">
                      ⚠ {area.overdueCount} Overdue Breach{area.overdueCount === 1 ? '' : 'es'}
                    </span>
                  )}
                </div>
              </Tooltip>

              <Popup>
                <div className="text-xs p-1 min-w-[180px] font-sans">
                  <div className="flex items-center justify-between gap-2 pb-1 border-b border-gray-200">
                    <strong className="text-gray-900 font-bold">{area.areaName}</strong>
                    <span
                      className={`text-[10px] font-extrabold px-1.5 py-0.5 rounded-full ${
                        area.overdueCount > 0
                          ? 'bg-red-100 text-red-700'
                          : area.criticalCount > 0
                          ? 'bg-amber-100 text-amber-700'
                          : 'bg-teal/10 text-teal'
                      }`}
                    >
                      {area.overdueCount > 0 ? 'Overdue Breach' : 'SLA Compliant'}
                    </span>
                  </div>

                  <div className="mt-2 space-y-1 text-gray-700">
                    <div className="flex justify-between">
                      <span className="text-gray-500">Total Grievances:</span>
                      <strong className="font-semibold">{area.complaintCount}</strong>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500">Overdue Breaches:</span>
                      <strong className={area.overdueCount > 0 ? 'text-red-600 font-bold' : ''}>
                        {area.overdueCount}
                      </strong>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500">Critical Risk (&lt;50):</span>
                      <strong className={area.criticalCount > 0 ? 'text-amber-600 font-bold' : ''}>
                        {area.criticalCount}
                      </strong>
                    </div>
                  </div>

                  <div className="mt-2 pt-1 border-t border-gray-100 text-[10px] text-gray-400 font-mono">
                    {position[0].toFixed(4)}° N, {position[1].toFixed(4)}° E • Radius {radius}m
                  </div>
                </div>
              </Popup>
            </Circle>
          );
        })}
      </MapContainer>

      {/* Map Legend Overlay */}
      <div className="absolute bottom-3 right-3 z-[1000] bg-white/95 backdrop-blur-xs border border-gray-200 rounded-xl p-2.5 shadow-md text-[11px] space-y-1.5 pointer-events-auto">
        <span className="font-bold text-gray-800 block text-[10px] uppercase tracking-wider">
          Area Heatmap Legend
        </span>
        <div className="flex items-center gap-2 text-gray-600">
          <span className="w-3 h-3 rounded-full bg-red-500/80 border border-red-700"></span>
          <span>Overdue SLA Breaches</span>
        </div>
        <div className="flex items-center gap-2 text-gray-600">
          <span className="w-3 h-3 rounded-full bg-amber-500/80 border border-amber-700"></span>
          <span>Critical Attention (&lt;50 Health)</span>
        </div>
        <div className="flex items-center gap-2 text-gray-600">
          <span className="w-3 h-3 rounded-full bg-teal/80 border border-teal-700"></span>
          <span>Active On-Time Caseload</span>
        </div>
      </div>
    </div>
  );
};

