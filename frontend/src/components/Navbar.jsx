import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  return (
    <nav className="bg-gray-800 border-b border-gray-700 px-6 py-4">
      <div className="max-w-5xl mx-auto flex items-center justify-between">
        <Link to="/" className="text-2xl font-bold text-orange-400">
          🍔 FoodFlow
        </Link>
        <div className="flex items-center gap-6">
          {user?.role === 'CUSTOMER' && (
            <>
              <Link to="/" className="text-gray-300 hover:text-white text-sm transition">
                Restaurants
              </Link>
              <Link to="/orders/my" className="text-gray-300 hover:text-white text-sm transition">
                My Orders
              </Link>
            </>
          )}
          {user?.role === 'RESTAURANT_OWNER' && (
            <Link to="/owner" className="text-gray-300 hover:text-white text-sm transition">
              Dashboard
            </Link>
          )}
          <div className="flex items-center gap-3">
            <span className="text-gray-400 text-sm">{user?.fullName}</span>
            <button
              onClick={handleLogout}
              className="bg-gray-700 hover:bg-gray-600 text-white text-sm px-3 py-2 rounded-lg transition"
            >
              Sign Out
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
}