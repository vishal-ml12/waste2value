import React, { useState, useEffect } from 'react';
import { analyticsApi } from '../../api/client';
import { MonthlyTrendPoint, CategoryAnalyticsPoint } from '../../types';
import { DateRangeFilter } from '../../components/analytics/DateRangeFilter';
import { EnvironmentalImpactSummaryCard } from '../../components/analytics/EnvironmentalImpactSummaryCard';
import { MonthlyTrendChart } from '../../components/analytics/MonthlyTrendChart';
import { CategoryDistributionTable } from '../../components/analytics/CategoryDistributionTable';
import { EmissionFactorsModal } from '../../components/analytics/EmissionFactorsModal';

export const EnvironmentalDashboardPage: React.FC = () => {
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [trends, setTrends] = useState<MonthlyTrendPoint[]>([]);
  const [categories, setCategories] = useState<CategoryAnalyticsPoint[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isFactorsModalOpen, setIsFactorsModalOpen] = useState<boolean>(false);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [trendsData, categoriesData] = await Promise.all([
        analyticsApi.getMonthlyTrends(startDate, endDate),
        analyticsApi.getCategoryAnalytics(startDate, endDate),
      ]);
      setTrends(trendsData);
      setCategories(categoriesData);
    } catch (err: any) {
      console.error('Failed to load environmental data', err);
      setError(err?.response?.data?.message || 'Failed to load environmental analytics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [startDate, endDate]);

  const handleDateChange = (start: string, end: string) => {
    setStartDate(start);
    setEndDate(end);
  };

  // Compute aggregate totals across categories
  const totalRecycledKg = categories.reduce((sum, c) => sum + (c.recycledKg || 0), 0);
  const totalCollectedKg = categories.reduce((sum, c) => sum + (c.collectedKg || 0), 0);
  const totalLandfillDivertedKg = categories.reduce((sum, c) => sum + (c.landfillDivertedKg || 0), 0);
  
  // Aggregate verified CO2 avoided
  const configuredCategories = categories.filter((c) => c.isFactorConfigured && c.co2AvoidedKg !== null);
  const totalCo2AvoidedKg = configuredCategories.length > 0
    ? configuredCategories.reduce((sum, c) => sum + (c.co2AvoidedKg || 0), 0)
    : null;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-emerald-800 via-teal-800 to-slate-900 rounded-2xl p-8 sm:p-10 text-white shadow-lg relative overflow-hidden">
        <div className="relative z-10 max-w-3xl">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-200 text-xs font-semibold mb-4 border border-emerald-400/30">
            <span>🌱</span>
            <span>Audited Transparency & Environmental Accounting</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
            Environmental Impact & Circular Analytics
          </h1>
          <p className="mt-3 text-emerald-100/90 text-sm sm:text-base leading-relaxed">
            Real-time tracking of diverted waste, certified recycling throughput, and avoided greenhouse gas emissions calculated using US EPA WARM v15 and UK DEFRA standards.
          </p>
          <div className="mt-6 flex flex-wrap items-center gap-3">
            <button
              onClick={() => setIsFactorsModalOpen(true)}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-lg shadow transition flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              View Emission Factors & Methodology
            </button>
            <div className="text-xs text-emerald-200/80">
              Authoritative Source: Verified Processor Receipts
            </div>
          </div>
        </div>
      </div>

      {/* Date Filter & Control Bar */}
      <DateRangeFilter
        startDate={startDate}
        endDate={endDate}
        onChange={handleDateChange}
      />

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700 flex items-center justify-between">
          <span>{error}</span>
          <button
            onClick={fetchData}
            className="text-xs font-semibold underline text-red-800 hover:text-red-900"
          >
            Retry
          </button>
        </div>
      )}

      {loading ? (
        <div className="py-20 flex flex-col items-center justify-center text-slate-400">
          <svg className="animate-spin h-8 w-8 text-emerald-600 mb-3" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
          </svg>
          <span className="text-sm font-medium">Aggregating environmental impact metrics...</span>
        </div>
      ) : (
        <>
          {/* Main KPI Impact Summary */}
          <EnvironmentalImpactSummaryCard
            totalRecycledKg={totalRecycledKg}
            totalCollectedKg={totalCollectedKg}
            totalLandfillDivertedKg={totalLandfillDivertedKg}
            totalCo2AvoidedKg={totalCo2AvoidedKg}
          />

          {/* Monthly Trend Chart */}
          <MonthlyTrendChart
            data={trends}
            title="Monthly Waste Collection & Recycling Trajectory"
            description="Comparing completed collection transactions against confirmed processor intake receipts across months."
          />

          {/* Category Breakdown Table */}
          <CategoryDistributionTable
            data={categories}
            title="Material-Specific Recycling & Emissions Offsets"
            description="Detailed breakdown of recovered materials, landfill diversion rates, and verified carbon offsets by stream."
            showCollected={true}
            showRecycled={true}
          />

          {/* Methodology Card */}
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-6 text-slate-600 text-xs leading-relaxed space-y-3">
            <h4 className="text-sm font-bold text-slate-800 flex items-center gap-2">
              <span className="text-emerald-600">ℹ️</span>
              Environmental Accounting Principles & Regulatory Compliance
            </h4>
            <p>
              1. <strong>Strict Segregation of Collection vs. Recycling:</strong> Collected volumes derive exclusively from completed marketplace transactions between Generators and Collectors. Recycled volumes and landfill diversion derive solely from verified Processor Receipts issued upon physical facility intake and qualification.
            </p>
            <p>
              2. <strong>Accredited Lifecycle Carbon Offsets:</strong> CO₂ equivalents are calculated using peer-reviewed factors from the US EPA Waste Reduction Model (WARM v15) and UK Department for Environment, Food & Rural Affairs (DEFRA). Offsets capture raw material conservation, lower processing energy, and landfill methane prevention.
            </p>
            <p>
              3. <strong>Anti-Fabrication Policy:</strong> Where regulatory emission factors have not yet been ratified for specialized or composite waste types (e.g., heterogeneous E-Waste), calculations transparently display &quot;Factor Pending&quot; rather than synthetic approximations.
            </p>
          </div>
        </>
      )}

      {/* Emission Factors Reference Modal */}
      <EmissionFactorsModal
        isOpen={isFactorsModalOpen}
        onClose={() => setIsFactorsModalOpen(false)}
        isAdmin={false}
      />
    </div>
  );
};
