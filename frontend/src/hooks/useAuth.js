import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used inside AuthProvider');
    }
    return context;
}
// Convenience hook so components write: const { user, login } = useAuth()
// instead of: const { user, login } = useContext(AuthContext)