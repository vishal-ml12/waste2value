import React from 'react';

interface EnvironmentalImpactSummaryCardProps {
  collectedKg?: number;
  recycledKg: number;
  co2AvoidedKg?: number | null;
  co2AvoidedMt?: number | null;
  landfillDivertedKg: number;
  disclaimer?: string;
  unconfiguredCategories?: string[];
  showCollected?: boolean;
}

export const EnvironmentalImpactSummaryCard: React.FC<EnvironmentalImpactSummaryCardProps> = ({
  collectedKg,
  recycledKg,
  co2AvoidedKg,
  co2AvoidedMt,
  landfillDivertedKg,
  disclaimer,
  unconfiguredCategories = [],
  showCollected = true,
}) => {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Waste Collected */}
        {showCollected && collectedKg !== undefined && (
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Total Waste Collected
              </span>
              <span className="p-2 bg-blue-50 text-blue-600 rounded-lg text-sm">📦</span>
            </div>
            <div className="text-2xl font-bold text-slate-800">
              {collectedKg.toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 2 })}{' '}
              <span className="text-sm font-normal text-slate-500">kg</span>
            </div>
            <div className="mt-2 text-xs text-slate-500">
              Verified from completed marketplace pickups
            </div>
          </div>
        )}

        {/* Waste Recycled / Processed */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Confirmed Recycled Waste
            </span>
            <span className="p-2 bg-emerald-50 text-emerald-600 rounded-lg text-sm">♻️</span>
          </div>
          <div className="text-2xl font-bold text-emerald-700">
            {recycledKg.toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 2 })}{' '}
            <span className="text-sm font-normal text-slate-500">kg</span>
          </div>
          <div className="mt-2 text-xs text-slate-500">
            Intake confirmed at verified recycling facilities
          </div>
        </div>

        {/* Estimated Avoided CO2 Emissions */}
        <div className="bg-white p-5 rounded-xl border border-emerald-100 shadow-sm relative overflow-hidden bg-gradient-to-br from-emerald-50/50 to-white">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-emerald-800 uppercase tracking-wider">
              Est. Avoided CO₂ Emissions
            </span>
            <span className="p-2 bg-emerald-100 text-emerald-700 rounded-lg text-sm">🌱</span>
          </div>
          <div className="text-2xl font-bold text-emerald-800">
            {co2AvoidedKg !== null && co2AvoidedKg !== undefined ? (
              <>
                {co2AvoidedKg.toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 2 })}{' '}
                <span className="text-sm font-normal text-emerald-600">kg CO₂e</span>
                {co2AvoidedMt && (
                  <div className="text-xs font-medium text-emerald-600 mt-0.5">
                    ({co2AvoidedMt.toFixed(3)} Metric Tons)
                  </div>
                )}
              </>
            ) : (
              <span className="text-sm font-medium text-amber-600 bg-amber-50 px-2 py-1 rounded">
                Unconfigured Factor
              </span>
            )}
          </div>
          <div className="mt-2 text-xs text-emerald-700 flex items-center gap-1">
            <span className="font-semibold">ESTIMATE</span> • US EPA WARM v15
          </div>
        </div>

        {/* Landfill Diversion */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Landfill Diversion
            </span>
            <span className="p-2 bg-teal-50 text-teal-600 rounded-lg text-sm">🌍</span>
          </div>
          <div className="text-2xl font-bold text-teal-700">
            {landfillDivertedKg.toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 2 })}{' '}
            <span className="text-sm font-normal text-slate-500">kg</span>
          </div>
          <div className="mt-2 text-xs text-slate-500">
            Diverted from municipal landfills
          </div>
        </div>
      </div>

      {/* Unconfigured Notice (if any) */}
      {unconfiguredCategories.length > 0 && (
        <div className="bg-amber-50 border border-amber-200 text-amber-800 text-xs px-4 py-2.5 rounded-lg flex items-center gap-2">
          <span>⚠️</span>
          <span>
            <strong>Note:</strong> CO₂ conversion factors for [
            {unconfiguredCategories.join(', ')}] are currently unconfigured to avoid fabricated
            estimates. Recycled weight is included in total kg, but lifecycle carbon savings are
            omitted.
          </span>
        </div>
      )}

      {/* Methodology Disclaimer */}
      {disclaimer && (
        <div className="text-xs text-slate-500 bg-slate-50 border border-slate-200 px-4 py-2 rounded-lg">
          <span className="font-semibold">Environmental Estimation Disclaimer:</span> {disclaimer}
        </div>
      )}
    </div>
  );
};
