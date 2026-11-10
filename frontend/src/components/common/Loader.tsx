import React from 'react';
import { Loader2 } from 'lucide-react';

interface LoaderProps {
  size?: 'sm' | 'md' | 'lg';
  message?: string;
  className?: string;
}

export const Loader: React.FC<LoaderProps> = ({ size = 'md', message, className = '' }) => {
  const sizeMap = {
    sm: 'w-4 h-4 text-xs',
    md: 'w-7 h-7 text-sm',
    lg: 'w-10 h-10 text-base',
  };

  const iconSizes = {
    sm: 'w-4 h-4',
    md: 'w-7 h-7',
    lg: 'w-10 h-10',
  };

  return (
    <div className={`flex flex-col items-center justify-center gap-2 p-4 text-gray-500 ${className}`}>
      <Loader2 className={`animate-spin text-primary-container ${iconSizes[size]}`} />
      {message && <span className={`font-medium ${sizeMap[size]}`}>{message}</span>}
    </div>
  );
};

