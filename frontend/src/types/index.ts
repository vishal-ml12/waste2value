export type Role = 'GENERATOR' | 'COLLECTOR' | 'PROCESSOR' | 'ADMIN';

export type AccountStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING';

export interface UserSummary {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  status: AccountStatus;
}

export interface UserProfile extends UserSummary {
  phone?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface Address {
  id: number;
  userId: number;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  latitude: number;
  longitude: number;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AddressRequest {
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  latitude: number;
  longitude: number;
  isDefault?: boolean;
}

export interface Category {
  id: number;
  parentId?: number | null;
  parentName?: string | null;
  name: string;
  slug: string;
  description?: string;
  unit: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CategoryTree {
  id: number;
  name: string;
  slug: string;
  description?: string;
  unit: string;
  active: boolean;
  subCategories: CategoryTree[];
}

export type ListingStatus = 'DRAFT' | 'POSTED' | 'ALLOCATED' | 'COMPLETED' | 'CANCELLED';

export interface ListingImage {
  id: number;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  contentType: string;
  createdAt: string;
}

export interface ListingSummary {
  id: number;
  generatorId: number;
  generatorName: string;
  categoryId: number;
  categoryName: string;
  title: string;
  quantity: number;
  unit: string;
  expectedPrice: number;
  city: string;
  state: string;
  status: ListingStatus;
  primaryImageUrl?: string | null;
  distanceKm?: number | null;
  createdAt: string;
}

export interface ListingDetail {
  id: number;
  generatorId: number;
  generatorName: string;
  generatorEmail?: string;
  generatorPhone?: string;
  categoryId: number;
  categoryName: string;
  categorySlug?: string;
  parentCategoryName?: string | null;
  addressId: number;
  addressLine1: string;
  addressLine2?: string | null;
  city: string;
  state: string;
  postalCode: string;
  latitude: number;
  longitude: number;
  title: string;
  description: string;
  quantity: number;
  unit: string;
  expectedPrice: number;
  status: ListingStatus;
  version: number;
  images: ListingImage[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateListingRequest {
  categoryId: number;
  addressId: number;
  title: string;
  description: string;
  quantity: number;
  unit: string;
  expectedPrice?: number;
}

export interface UpdateListingRequest {
  categoryId: number;
  addressId: number;
  title: string;
  description: string;
  quantity: number;
  unit: string;
  expectedPrice?: number;
}

export interface GeneratorDashboardStats {
  activeListingsCount: number;
  draftListingsCount: number;
  completedListingsCount: number;
  cancelledListingsCount: number;
  totalListingsCount: number;
  totalQuantityListed: number;
  recentListings: ListingSummary[];
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export interface ListingFilterParams {
  keyword?: string;
  categoryId?: number;
  city?: string;
  state?: string;
  minQuantity?: number;
  maxQuantity?: number;
  minPrice?: number;
  maxPrice?: number;
  latitude?: number;
  longitude?: number;
  maxDistanceKm?: number;
  sortBy?: string;
  page?: number;
  size?: number;
}

// Phase 2 Types
export interface CollectorProfile {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  vehicleType: string;
  serviceArea: string;
  maxDistanceKm: number;
  isVerified: boolean;
  avgRating: number;
  totalPickups: number;
  verifiedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CollectorProfileRequest {
  vehicleType: string;
  serviceArea: string;
  maxDistanceKm: number;
}

export interface ProcessorProfile {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  companyName: string;
  licenseNumber: string;
  description?: string;
  maxCapacityKg: number;
  currentLoadKg: number;
  availableCapacityKg: number;
  isVerified: boolean;
  avgRating: number;
  verifiedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProcessorProfileRequest {
  companyName: string;
  licenseNumber: string;
  description?: string;
  maxCapacityKg: number;
}

export interface ProcessorCapability {
  id: number;
  processorId: number;
  companyName: string;
  categoryId: number;
  categoryName: string;
  unit: string;
  minQuantity: number;
  maxQuantity: number;
  pricePerUnit: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProcessorCapabilityRequest {
  categoryId: number;
  minQuantity: number;
  maxQuantity: number;
  pricePerUnit: number;
  isActive?: boolean;
}

export type PickupRequestStatus = 'REQUESTED' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';

export interface PickupRequest {
  id: number;
  listingId: number;
  listingTitle: string;
  categoryName: string;
  listingQuantity: number;
  unit: string;
  expectedPrice: number;
  generatorId: number;
  generatorName: string;
  generatorEmail?: string;
  generatorPhone?: string;
  pickupAddress: string;
  pickupCity: string;
  pickupState: string;
  collectorId: number;
  collectorName: string;
  collectorEmail?: string;
  collectorPhone?: string;
  status: PickupRequestStatus;
  proposedPrice: number;
  note?: string;
  actualQuantity?: number;
  completionNote?: string;
  requestedAt: string;
  respondedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePickupRequest {
  proposedPrice: number;
  note?: string;
}

export interface CompletePickupRequest {
  actualQuantity: number;
  completionNote?: string;
}

export type TransactionStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface Transaction {
  id: number;
  listingId: number;
  listingTitle: string;
  pickupRequestId: number;
  generatorId: number;
  generatorName: string;
  collectorId: number;
  collectorName: string;
  processorId?: number;
  processorCompanyName?: string;
  quantity: number;
  unit: string;
  agreedPrice: number;
  totalAmount: number;
  status: TransactionStatus;
  transactionDate: string;
  createdAt: string;
}

export type ProcessorReceiptStatus = 'RECEIVED' | 'VERIFIED' | 'REJECTED';

export interface ProcessorReceipt {
  id: number;
  processorId: number;
  companyName: string;
  collectorId: number;
  collectorName: string;
  categoryId: number;
  categoryName: string;
  listingId?: number;
  pickupRequestId?: number;
  transactionId?: number;
  generatorId?: number;
  generatorName?: string;
  quantity: number;
  unit: string;
  agreedPrice: number;
  totalAmount: number;
  status: ProcessorReceiptStatus;
  notes?: string;
  receivedDate: string;
  createdAt: string;
}

export interface CreateProcessorReceiptRequest {
  collectorId: number;
  categoryId: number;
  listingId?: number;
  pickupRequestId?: number;
  transactionId?: number;
  generatorId?: number;
  generatorName?: string;
  quantity: number;
  unit: string;
  agreedPrice: number;
  notes?: string;
}

export interface Review {
  id: number;
  reviewerId: number;
  reviewerName: string;
  targetUserId: number;
  targetUserName: string;
  pickupRequestId: number;
  rating: number;
  comment?: string;
  createdAt: string;
}

export interface CreateReviewRequest {
  pickupRequestId: number;
  targetUserId: number;
  rating: number;
  comment?: string;
}

export interface ReviewSummary {
  targetUserId: number;
  avgRating: number;
  totalReviews: number;
  reviews: PageResponse<Review>;
}

export type NotificationType =
  | 'PICKUP_REQUESTED'
  | 'PICKUP_ACCEPTED'
  | 'PICKUP_REJECTED'
  | 'PICKUP_CANCELLED'
  | 'PICKUP_COMPLETED'
  | 'LISTING_ALLOCATED'
  | 'PROCESSOR_RECEIPT'
  | 'PROFILE_VERIFIED'
  | 'PROFILE_REJECTED'
  | 'TRANSACTION_CREATED'
  | 'REVIEW_RECEIVED'
  | 'REPORT_UPDATE'
  | 'SYSTEM';

export interface NotificationItem {
  id: number;
  userId: number;
  type: NotificationType;
  title: string;
  message: string;
  relatedEntityType?: string;
  relatedEntityId?: number;
  readAt?: string;
  isRead: boolean;
  createdAt: string;
}

export type ReportTargetType = 'LISTING' | 'PICKUP' | 'TRANSACTION' | 'USER';
export type ReportStatus = 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED' | 'DISMISSED';

export interface ReportItem {
  id: number;
  reporterId: number;
  reporterName: string;
  targetType: ReportTargetType;
  targetId: number;
  reason: string;
  description: string;
  status: ReportStatus;
  adminNote?: string;
  resolvedByUserId?: number;
  resolvedByUserName?: string;
  resolvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CollectorDashboardStats {
  isProfileComplete: boolean;
  isVerified: boolean;
  availableListingsCount: number;
  activeRequestsCount: number;
  acceptedPickupsCount: number;
  completedPickupsCount: number;
  totalQuantityCollected: number;
  totalTransactionValue: number;
  avgRating: number;
  recentRequests: PickupRequest[];
}

export interface ProcessorDashboardStats {
  isProfileComplete: boolean;
  isVerified: boolean;
  maxCapacityKg: number;
  currentLoadKg: number;
  availableCapacityKg: number;
  activeCapabilitiesCount: number;
  totalReceiptsCount: number;
  avgRating: number;
  recentReceipts: ProcessorReceipt[];
}

export interface AdminDashboardStats {
  pendingCollectorsCount: number;
  pendingProcessorsCount: number;
  totalUsersCount: number;
  activeListingsCount: number;
  activePickupRequestsCount: number;
  totalTransactionsCount: number;
  openReportsCount: number;
}

export interface CollectorRecommendation {
  collectorId: number;
  userId: number;
  collectorName: string;
  vehicleType: string;
  serviceArea: string;
  maxDistanceKm: number;
  isVerified: boolean;
  avgRating: number;
  totalPickups: number;
  distanceKm?: number;
  isDistanceAvailable: boolean;
  suitabilityScore: number;
  matchReasons: string[];
}

export interface ProcessorRecommendation {
  processorId: number;
  userId: number;
  companyName: string;
  licenseNumber: string;
  isVerified: boolean;
  avgRating: number;
  maxCapacityKg: number;
  currentLoadKg: number;
  availableCapacityKg: number;
  categoryName: string;
  minQuantity: number;
  maxQuantity: number;
  pricePerUnit: number;
  distanceKm?: number;
  isDistanceAvailable: boolean;
  suitabilityScore: number;
  matchReasons: string[];
}

export interface ListingRecommendation {
  listingId: number;
  title: string;
  categoryId: number;
  categoryName: string;
  categorySlug: string;
  quantity: number;
  unit: string;
  expectedPrice: number;
  generatorId: number;
  generatorName: string;
  city: string;
  state: string;
  distanceKm?: number;
  isDistanceAvailable: boolean;
  suitabilityScore: number;
  matchReasons: string[];
  createdAt: string;
}

// Phase 3B: Environmental Impact & Analytics Types
export interface MonthlyTrendPoint {
  month: string;
  collectedKg: number;
  recycledKg: number;
  co2AvoidedKg: number;
  landfillDivertedKg: number;
  transactionsCount: number;
  receiptsCount: number;
}

export interface CategoryAnalyticsPoint {
  categoryId: number;
  categoryName: string;
  categorySlug: string;
  collectedKg: number;
  recycledKg: number;
  co2Factor?: number | null;
  co2AvoidedKg?: number | null;
  landfillDivertedKg: number;
  isFactorConfigured: boolean;
  sourceReference: string;
}

export interface ProcessorAnalyticsPoint {
  processorId: number;
  companyName: string;
  licenseNumber: string;
  recycledKg: number;
  receiptsCount: number;
  co2AvoidedKg?: number | null;
  landfillDivertedKg: number;
}

export interface CategoryImpact {
  categoryId: number;
  categoryName: string;
  quantityProcessedKg: number;
  co2FactorKgPerUnit?: number | null;
  landfillDiversionRate: number;
  co2AvoidedKg?: number | null;
  landfillDivertedKg: number;
  sourceReference: string;
  isFactorConfigured: boolean;
}

export interface AdminOverviewStats {
  totalWasteCollectedKg: number;
  totalWasteRecycledKg: number;
  totalCo2AvoidedKg?: number | null;
  totalCo2AvoidedMt?: number | null;
  totalLandfillDivertedKg: number;
  completedTransactionsCount: number;
  confirmedReceiptsCount: number;
  activeGeneratorsCount: number;
  activeCollectorsCount: number;
  activeProcessorsCount: number;
  disclaimer: string;
}

export interface WasteCollectionAnalytics {
  totalCollectedKg: number;
  completedTransactionsCount: number;
  totalTransactionValue: number;
  categoryBreakdown: CategoryAnalyticsPoint[];
  monthlyTrends: MonthlyTrendPoint[];
  startDate?: string | null;
  endDate?: string | null;
}

export interface RecyclingAnalytics {
  totalRecycledKg: number;
  confirmedReceiptsCount: number;
  activeFacilitiesCount: number;
  categoryBreakdown: CategoryAnalyticsPoint[];
  processorBreakdown: ProcessorAnalyticsPoint[];
  monthlyTrends: MonthlyTrendPoint[];
  startDate?: string | null;
  endDate?: string | null;
}

export interface EnvironmentalImpactResponse {
  totalRecycledKg: number;
  totalCo2AvoidedKg?: number | null;
  totalCo2AvoidedMt?: number | null;
  totalLandfillDivertedKg: number;
  categoryImpacts: CategoryImpact[];
  unconfiguredCategories: string[];
  methodology: string;
  disclaimer: string;
}

export interface GeneratorAnalyticsResponse {
  generatorId: number;
  totalListedKg: number;
  totalTransactedKg: number;
  completedTransactionsCount: number;
  totalEarnings: number;
  categoryDistribution: CategoryAnalyticsPoint[];
  confirmedRecycledKg: number;
  estimatedCo2AvoidedKg?: number | null;
  estimatedCo2AvoidedMt?: number | null;
  estimatedLandfillDivertedKg: number;
  monthlyTrends: MonthlyTrendPoint[];
  disclaimer: string;
}

export interface CollectorAnalyticsResponse {
  collectorId: number;
  totalCollectedKg: number;
  completedPickupsCount: number;
  totalCollectedValue: number;
  totalDeliveredToProcessorsKg: number;
  processorReceiptsCount: number;
  categoryDistribution: CategoryAnalyticsPoint[];
  monthlyTrends: MonthlyTrendPoint[];
}

export interface ProcessorAnalyticsResponse {
  processorProfileId: number;
  companyName: string;
  totalProcessedKg: number;
  confirmedReceiptsCount: number;
  maxCapacityKg: number;
  currentLoadKg: number;
  capacityUtilizationPercent: number;
  estimatedCo2AvoidedKg?: number | null;
  estimatedCo2AvoidedMt?: number | null;
  estimatedLandfillDivertedKg: number;
  categoryDistribution: CategoryAnalyticsPoint[];
  monthlyTrends: MonthlyTrendPoint[];
  disclaimer: string;
}

export interface EmissionFactor {
  id: number;
  categoryId: number;
  categoryName: string;
  co2FactorKgPerUnit?: number | null;
  landfillDiversionRate: number;
  unit: string;
  sourceReference: string;
  methodologyNotes?: string | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
  isActive: boolean;
}

export interface UpdateEmissionFactorRequest {
  co2FactorKgPerUnit?: number;
  landfillDiversionRate: number;
  sourceReference: string;
  methodologyNotes?: string;
  isActive?: boolean;
}
