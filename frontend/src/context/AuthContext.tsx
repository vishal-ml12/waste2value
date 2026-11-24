import React, { createContext, useContext, useState, useEffect } from 'react';
import { UserSummary, Role } from '../types';
import { authApi } from '../api/client';

interface AuthContextType {
  user: UserSummary | null;
  role: Role | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: { email: string; password: string }) => Promise<UserSummary>;
  register: (data: any) => Promise<UserSummary>;
  logout: () => Promise<void>;
  updateUser: (user: UserSummary) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    try {
      const storedUser = localStorage.getItem('user');
      const token = localStorage.getItem('accessToken');
      if (storedUser && token) {
        setUser(JSON.parse(storedUser));
      }
    } catch (e) {
      console.error('Failed to parse stored user', e);
      localStorage.clear();
    } finally {
      setIsLoading(false);
    }
  }, []);

  const login = async (credentials: { email: string; password: string }) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(credentials);
      const authData = response.data;
      localStorage.setItem('accessToken', authData.accessToken);
      localStorage.setItem('refreshToken', authData.refreshToken);
      localStorage.setItem('user', JSON.stringify(authData.user));
      setUser(authData.user);
      return authData.user;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: any) => {
    setIsLoading(true);
    try {
      const response = await authApi.register(data);
      const authData = response.data;
      localStorage.setItem('accessToken', authData.accessToken);
      localStorage.setItem('refreshToken', authData.refreshToken);
      localStorage.setItem('user', JSON.stringify(authData.user));
      setUser(authData.user);
      return authData.user;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
      localStorage.clear();
      window.location.href = '/login';
    }
  };

  const updateUser = (updatedUser: UserSummary) => {
    setUser(updatedUser);
    localStorage.setItem('user', JSON.stringify(updatedUser));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        role: user?.role || null,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
