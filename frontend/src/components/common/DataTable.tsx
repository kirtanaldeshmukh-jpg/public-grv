import React from 'react';
import { ChevronLeft, ChevronRight, ArrowUpDown } from 'lucide-react';
import { Loader } from './Loader';
import { EmptyState } from './EmptyState';

export interface Column<T> {
  key: string;
  header: string;
  render?: (row: T, index: number) => React.ReactNode;
  sortable?: boolean;
  align?: 'left' | 'center' | 'right';
  className?: string;
  width?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  isLoading?: boolean;
  keyExtractor: (item: T, index: number) => string | number;
  emptyTitle?: string;
  emptyDescription?: string;
  onRowClick?: (item: T) => void;
  // Optional pagination
  page?: number;
  pageSize?: number;
  totalPages?: number;
  totalElements?: number;
  onPageChange?: (newPage: number) => void;
  className?: string;
}

export function DataTable<T>({
  columns,
  data,
  isLoading = false,
  keyExtractor,
  emptyTitle = 'No records found',
  emptyDescription = 'There are no items to display matching the criteria.',
  onRowClick,
  page,
  pageSize,
  totalPages,
  totalElements,
  onPageChange,
  className = '',
}: DataTableProps<T>) {
  if (isLoading) {
    return (
      <div className="bg-white rounded-xl border border-gray-200/80 p-12 flex items-center justify-center shadow-xs">
        <Loader size="md" message="Loading records..." />
      </div>
    );
  }

  if (!data || data.length === 0) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }

  return (
    <div className={`bg-white rounded-xl border border-gray-200/80 shadow-xs overflow-hidden flex flex-col ${className}`}>
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-surface-low border-b border-gray-200/80 text-[11px] font-semibold text-gray-500 uppercase tracking-wider">
              {columns.map((col) => (
                <th
                  key={col.key}
                  style={col.width ? { width: col.width } : undefined}
                  className={`py-3 px-4 ${
                    col.align === 'center'
                      ? 'text-center'
                      : col.align === 'right'
                      ? 'text-right'
                      : 'text-left'
                  } ${col.className || ''}`}
                >
                  <div className={`inline-flex items-center gap-1 ${col.align === 'right' ? 'justify-end' : ''}`}>
                    <span>{col.header}</span>
                    {col.sortable && <ArrowUpDown className="w-3 h-3 text-gray-400" />}
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 text-xs text-gray-700">
            {data.map((item, index) => (
              <tr
                key={keyExtractor(item, index)}
                onClick={() => onRowClick?.(item)}
                className={`transition-colors hover:bg-indigo-50/20 ${
                  onRowClick ? 'cursor-pointer' : ''
                }`}
              >
                {columns.map((col) => (
                  <td
                    key={col.key}
                    className={`py-3.5 px-4 align-middle ${
                      col.align === 'center'
                        ? 'text-center'
                        : col.align === 'right'
                        ? 'text-right'
                        : 'text-left'
                    } ${col.className || ''}`}
                  >
                    {col.render
                      ? col.render(item, index)
                      : String((item as any)[col.key] ?? '—')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      {totalPages !== undefined && totalPages > 1 && onPageChange && (
        <div className="px-4 py-3 border-t border-gray-100 bg-surface-low/50 flex flex-wrap items-center justify-between gap-3 text-xs text-gray-500">
          <div>
            Showing{' '}
            <span className="font-semibold text-gray-800">
              {((page ?? 0) * (pageSize ?? 10)) + 1}
            </span>{' '}
            to{' '}
            <span className="font-semibold text-gray-800">
              {Math.min(((page ?? 0) + 1) * (pageSize ?? 10), totalElements ?? data.length)}
            </span>{' '}
            of{' '}
            <span className="font-semibold text-gray-800">{totalElements ?? data.length}</span>{' '}
            entries
          </div>

          <div className="flex items-center gap-1.5 ml-auto">
            <button
              onClick={() => onPageChange((page ?? 0) - 1)}
              disabled={(page ?? 0) <= 0}
              className="p-1.5 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-2.5 py-1 rounded-lg bg-primary-container text-white text-[11px] font-semibold">
              {(page ?? 0) + 1}
            </span>
            <span className="text-gray-400">/ {totalPages}</span>
            <button
              onClick={() => onPageChange((page ?? 0) + 1)}
              disabled={(page ?? 0) >= totalPages - 1}
              className="p-1.5 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

