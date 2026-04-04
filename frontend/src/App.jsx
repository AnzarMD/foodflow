import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import HomePage from './pages/HomePage';
import RestaurantPage from './pages/RestaurantPage';
import MyOrdersPage from './pages/MyOrdersPage';
import OwnerPage from './pages/OwnerPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000 },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            <Route path="/" element={
              <ProtectedRoute>
                <HomePage />
              </ProtectedRoute>
            } />

            <Route path="/restaurants/:id" element={
              <ProtectedRoute requiredRole="CUSTOMER">
                <RestaurantPage />
              </ProtectedRoute>
            } />

            <Route path="/orders/my" element={
              <ProtectedRoute requiredRole="CUSTOMER">
                <MyOrdersPage />
              </ProtectedRoute>
            } />

            <Route path="/owner" element={
              <ProtectedRoute requiredRole="RESTAURANT_OWNER">
                <OwnerPage />
              </ProtectedRoute>
            } />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}