import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Navbar from '../components/Navbar';
import OrderStatusBadge from '../components/OrderStatusBadge';
import { getIncomingOrders, acceptOrder, rejectOrder } from '../api/restaurants';

export default function OwnerPage() {
  const queryClient = useQueryClient();

  const { data: orders = [], isLoading, refetch } = useQuery({
    queryKey: ['incomingOrders'],
    queryFn: getIncomingOrders,
    refetchInterval: 15_000,
    // ↑ Poll every 15 seconds as a safety net.
    //   In a full implementation the owner would also have a WebSocket.
  });

  const acceptMutation = useMutation({
    mutationFn: acceptOrder,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['incomingOrders'] }),
    // ↑ After accepting, refetch the list so the accepted order disappears
    //   (getIncomingOrders only returns PENDING orders).
  });

  const rejectMutation = useMutation({
    mutationFn: rejectOrder,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['incomingOrders'] }),
  });

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <Navbar />
      <div className="max-w-3xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold">Incoming Orders</h2>
          <button
            onClick={() => refetch()}
            className="text-sm text-gray-400 hover:text-white transition"
          >
            Refresh
          </button>
        </div>

        {isLoading && <p className="text-gray-400 text-center py-20">Loading orders...</p>}

        {!isLoading && orders.length === 0 && (
          <div className="bg-gray-800 rounded-2xl p-8 text-center text-gray-400">
            No pending orders right now.
          </div>
        )}

        <div className="space-y-4">
          {orders.map((order) => (
            <div key={order.id} className="bg-gray-800 rounded-2xl p-6">
              <div className="flex items-start justify-between mb-4">
                <div>
                  <p className="font-semibold">Order #{order.orderId.slice(0, 8)}...</p>
                  <p className="text-gray-400 text-sm mt-1">
                    {new Date(order.createdAt).toLocaleString()}
                  </p>
                  {order.deliveryAddress && (
                    <p className="text-gray-400 text-sm">📍 {order.deliveryAddress}</p>
                  )}
                </div>
                <OrderStatusBadge status={order.status} />
              </div>
              <div className="flex justify-between font-semibold border-t border-gray-700 pt-3 mb-4">
                <span className="text-gray-300">Total</span>
                <span className="text-orange-400">₹{order.totalAmount}</span>
              </div>
              {order.status === 'PENDING' && (
                <div className="flex gap-3">
                  <button
                    onClick={() => acceptMutation.mutate(order.orderId)}
                    disabled={acceptMutation.isPending}
                    className="flex-1 bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white font-semibold py-2 rounded-xl transition"
                  >
                    Accept
                  </button>
                  <button
                    onClick={() => rejectMutation.mutate(order.orderId)}
                    disabled={rejectMutation.isPending}
                    className="flex-1 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white font-semibold py-2 rounded-xl transition"
                  >
                    Reject
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}