import React, { useState } from 'react';
import { MonthlyTrendPoint } from '../../types';

interface MonthlyTrendChartProps {
  data: MonthlyTrendPoint[];
  title?: string;
  description?: string;
}

export const MonthlyTrendChart: React.FC<MonthlyTrendChartProps> = ({
  data,
  title = 'Monthly Collection vs. Recycling Trends',
  description = 'Authoritative quantities comparing completed collections (transactions) with confirmed recycling (processor receipts).',
}) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  if (!data || data.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
        <h3 className="text-base font-semibold text-slate-800">{title}</h3>
        <p className="text-xs text-slate-500 mt-1">{description}</p>
        <div className="flex flex-col items-center justify-center py-12 text-center text-slate-400">
          <svg className="w-12 h-12 mb-3 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
          </svg>
          <p className="text-sm font-medium text-slate-600">No monthly trend data available</p>
          <p className="text-xs text-slate-400 max-w-xs mt-1">Complete transactions and confirmed processor receipts will populate this chart.</p>
        </div>
      </div>
    );
  }

  // Find max value for scaling
  const maxVal = Math.max(
    ...data.map((d) => Math.max(d.collectedKg || 0, d.recycledKg || 0)),
    100
  );

  const formatMonth = (monthStr: string) => {
    try {
      const [year, month] = monthStr.split('-');
      const date = new Date(parseInt(year, 10), parseInt(month, 10) - 1, 1);
      return date.toLocaleDateString(undefined, { month: 'short', year: '2-digit' });
    } catch {
      return monthStr;
    }
  };

  const totalCollected = data.reduce((acc, curr) => acc + (curr.collectedKg || 0), 0);
  const totalRecycled = data.reduce((acc, curr) => acc + (curr.recycledKg || 0), 0);
  const totalCo2Avoided = data.reduce((acc, curr) => acc + (curr.co2AvoidedKg || 0), 0);

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-5 border-b border-slate-100">
        <div>
          <h3 className="text-base font-semibold text-slate-800">{title}</h3>
          <p className="text-xs text-slate-500 mt-1">{description}</p>
        </div>
        <div className="flex items-center gap-4 text-xs">
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-sm bg-blue-500 inline-block"></span>
            <span className="text-slate-600 font-medium">Collected (kg)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-sm bg-emerald-500 inline-block"></span>
            <span className="text-slate-600 font-medium">Recycled (kg)</span>
          </div>
        </div>
      </div>

      {/* Summary KPI Strip */}
      <div className="grid grid-cols-3 gap-3 my-4 p-3 bg-slate-50 rounded-lg text-center">
        <div>
          <div className="text-xs text-slate-500 font-medium">Period Collected</div>
          <div className="text-sm font-bold text-blue-700">{totalCollected.toLocaleString()} kg</div>
        </div>
        <div>
          <div className="text-xs text-slate-500 font-medium">Period Recycled</div>
          <div className="text-sm font-bold text-emerald-700">{totalRecycled.toLocaleString()} kg</div>
        </div>
        <div>
          <div className="text-xs text-slate-500 font-medium">Est. CO₂ Avoided</div>
          <div className="text-sm font-bold text-teal-700">
            {totalCo2Avoided > 0 ? `${(totalCo2Avoided / 1000).toFixed(2)} MT` : 'N/A'}
          </div>
        </div>
      </div>

      {/* Bar Chart Container */}
      <div className="mt-6">
        <div className="h-64 flex items-end gap-2 sm:gap-4 pt-6 pb-2 border-b border-slate-200">
          {data.map((point, index) => {
            const collectedHeight = Math.min(100, Math.max(4, ((point.collectedKg || 0) / maxVal) * 100));
            const recycledHeight = Math.min(100, Math.max(4, ((point.recycledKg || 0) / maxVal) * 100));
            const isHovered = hoveredIndex === index;

            return (
              <div
                key={point.month}
                className="flex-1 flex flex-col items-center h-full justify-end relative group cursor-pointer"
                onMouseEnter={() => setHoveredIndex(index)}
                onMouseLeave={() => setHoveredIndex(null)}
              >
                {/* Tooltip */}
                {isHovered && (
                  <div className="absolute -top-24 z-20 bg-slate-900 text-white text-xs rounded-lg p-2.5 shadow-xl pointer-events-none whitespace-nowrap min-w-[150px]">
                    <div className="font-semibold text-slate-200 border-b border-slate-700 pb-1 mb-1.5">
                      {formatMonth(point.month)}
                    </div>
                    <div className="flex justify-between gap-2 text-blue-300">
                      <span>Collected:</span>
                      <span className="font-bold">{point.collectedKg.toLocaleString()} kg</span>
                    </div>
                    <div className="flex justify-between gap-2 text-emerald-300">
                      <span>Recycled:</span>
                      <span className="font-bold">{point.recycledKg.toLocaleString()} kg</span>
                    </div>
                    {point.co2AvoidedKg !== undefined && point.co2AvoidedKg !== null && (
                      <div className="flex justify-between gap-2 text-teal-300">
                        <span>CO₂ Avoided:</span>
                        <span className="font-bold">{point.co2AvoidedKg.toLocaleString()} kg</span>
                      </div>
                    )}
                  </div>
                )}

                {/* Bars side by side */}
                <div className="w-full flex items-end justify-center gap-1 h-full">
                  <div
                    style={{ height: `${collectedHeight}%` }}
                    className={`w-1/2 max-w-[20px] rounded-t transition-all duration-300 ${
                      isHovered ? 'bg-blue-600' : 'bg-blue-500'
                    }`}
                  />
                  <div
                    style={{ height: `${recycledHeight}%` }}
                    className={`w-1/2 max-w-[20px] rounded-t transition-all duration-300 ${
                      isHovered ? 'bg-emerald-600' : 'bg-emerald-500'
                    }`}
                  />
                </div>
              </div>
            );
          })}
        </div>

        {/* X-Axis Month Labels */}
        <div className="flex items-center gap-2 sm:gap-4 mt-2">
          {data.map((point) => (
            <div
              key={point.month}
              className="flex-1 text-center text-[10px] sm:text-xs font-medium text-slate-500 truncate"
            >
              {formatMonth(point.month)}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
