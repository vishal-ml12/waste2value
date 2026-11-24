import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/common/Navbar';
import { Footer } from './components/common/Footer';
import { ProtectedRoute } from './components/common/ProtectedRoute';

// Public pages
import { LandingPage } from './pages/public/LandingPage';
import { BrowseWastePage } from './pages/public/BrowseWastePage';
import { ListingDetailPage } from './pages/public/ListingDetailPage';
import { LoginPage } from './pages/public/LoginPage';
import { RegisterPage } from './pages/public/RegisterPage';
import { EnvironmentalDashboardPage } from './pages/public/EnvironmentalDashboardPage';

// Generator pages
import { GeneratorDashboardPage } from './pages/generator/GeneratorDashboardPage';
import { MyListingsPage } from './pages/generator/MyListingsPage';
import { CreateListingPage } from './pages/generator/CreateListingPage';
import { EditListingPage } from './pages/generator/EditListingPage';
import { AddressesPage } from './pages/generator/AddressesPage';
import { ProfilePage } from './pages/generator/ProfilePage';
import { GeneratorPickupRequestsPage } from './pages/generator/GeneratorPickupRequestsPage';

// Collector pages
import { CollectorDashboardPage } from './pages/collector/CollectorDashboardPage';
import { CollectorProfilePage } from './pages/collector/CollectorProfilePage';
import { CollectorPickupRequestsPage } from './pages/collector/CollectorPickupRequestsPage';
import { CollectorDeliveriesPage } from './pages/collector/CollectorDeliveriesPage';

// Processor pages
import { ProcessorDashboardPage } from './pages/processor/ProcessorDashboardPage';
import { ProcessorProfilePage } from './pages/processor/ProcessorProfilePage';
import { ProcessorCapabilitiesPage } from './pages/processor/ProcessorCapabilitiesPage';
import { ProcessorReceiptsPage } from './pages/processor/ProcessorReceiptsPage';

// Admin pages
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminVerificationsPage } from './pages/admin/AdminVerificationsPage';
import { AdminCategoriesPage } from './pages/admin/AdminCategoriesPage';
import { AdminUsersPage } from './pages/admin/AdminUsersPage';
import { AdminPickupRequestsPage } from './pages/admin/AdminPickupRequestsPage';
import { AdminTransactionsPage } from './pages/admin/AdminTransactionsPage';
import { AdminReportsPage } from './pages/admin/AdminReportsPage';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="min-h-screen flex flex-col bg-slate-50">
          <Navbar />
          <main className="flex-1">
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<LandingPage />} />
              <Route path="/browse" element={<BrowseWastePage />} />
              <Route path="/environmental-impact" element={<EnvironmentalDashboardPage />} />
              <Route path="/listings/:id" element={<ListingDetailPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <ProfilePage />
                  </ProtectedRoute>
                }
              />

              {/* Generator Protected Routes */}
              <Route
                path="/generator/dashboard"
                element={
                  <ProtectedRoute allowedRoles={['GENERATOR']}>
                    <GeneratorDashboardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/generator/listings"
                element={
                  <ProtectedRoute allowedRoles={['GENERATOR']}>
                    <MyListingsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/generator/listings/new"
                element={
                  <ProtectedRoute allowedRoles={['GENERATOR']}>
                    <CreateListingPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/generator/listings/:id/edit"
                element={
                  <ProtectedRoute allowedRoles={['GENERATOR']}>
                    <EditListingPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/generator/requests"
                element={
                  <ProtectedRoute allowedRoles={['GENERATOR']}>
                    <GeneratorPickupRequestsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/generator/addresses"
                element={
                  <ProtectedRoute allowedRoles={['GENERATOR']}>
                    <AddressesPage />
                  </ProtectedRoute>
                }
              />

              {/* Collector Protected Routes */}
              <Route
                path="/collector/dashboard"
                element={
                  <ProtectedRoute allowedRoles={['COLLECTOR']}>
                    <CollectorDashboardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/collector/profile"
                element={
                  <ProtectedRoute allowedRoles={['COLLECTOR']}>
                    <CollectorProfilePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/collector/requests"
                element={
                  <ProtectedRoute allowedRoles={['COLLECTOR']}>
                    <CollectorPickupRequestsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/collector/deliveries"
                element={
                  <ProtectedRoute allowedRoles={['COLLECTOR']}>
                    <CollectorDeliveriesPage />
                  </ProtectedRoute>
                }
              />

              {/* Processor Protected Routes */}
              <Route
                path="/processor/dashboard"
                element={
                  <ProtectedRoute allowedRoles={['PROCESSOR']}>
                    <ProcessorDashboardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/processor/profile"
                element={
                  <ProtectedRoute allowedRoles={['PROCESSOR']}>
                    <ProcessorProfilePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/processor/capabilities"
                element={
                  <ProtectedRoute allowedRoles={['PROCESSOR']}>
                    <ProcessorCapabilitiesPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/processor/receipts"
                element={
                  <ProtectedRoute allowedRoles={['PROCESSOR']}>
                    <ProcessorReceiptsPage />
                  </ProtectedRoute>
                }
              />

              {/* Admin Protected Routes */}
              <Route
                path="/admin/dashboard"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminDashboardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/verifications"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminVerificationsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/categories"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminCategoriesPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/users"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminUsersPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/pickups"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminPickupRequestsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/transactions"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminTransactionsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/reports"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <AdminReportsPage />
                  </ProtectedRoute>
                }
              />

              {/* Fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
          <Footer />
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
