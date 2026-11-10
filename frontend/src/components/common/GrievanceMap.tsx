import React from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';

// Fix for default Leaflet marker icon in Vite bundles
const customMarkerIcon = new L.Icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

interface GrievanceMapProps {
  latitude?: number | null;
  longitude?: number | null;
  title?: string;
  locationAddress?: string;
  className?: string;
  height?: string;
}

export const GrievanceMap: React.FC<GrievanceMapProps> = ({
  latitude,
  longitude,
  title = 'Incident Location',
  locationAddress,
  className = '',
  height = '200px',
}) => {
  const hasCoords = typeof latitude === 'number' && typeof longitude === 'number' && !isNaN(latitude) && !isNaN(longitude);
  const position: [number, number] = hasCoords ? [latitude!, longitude!] : [12.9716, 77.5946];

  return (
    <div className={`relative rounded-xl overflow-hidden border border-gray-200 ${className}`} style={{ height }}>
      <MapContainer
        center={position}
        zoom={hasCoords ? 15 : 12}
        scrollWheelZoom={false}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Marker position={position} icon={customMarkerIcon}>
          <Popup>
            <div className="text-xs p-1">
              <strong className="text-gray-900 block font-bold">{title}</strong>
              {locationAddress && <span className="text-gray-600 block mt-0.5">{locationAddress}</span>}
              <span className="text-[10px] text-primary font-mono block mt-1">
                {position[0].toFixed(4)}° N, {position[1].toFixed(4)}° E
              </span>
            </div>
          </Popup>
        </Marker>
      </MapContainer>

      {/* Floating GPS coordinates badge */}
      <div className="absolute bottom-2 left-2 right-2 z-[400] bg-white/95 backdrop-blur-xs px-3 py-1.5 rounded-lg shadow-sm flex items-center justify-between border border-gray-100 text-xs pointer-events-none">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-red-500 animate-ping shrink-0" />
          <span className="font-bold text-gray-800 truncate">
            GPS: {position[0].toFixed(4)}° N, {position[1].toFixed(4)}° E
          </span>
        </div>
        <span className="text-[10px] bg-red-50 text-red-700 font-bold px-1.5 py-0.5 rounded shrink-0">
          GIS Tagged
        </span>
      </div>
    </div>
  );
};

