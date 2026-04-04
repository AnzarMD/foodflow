import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { getRestaurants } from '../api/restaurants';

export default function HomePage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['restaurants'],
    queryFn: getRestaurants,
  });

  const restaurants = data?.content ?? [];

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <Navbar />
      <div className="max-w-5xl mx-auto px-6 py-10">
        <h2 className="text-2xl font-bold mb-6">Restaurants near you</h2>

        {isLoading && (
          <div className="text-gray-400 text-center py-20">Loading restaurants...</div>
        )}

        {error && (
          <div className="text-red-400 text-center py-20">
            Failed to load restaurants. Make sure the backend is running.
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {restaurants.map((r) => (
            <Link
              key={r.id}
              to={`/restaurants/${r.id}`}
              className="bg-gray-800 rounded-2xl p-6 hover:bg-gray-750 hover:ring-1 hover:ring-orange-500 transition group"
            >
              <div className="flex items-start justify-between mb-3">
                <h3 className="text-lg font-semibold group-hover:text-orange-400 transition">
                  {r.name}
                </h3>
                {r.averageRating > 0 && (
                  <span className="text-yellow-400 text-sm">⭐ {r.averageRating}</span>
                )}
              </div>
              {r.cuisineType && (
                <p className="text-orange-400 text-sm font-medium mb-2">{r.cuisineType}</p>
              )}
              {r.address && (
                <p className="text-gray-400 text-sm">{r.address}</p>
              )}
              <p className="text-gray-500 text-sm mt-4">
                {r.description || 'Tap to view menu'}
              </p>
            </Link>
          ))}
        </div>

        {!isLoading && restaurants.length === 0 && (
          <div className="text-gray-400 text-center py-20">
            No restaurants found. Add one via the API as a Restaurant Owner.
          </div>
        )}
      </div>
    </div>
  );
}