import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import Navbar from '../components/Navbar';
import OrderStatusBadge from '../components/OrderStatusBadge';
import { getMyOrders } from '../api/orders';
import { useAuth } from '../hooks/useAuth';
import { useWebSocket } from '../hooks/useWebSocket';

export default function MyOrdersPage() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const { lastMessage, connected } = useWebSocket(accessToken);

  const { data, isLoading } = useQuery({
    queryKey: ['myOrders'],
    queryFn: getMyOrders,
  });

  const orders = data?.content ?? [];

  // When a WebSocket notification arrives, update the matching order in the
  // React Query cache immediately — no refetch needed.
  useEffect(() => {
    if (!lastMessage) return;

    queryClient.setQueryData(['myOrders'], (old) => {
      if (!old) return old;
      return {
        ...old,
        content: old.content.map((order) =>
          order.id === lastMessage.orderId
            ? { ...order, status: lastMessage.status }
            : order
        ),
      };
    });
    // ↑ We mutate the cache directly instead of calling refetch().
    //   This gives instant UI update with no network round-trip.
    //   The server already updated the DB — we just sync the UI.
  }, [lastMessage, queryClient]);

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <Navbar />
      <div className="max-w-3xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold">My Orders</h2>
          <div className="flex items-center gap-2 text-sm">
            <div className={`w-2 h-2 rounded-full ${connected ? 'bg-green-400' : 'bg-gray-500'}`} />
            <span className="text-gray-400">{connected ? 'Live updates on' : 'Connecting...'}</span>
          </div>
        </div>

        {isLoading && <p className="text-gray-400 text-center py-20">Loading orders...</p>}

        {!isLoading && orders.length === 0 && (
          <div className="bg-gray-800 rounded-2xl p-8 text-center text-gray-400">
            No orders yet. Go order something!
          </div>
        )}

        <div className="space-y-4">
          {orders.map((order) => (
            <div key={order.id} className="bg-gray-800 rounded-2xl p-6">
              <div className="flex items-start justify-between mb-4">
                <div>
                  <p className="font-semibold text-lg">{order.restaurantName}</p>
                  <p className="text-gray-400 text-sm mt-1">
                    {new Date(order.createdAt).toLocaleString()}
                  </p>
                </div>
                <OrderStatusBadge status={order.status} />
              </div>
              <div className="space-y-1 mb-4">
                {order.items?.map((item) => (
                  <div key={item.id} className="flex justify-between text-sm text-gray-300">
                    <span>{item.name} × {item.quantity}</span>
                    <span>₹{(item.unitPrice * item.quantity).toFixed(2)}</span>
                  </div>
                ))}
              </div>
              <div className="flex justify-between font-semibold border-t border-gray-700 pt-3">
                <span className="text-gray-300">Total</span>
                <span className="text-orange-400">₹{order.totalAmount}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}