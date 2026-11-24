import React from 'react';
import { CategoryAnalyticsPoint } from '../../types';

interface CategoryDistributionTableProps {
  data: CategoryAnalyticsPoint[];
  title?: string;
  description?: string;
  showCollected?: boolean;
  showRecycled?: boolean;
}

export const CategoryDistributionTable: React.FC<CategoryDistributionTableProps> = ({
  data,
  title = 'Category Breakdown & Environmental Impact',
  description = 'Material distribution across waste streams, landfill diversion rates, and verified emission factors.',
  showCollected = true,
  showRecycled = true,
}) => {
  if (!data || data.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
        <h3 className="text-base font-semibold text-slate-800">{title}</h3>
        <p className="text-xs text-slate-500 mt-1">{description}</p>
        <div className="text-center py-8 text-slate-400 text-sm">
          No category distribution records found for this period.
        </div>
      </div>
    );
  }

  // Calculate totals for percentage
  const totalCollected = data.reduce((acc, c) => acc + (c.collectedKg || 0), 0);
  const totalRecycled = data.reduce((acc, c) => acc + (c.recycledKg || 0), 0);
  const maxVolume = Math.max(
    ...data.map((c) => Math.max(c.collectedKg || 0, c.recycledKg || 0)),
    1
  );

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="p-6 border-b border-slate-100">
        <h3 className="text-base font-semibold text-slate-800">{title}</h3>
        <p className="text-xs text-slate-500 mt-1">{description}</p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-600 text-xs uppercase tracking-wider font-semibold border-b border-slate-200">
            <tr>
              <th className="px-6 py-3.5">Category</th>
              {showCollected && <th className="px-6 py-3.5 text-right">Collected (kg)</th>}
              {showRecycled && <th className="px-6 py-3.5 text-right">Recycled (kg)</th>}
              <th className="px-6 py-3.5">Volume Share</th>
              <th className="px-6 py-3.5 text-right">Est. CO₂ Avoided</th>
              <th className="px-6 py-3.5">Emission Factor Source</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {data.map((cat) => {
              const relevantKg = showRecycled ? cat.recycledKg : cat.collectedKg;
              const totalRelevant = showRecycled ? totalRecycled : totalCollected;
              const sharePercent = totalRelevant > 0 ? ((relevantKg / totalRelevant) * 100).toFixed(1) : '0.0';
              const progressWidth = Math.min(100, Math.max(5, (relevantKg / maxVolume) * 100));

              return (
                <tr key={cat.categoryId} className="hover:bg-slate-50 transition">
                  <td className="px-6 py-4">
                    <div className="font-semibold text-slate-800">{cat.categoryName}</div>
                    <div className="text-xs text-slate-400 font-mono">{cat.categorySlug}</div>
                  </td>
                  {showCollected && (
                    <td className="px-6 py-4 text-right font-medium text-slate-700">
                      {cat.collectedKg.toLocaleString()} <span className="text-xs text-slate-400">kg</span>
                    </td>
                  )}
                  {showRecycled && (
                    <td className="px-6 py-4 text-right font-semibold text-emerald-700">
                      {cat.recycledKg.toLocaleString()} <span className="text-xs text-emerald-500">kg</span>
                    </td>
                  )}
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="flex-1 bg-slate-100 rounded-full h-2 max-w-[120px] overflow-hidden">
                        <div
                          style={{ width: `${progressWidth}%` }}
                          className={`h-full rounded-full ${
                            showRecycled ? 'bg-emerald-500' : 'bg-blue-500'
                          }`}
                        />
                      </div>
                      <span className="text-xs text-slate-500 font-medium">{sharePercent}%</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-right">
                    {cat.isFactorConfigured && cat.co2AvoidedKg !== null && cat.co2AvoidedKg !== undefined ? (
                      <div>
                        <div className="font-bold text-teal-700">
                          {cat.co2AvoidedKg >= 1000
                            ? `${(cat.co2AvoidedKg / 1000).toFixed(2)} MT`
                            : `${cat.co2AvoidedKg.toLocaleString()} kg`}
                        </div>
                        <div className="text-[11px] text-slate-400">
                          Factor: {cat.co2Factor?.toFixed(2)} kg CO₂e/kg
                        </div>
                      </div>
                    ) : (
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-amber-50 text-amber-700 border border-amber-200">
                        Factor Pending
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4">
                    {cat.isFactorConfigured ? (
                      <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
                        {cat.sourceReference || 'US EPA WARM v15'}
                      </span>
                    ) : (
                      <span className="text-xs text-slate-400 italic">
                        Unconfigured in database
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};
