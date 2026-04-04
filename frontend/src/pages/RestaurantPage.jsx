import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import Navbar from '../components/Navbar';
import { getRestaurants, getMenu } from '../api/restaurants';
import { placeOrder } from '../api/orders';

export default function RestaurantPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [cart, setCart] = useState({});
  // cart = { [menuItemId]: { item, quantity } }
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState('');

  const { data: restaurantsData } = useQuery({
    queryKey: ['restaurants'],
    queryFn: getRestaurants,
  });

  const restaurant = restaurantsData?.content?.find((r) => r.id === id);

  const { data: menuItems = [], isLoading } = useQuery({
    queryKey: ['menu', id],
    queryFn: () => getMenu(id),
    enabled: !!id,
  });

  function addToCart(item) {
    setCart((prev) => ({
      ...prev,
      [item.id]: {
        item,
        quantity: (prev[item.id]?.quantity || 0) + 1,
      },
    }));
  }

  function removeFromCart(itemId) {
    setCart((prev) => {
      const updated = { ...prev };
      if (updated[itemId]?.quantity > 1) {
        updated[itemId] = { ...updated[itemId], quantity: updated[itemId].quantity - 1 };
      } else {
        delete updated[itemId];
      }
      return updated;
    });
  }

  const cartItems = Object.values(cart);
  const total = cartItems.reduce((sum, { item, quantity }) => sum + item.price * quantity, 0);

  async function handlePlaceOrder() {
    if (cartItems.length === 0) return;
    setPlacing(true);
    setError('');
    try {
      await placeOrder({
        restaurantId: id,
        restaurantName: restaurant?.name || 'Restaurant',
        deliveryAddress: '123 MG Road, Bengaluru',
        // In a real app this would come from a form or user profile
        items: cartItems.map(({ item, quantity }) => ({
          menuItemId: item.id,
          name: item.name,
          unitPrice: item.price,
          quantity,
        })),
      });
      navigate('/orders/my');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to place order');
    } finally {
      setPlacing(false);
    }
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <Navbar />
      <div className="max-w-5xl mx-auto px-6 py-10">
        <button
          onClick={() => navigate('/')}
          className="text-gray-400 hover:text-white text-sm mb-6 flex items-center gap-2 transition"
        >
          ← Back to restaurants
        </button>

        <h2 className="text-2xl font-bold mb-1">{restaurant?.name || 'Restaurant'}</h2>
        {restaurant?.cuisineType && (
          <p className="text-orange-400 text-sm mb-6">{restaurant.cuisineType}</p>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Menu */}
          <div className="lg:col-span-2 space-y-4">
            <h3 className="text-lg font-semibold text-gray-300">Menu</h3>
            {isLoading && <p className="text-gray-400">Loading menu...</p>}
            {menuItems.map((item) => {
              const inCart = cart[item.id]?.quantity || 0;
              return (
                <div key={item.id} className="bg-gray-800 rounded-xl p-4 flex items-center justify-between">
                  <div>
                    <p className="font-medium">{item.name}</p>
                    {item.description && (
                      <p className="text-gray-400 text-sm mt-1">{item.description}</p>
                    )}
                    <p className="text-orange-400 font-semibold mt-2">₹{item.price}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    {inCart > 0 && (
                      <>
                        <button
                          onClick={() => removeFromCart(item.id)}
                          className="w-8 h-8 rounded-full bg-gray-700 hover:bg-gray-600 flex items-center justify-center font-bold transition"
                        >
                          −
                        </button>
                        <span className="w-4 text-center font-semibold">{inCart}</span>
                      </>
                    )}
                    <button
                      onClick={() => addToCart(item)}
                      className="w-8 h-8 rounded-full bg-orange-500 hover:bg-orange-600 flex items-center justify-center font-bold transition"
                    >
                      +
                    </button>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Cart */}
          <div className="bg-gray-800 rounded-2xl p-6 h-fit sticky top-6">
            <h3 className="text-lg font-semibold mb-4">Your Order</h3>
            {cartItems.length === 0 ? (
              <p className="text-gray-400 text-sm">Add items from the menu</p>
            ) : (
              <>
                <div className="space-y-3 mb-4">
                  {cartItems.map(({ item, quantity }) => (
                    <div key={item.id} className="flex justify-between text-sm">
                      <span className="text-gray-300">{item.name} × {quantity}</span>
                      <span className="text-white font-medium">₹{(item.price * quantity).toFixed(2)}</span>
                    </div>
                  ))}
                </div>
                <div className="border-t border-gray-700 pt-4 flex justify-between font-semibold mb-4">
                  <span>Total</span>
                  <span className="text-orange-400">₹{total.toFixed(2)}</span>
                </div>
                {error && (
                  <p className="text-red-400 text-sm mb-3">{error}</p>
                )}
                <button
                  onClick={handlePlaceOrder}
                  disabled={placing}
                  className="w-full bg-orange-500 hover:bg-orange-600 disabled:bg-orange-500/50 text-white font-semibold py-3 rounded-xl transition"
                >
                  {placing ? 'Placing...' : 'Place Order'}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}