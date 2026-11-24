import React from 'react';

interface DateRangeFilterProps {
  startDate: string;
  endDate: string;
  onChange: (start: string, end: string) => void;
}

export const DateRangeFilter: React.FC<DateRangeFilterProps> = ({
  startDate,
  endDate,
  onChange,
}) => {
  const setPreset = (days: number | null) => {
    if (days === null) {
      onChange('', '');
      return;
    }
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days);
    onChange(start.toISOString().split('T')[0], end.toISOString().split('T')[0]);
  };

  const isAllTime = !startDate && !endDate;

  return (
    <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-4">
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 mr-2">
          Time Range:
        </span>
        <button
          type="button"
          onClick={() => setPreset(null)}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
            isAllTime
              ? 'bg-emerald-600 text-white'
              : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
          }`}
        >
          All Time
        </button>
        <button
          type="button"
          onClick={() => setPreset(30)}
          className="px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-100 text-slate-600 hover:bg-slate-200 transition"
        >
          Last 30 Days
        </button>
        <button
          type="button"
          onClick={() => setPreset(90)}
          className="px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-100 text-slate-600 hover:bg-slate-200 transition"
        >
          Last 90 Days
        </button>
        <button
          type="button"
          onClick={() => setPreset(365)}
          className="px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-100 text-slate-600 hover:bg-slate-200 transition"
        >
          Past Year
        </button>
      </div>

      <div className="flex items-center gap-2 text-xs">
        <label className="text-slate-500">From:</label>
        <input
          type="date"
          value={startDate}
          onChange={(e) => onChange(e.target.value, endDate)}
          className="px-2.5 py-1.5 border border-slate-200 rounded-lg text-slate-700 bg-white focus:outline-none focus:ring-1 focus:ring-emerald-500"
        />
        <label className="text-slate-500">To:</label>
        <input
          type="date"
          value={endDate}
          onChange={(e) => onChange(startDate, e.target.value)}
          className="px-2.5 py-1.5 border border-slate-200 rounded-lg text-slate-700 bg-white focus:outline-none focus:ring-1 focus:ring-emerald-500"
        />
        {(startDate || endDate) && (
          <button
            type="button"
            onClick={() => onChange('', '')}
            className="text-xs text-rose-600 hover:underline ml-1"
          >
            Clear
          </button>
        )}
      </div>
    </div>
  );
};
