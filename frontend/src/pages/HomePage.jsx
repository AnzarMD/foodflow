import { useAuth } from '../hooks/useAuth';

export default function HomePage() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-gray-900 text-white p-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold text-orange-400">🍔 FoodFlow</h1>
          <div className="flex items-center gap-4">
            <span className="text-gray-400">
              {user?.fullName} ({user?.role})
            </span>
            <button
              onClick={logout}
              className="bg-gray-700 hover:bg-gray-600 px-4 py-2 rounded-lg text-sm transition"
            >
              Sign Out
            </button>
          </div>
        </div>
        <div className="bg-gray-800 rounded-2xl p-8 text-center">
          <p className="text-gray-400 text-lg">
            Welcome! Restaurant list and order flow coming on Day 13.
          </p>
        </div>
      </div>
    </div>
  );
}