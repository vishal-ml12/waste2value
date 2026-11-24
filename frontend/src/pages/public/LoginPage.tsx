import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const from = (location.state as any)?.from?.pathname;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const user = await login({ email, password });
      if (from) {
        navigate(from, { replace: true });
      } else {
        switch (user.role) {
          case 'GENERATOR':
            navigate('/generator/dashboard');
            break;
          case 'COLLECTOR':
            navigate('/collector/dashboard');
            break;
          case 'PROCESSOR':
            navigate('/processor/dashboard');
            break;
          case 'ADMIN':
            navigate('/admin/categories');
            break;
          default:
            navigate('/browse');
        }
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid email or password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickFill = (demoEmail: string, demoPass: string) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setError(null);
  };

  return (
    <div className="min-h-[80vh] flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <div className="w-12 h-12 rounded-xl bg-emerald-600 flex items-center justify-center text-white font-bold text-2xl shadow-sm">
            ♻
          </div>
        </div>
        <h2 className="mt-4 text-center text-2xl font-extrabold text-gray-900 tracking-tight">
          Sign In to Waste2Value
        </h2>
        <p className="mt-1 text-center text-xs text-gray-500">
          Enter your credentials to access marketplace collection & listings
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-sm border border-gray-200 rounded-2xl sm:px-10">
          {error && (
            <div className="mb-4 bg-rose-50 border border-rose-200 text-rose-700 px-4 py-3 rounded-lg text-xs">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-gray-700">Email Address</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@company.com"
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-700">Password</label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 outline-none"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-semibold text-white bg-emerald-600 hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 disabled:opacity-50 transition"
            >
              {loading ? 'Authenticating...' : 'Sign In'}
            </button>
          </form>

          {/* Demo Credentials Quick-Fill */}
          <div className="mt-6 pt-6 border-t border-gray-100">
            <div className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider mb-2 text-center">
              Quick Fill Demo Accounts
            </div>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleQuickFill('generator@waste2value.com', 'Generator@123456')}
                className="px-2 py-1.5 rounded border border-gray-200 text-[11px] font-medium text-gray-700 hover:bg-emerald-50 hover:text-emerald-700 hover:border-emerald-300 text-left transition"
              >
                🏭 Generator Demo
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('collector@waste2value.com', 'Collector@123456')}
                className="px-2 py-1.5 rounded border border-gray-200 text-[11px] font-medium text-gray-700 hover:bg-emerald-50 hover:text-emerald-700 hover:border-emerald-300 text-left transition"
              >
                🚛 Collector Demo
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('processor@waste2value.com', 'Processor@123456')}
                className="px-2 py-1.5 rounded border border-gray-200 text-[11px] font-medium text-gray-700 hover:bg-emerald-50 hover:text-emerald-700 hover:border-emerald-300 text-left transition"
              >
                ⚙️ Processor Demo
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('admin@waste2value.com', 'Admin@123456')}
                className="px-2 py-1.5 rounded border border-gray-200 text-[11px] font-medium text-gray-700 hover:bg-emerald-50 hover:text-emerald-700 hover:border-emerald-300 text-left transition"
              >
                🛡️ Platform Admin
              </button>
            </div>
          </div>

          <div className="mt-6 text-center text-xs text-gray-500">
            Don't have an account?{' '}
            <Link to="/register" className="font-semibold text-emerald-600 hover:text-emerald-700">
              Create an account
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
