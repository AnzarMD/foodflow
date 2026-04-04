import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function ProtectedRoute({ children, requiredRole }) {
  const { user, loading } = useAuth();

  // While we're checking for a stored refresh token, show nothing.
  // Without this, the app flashes the login page before restoring the session.
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-900">
        <div className="text-white text-lg">Loading...</div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
    // replace means the login page doesn't go into browser history,
    // so the back button doesn't take them to a page they can't access.
  }

  if (requiredRole && user.role !== requiredRole) {
    return <Navigate to="/" replace />;
    // Wrong role — redirect to home instead of login
  }

  return children;
}