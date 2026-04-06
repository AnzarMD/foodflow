import axiosInstance from './axios';

export const placeOrder = (orderData) =>
  axiosInstance.post('/api/v1/orders', orderData).then((r) => r.data);

export const getMyOrders = () =>
  axiosInstance.get('/api/v1/orders/my?size=50&sort=createdAt,desc').then((r) => r.data);

export const getOrder = (orderId) =>
  axiosInstance.get(`/api/v1/orders/${orderId}`).then((r) => r.data);

export const cancelOrder = (orderId) =>
  axiosInstance.patch(`/api/v1/orders/${orderId}/cancel`).then((r) => r.data);