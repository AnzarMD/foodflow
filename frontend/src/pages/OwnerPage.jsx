import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Navbar from '../components/Navbar';
import OrderStatusBadge from '../components/OrderStatusBadge';
import { useAuth } from '../hooks/useAuth';
import {
  getIncomingOrders,
  acceptOrder,
  rejectOrder,
  createRestaurant,
  addMenuItem,
  getOwnerRestaurants,
  getMenu,
} from '../api/restaurants';

// ─── Tab: Incoming Orders ────────────────────────────────────────────────────

function IncomingOrdersTab() {
  const queryClient = useQueryClient();

  const { data: orders = [], isLoading, refetch } = useQuery({
    queryKey: ['incomingOrders'],
    queryFn: getIncomingOrders,
    refetchInterval: 5_000,
  });

  const acceptMutation = useMutation({
    mutationFn: acceptOrder,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['incomingOrders'] }),
  });

  const rejectMutation = useMutation({
    mutationFn: rejectOrder,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['incomingOrders'] }),
  });

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-xl font-bold">Incoming Orders</h3>
        <button onClick={() => refetch()} className="text-sm text-gray-400 hover:text-white transition">
          Refresh
        </button>
      </div>

      {isLoading && <p className="text-gray-400 text-center py-20">Loading...</p>}

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
  );
}

// ─── Tab: My Restaurants + Menu Management ───────────────────────────────────

function MenuManager({ restaurant }) {
  const queryClient = useQueryClient();
  const [showAddForm, setShowAddForm] = useState(false);
  const [newItem, setNewItem] = useState({ name: '', price: '', category: '', description: '' });
  const [error, setError] = useState('');

  const { data: menuItems = [], isLoading } = useQuery({
    queryKey: ['menu', restaurant.id],
    queryFn: () => getMenu(restaurant.id),
  });

  const addMutation = useMutation({
    mutationFn: (data) => addMenuItem(restaurant.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['menu', restaurant.id] });
      setNewItem({ name: '', price: '', category: '', description: '' });
      setShowAddForm(false);
      setError('');
    },
    onError: (err) => setError(err.response?.data?.message || 'Failed to add item'),
  });

  function handleAddItem(e) {
    e.preventDefault();
    if (!newItem.name || !newItem.price) return;
    addMutation.mutate({
      name: newItem.name,
      price: parseFloat(newItem.price),
      category: newItem.category,
      description: newItem.description,
    });
  }

  return (
    <div className="mt-4 border-t border-gray-700 pt-4">
      <div className="flex items-center justify-between mb-3">
        <p className="text-sm font-semibold text-gray-300">Menu Items</p>
        <button
          onClick={() => setShowAddForm(!showAddForm)}
          className="text-xs bg-orange-500 hover:bg-orange-600 text-white px-3 py-1 rounded-lg transition"
        >
          {showAddForm ? 'Cancel' : '+ Add Item'}
        </button>
      </div>

      {showAddForm && (
        <form onSubmit={handleAddItem} className="bg-gray-900 rounded-xl p-4 mb-4 space-y-3">
          {error && <p className="text-red-400 text-xs">{error}</p>}
          <div className="grid grid-cols-2 gap-3">
            <input
              value={newItem.name}
              onChange={(e) => setNewItem((p) => ({ ...p, name: e.target.value }))}
              placeholder="Item name *"
              required
              className="bg-gray-700 text-white text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-orange-500"
            />
            <input
              value={newItem.price}
              onChange={(e) => setNewItem((p) => ({ ...p, price: e.target.value }))}
              placeholder="Price (₹) *"
              type="number"
              min="0"
              step="0.01"
              required
              className="bg-gray-700 text-white text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-orange-500"
            />
            <input
              value={newItem.category}
              onChange={(e) => setNewItem((p) => ({ ...p, category: e.target.value }))}
              placeholder="Category"
              className="bg-gray-700 text-white text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-orange-500"
            />
            <input
              value={newItem.description}
              onChange={(e) => setNewItem((p) => ({ ...p, description: e.target.value }))}
              placeholder="Description"
              className="bg-gray-700 text-white text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-orange-500"
            />
          </div>
          <button
            type="submit"
            disabled={addMutation.isPending}
            className="w-full bg-orange-500 hover:bg-orange-600 disabled:opacity-50 text-white text-sm font-semibold py-2 rounded-lg transition"
          >
            {addMutation.isPending ? 'Adding...' : 'Add Menu Item'}
          </button>
        </form>
      )}

      {isLoading && <p className="text-gray-400 text-xs">Loading menu...</p>}

      {!isLoading && menuItems.length === 0 && (
        <p className="text-gray-500 text-sm">No menu items yet. Add one above.</p>
      )}

      <div className="space-y-2">
        {menuItems.map((item) => (
          <div key={item.id} className="flex items-center justify-between bg-gray-900 rounded-lg px-3 py-2">
            <div>
              <p className="text-sm font-medium text-white">{item.name}</p>
              {item.category && (
                <p className="text-xs text-gray-500">{item.category}</p>
              )}
            </div>
            <span className="text-orange-400 text-sm font-semibold">₹{item.price}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function MyRestaurantsTab() {
  const { user } = useAuth();

  const { data, isLoading } = useQuery({
    queryKey: ['allRestaurants'],
    queryFn: getOwnerRestaurants,
  });

  // Filter to only show restaurants owned by the current user
  const myRestaurants = (data?.content ?? []).filter(
    (r) => r.ownerId === user?.id
  );
  // Note: ownerId may not be in the response DTO — see note below

  return (
    <div>
      <h3 className="text-xl font-bold mb-6">My Restaurants</h3>

      {isLoading && <p className="text-gray-400">Loading...</p>}

      {!isLoading && myRestaurants.length === 0 && (
        <div className="bg-gray-800 rounded-2xl p-8 text-center text-gray-400">
          No restaurants yet. Create one in the Create Restaurant tab.
        </div>
      )}

      <div className="space-y-4">
        {myRestaurants.map((r) => (
          <div key={r.id} className="bg-gray-800 rounded-2xl p-6">
            <div className="flex items-start justify-between">
              <div>
                <h4 className="text-lg font-semibold">{r.name}</h4>
                {r.cuisineType && (
                  <p className="text-orange-400 text-sm">{r.cuisineType}</p>
                )}
                {r.address && (
                  <p className="text-gray-400 text-sm mt-1">📍 {r.address}</p>
                )}
              </div>
              <span className={`text-xs px-2 py-1 rounded-full ${r.active ? 'bg-green-500/20 text-green-400' : 'bg-gray-700 text-gray-400'}`}>
                {r.active ? 'Active' : 'Inactive'}
              </span>
            </div>
            <MenuManager restaurant={r} />
          </div>
        ))}
      </div>
    </div>
  );
}

function CreateRestaurantTab() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({ name: '', cuisineType: '', address: '', description: '' });
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  const mutation = useMutation({
    mutationFn: createRestaurant,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['allRestaurants'] });
      queryClient.invalidateQueries({ queryKey: ['restaurants'] });
      // ↑ Invalidate both so the customer's home page also refreshes
      setSuccess(`Restaurant "${data.name}" created! You can now add menu items in the My Restaurants tab.`);
      setForm({ name: '', cuisineType: '', address: '', description: '' });
      setError('');
    },
    onError: (err) => setError(err.response?.data?.message || 'Failed to create restaurant'),
  });

  function handleSubmit(e) {
    e.preventDefault();
    setSuccess('');
    setError('');
    mutation.mutate(form);
  }

  return (
    <div className="max-w-lg">
      <h3 className="text-xl font-bold mb-6">Create Restaurant</h3>

      {success && (
        <div className="bg-green-500/10 border border-green-500 text-green-400 px-4 py-3 rounded-lg mb-6 text-sm">
          {success}
        </div>
      )}
      {error && (
        <div className="bg-red-500/10 border border-red-500 text-red-400 px-4 py-3 rounded-lg mb-6 text-sm">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-1">Restaurant Name *</label>
          <input
            value={form.name}
            onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
            required
            className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-3 focus:outline-none focus:border-orange-500 transition"
            placeholder="e.g. Pizza Palace"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-1">Cuisine Type</label>
          <input
            value={form.cuisineType}
            onChange={(e) => setForm((p) => ({ ...p, cuisineType: e.target.value }))}
            className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-3 focus:outline-none focus:border-orange-500 transition"
            placeholder="e.g. Italian, Indian, Chinese"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-1">Address</label>
          <input
            value={form.address}
            onChange={(e) => setForm((p) => ({ ...p, address: e.target.value }))}
            className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-3 focus:outline-none focus:border-orange-500 transition"
            placeholder="e.g. 123 MG Road, Bengaluru"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-1">Description</label>
          <input
            value={form.description}
            onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
            className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-3 focus:outline-none focus:border-orange-500 transition"
            placeholder="Short description of your restaurant"
          />
        </div>
        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full bg-orange-500 hover:bg-orange-600 disabled:bg-orange-500/50 text-white font-semibold py-3 rounded-lg transition"
        >
          {mutation.isPending ? 'Creating...' : 'Create Restaurant'}
        </button>
      </form>
    </div>
  );
}

// ─── Main OwnerPage ──────────────────────────────────────────────────────────

export default function OwnerPage() {
  const [activeTab, setActiveTab] = useState('orders');

  const tabs = [
    { id: 'orders', label: 'Incoming Orders' },
    { id: 'restaurants', label: 'My Restaurants' },
    { id: 'create', label: 'Create Restaurant' },
  ];

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <Navbar />
      <div className="max-w-3xl mx-auto px-6 py-10">
        {/* Tab nav */}
        <div className="flex gap-2 mb-8 border-b border-gray-700 pb-4">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                activeTab === tab.id
                  ? 'bg-orange-500 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-gray-800'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {activeTab === 'orders' && <IncomingOrdersTab />}
        {activeTab === 'restaurants' && <MyRestaurantsTab />}
        {activeTab === 'create' && <CreateRestaurantTab />}
      </div>
    </div>
  );
}