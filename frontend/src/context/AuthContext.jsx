import { createContext, useState, useEffect, useCallback } from 'react';
import axiosInstance from '../api/axios';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // user = { userId, email, fullName, role } or null if not logged in

  const [accessToken, setAccessToken] = useState(null);
  // In-memory only — never in localStorage

  const [loading, setLoading] = useState(true);
  // True while we attempt to restore session from a stored refresh token

  // On app startup, try to restore session using a stored refresh token.
  // This means the user doesn't have to log in again after a page refresh.
  useEffect(() => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      setLoading(false);
      return;
    }

    axiosInstance
      .post('/api/v1/auth/refresh', { refreshToken })
      .then((res) => {
        const { accessToken: newToken, refreshToken: newRefresh, user: userData } = res.data;
        window.__accessToken = newToken;
        setAccessToken(newToken);
        setUser(userData);
        localStorage.setItem('refreshToken', newRefresh);
      })
      .catch(() => {
        // Refresh token expired or invalid — clear everything
        localStorage.removeItem('refreshToken');
      })
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email, password) => {
    const res = await axiosInstance.post('/api/v1/auth/login', { email, password });
    const { accessToken: token, refreshToken, user: userData } = res.data;

    window.__accessToken = token;
    setAccessToken(token);
    setUser(userData);
    localStorage.setItem('refreshToken', refreshToken);
    // ↑ Refresh token goes to localStorage so it survives page refresh.
    //   Access token stays in memory only (window.__accessToken + React state).
    return userData;
  }, []);

  const register = useCallback(async (email, password, fullName, role = 'CUSTOMER') => {
    const res = await axiosInstance.post('/api/v1/auth/register', {
      email,
      password,
      fullName,
      role,
    });
    const { accessToken: token, refreshToken, user: userData } = res.data;

    window.__accessToken = token;
    setAccessToken(token);
    setUser(userData);
    localStorage.setItem('refreshToken', refreshToken);
    return userData;
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      try {
        await axiosInstance.post('/api/v1/auth/logout', { refreshToken });
      } catch {
        // Ignore errors — log out locally regardless
      }
    }
    window.__accessToken = null;
    setAccessToken(null);
    setUser(null);
    localStorage.removeItem('refreshToken');
  }, []);

  return (
    <AuthContext.Provider value={{ user, accessToken, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}