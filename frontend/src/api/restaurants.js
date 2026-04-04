import axiosInstance from './axios';

export const getRestaurants = () =>
  axiosInstance.get('/api/v1/restaurants').then((r) => r.data);

export const getMenu = (restaurantId) =>
  axiosInstance.get(`/api/v1/restaurants/${restaurantId}/menu`).then((r) => r.data);

export const getIncomingOrders = () =>
  axiosInstance.get('/api/v1/restaurants/incoming-orders').then((r) => r.data);

export const acceptOrder = (orderId) =>
  axiosInstance.patch(`/api/v1/restaurants/orders/${orderId}/accept`).then((r) => r.data);

export const rejectOrder = (orderId) =>
  axiosInstance.patch(`/api/v1/restaurants/orders/${orderId}/reject`).then((r) => r.data);