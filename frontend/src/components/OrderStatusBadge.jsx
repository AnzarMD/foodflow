const STATUS_STYLES = {
  PENDING:          'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
  ACCEPTED:         'bg-blue-500/20 text-blue-400 border-blue-500/30',
  PREPARING:        'bg-purple-500/20 text-purple-400 border-purple-500/30',
  OUT_FOR_DELIVERY: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
  DELIVERED:        'bg-green-500/20 text-green-400 border-green-500/30',
  CANCELLED:        'bg-red-500/20 text-red-400 border-red-500/30',
};

export default function OrderStatusBadge({ status }) {
  const style = STATUS_STYLES[status] || 'bg-gray-500/20 text-gray-400 border-gray-500/30';
  return (
    <span className={`inline-block px-3 py-1 rounded-full text-xs font-semibold border ${style}`}>
      {status}
    </span>
  );
}