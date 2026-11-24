import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Role } from '../../types';

export const RegisterPage: React.FC = () => {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [role, setRole] = useState<Role>('GENERATOR');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password.length < 8) {
      setError('Password must be at least 8 characters long');
      return;
    }

    setLoading(true);
    try {
      const user = await register({
        firstName,
        lastName,
        email,
        password,
        phone,
        role,
      });

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
        default:
          navigate('/browse');
      }
    } catch (err: any) {
      const resp = err.response?.data;
      if (resp?.validationErrors && resp.validationErrors.length > 0) {
        setError(resp.validationErrors.map((v: any) => v.message).join(', '));
      } else {
        setError(resp?.message || 'Registration failed. Please check your information.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[85vh] flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <div className="w-12 h-12 rounded-xl bg-emerald-600 flex items-center justify-center text-white font-bold text-2xl shadow-sm">
            ♻
          </div>
        </div>
        <h2 className="mt-4 text-center text-2xl font-extrabold text-gray-900 tracking-tight">
          Join Waste2Value Marketplace
        </h2>
        <p className="mt-1 text-center text-xs text-gray-500">
          Create an enterprise account to list, collect, or process waste materials
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-lg">
        <div className="bg-white py-8 px-6 shadow-sm border border-gray-200 rounded-2xl sm:px-10">
          {error && (
            <div className="mb-4 bg-rose-50 border border-rose-200 text-rose-700 px-4 py-3 rounded-lg text-xs">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Role Selection */}
            <div>
              <label className="block text-xs font-semibold text-gray-900 mb-2">
                Select Your Role in the Value Chain
              </label>
              <div className="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  onClick={() => setRole('GENERATOR')}
                  className={`p-3 rounded-xl border text-center transition flex flex-col items-center ${
                    role === 'GENERATOR'
                      ? 'border-emerald-600 bg-emerald-50/50 text-emerald-800'
                      : 'border-gray-200 hover:bg-gray-50 text-gray-600'
                  }`}
                >
                  <span className="text-xl mb-1">🏭</span>
                  <span className="text-xs font-bold">Generator</span>
                  <span className="text-[10px] text-gray-400 mt-0.5">List waste lots</span>
                </button>

                <button
                  type="button"
                  onClick={() => setRole('COLLECTOR')}
                  className={`p-3 rounded-xl border text-center transition flex flex-col items-center ${
                    role === 'COLLECTOR'
                      ? 'border-emerald-600 bg-emerald-50/50 text-emerald-800'
                      : 'border-gray-200 hover:bg-gray-50 text-gray-600'
                  }`}
                >
                  <span className="text-xl mb-1">🚛</span>
                  <span className="text-xs font-bold">Collector</span>
                  <span className="text-[10px] text-gray-400 mt-0.5">Aggregate pickups</span>
                </button>

                <button
                  type="button"
                  onClick={() => setRole('PROCESSOR')}
                  className={`p-3 rounded-xl border text-center transition flex flex-col items-center ${
                    role === 'PROCESSOR'
                      ? 'border-emerald-600 bg-emerald-50/50 text-emerald-800'
                      : 'border-gray-200 hover:bg-gray-50 text-gray-600'
                  }`}
                >
                  <span className="text-xl mb-1">⚙️</span>
                  <span className="text-xs font-bold">Processor</span>
                  <span className="text-[10px] text-gray-400 mt-0.5">Refine materials</span>
                </button>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-2">
              <div>
                <label className="block text-xs font-medium text-gray-700">First Name</label>
                <input
                  type="text"
                  required
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  placeholder="John"
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700">Last Name</label>
                <input
                  type="text"
                  required
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  placeholder="Doe"
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-700">Email Address</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@enterprise.com"
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 outline-none"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-gray-700">Phone</label>
                <input
                  type="text"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+91 9876543210"
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
                  placeholder="Min. 8 characters"
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 outline-none"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-semibold text-white bg-emerald-600 hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 disabled:opacity-50 transition mt-2"
            >
              {loading ? 'Creating Account...' : 'Complete Registration'}
            </button>
          </form>

          <div className="mt-6 text-center text-xs text-gray-500">
            Already registered?{' '}
            <Link to="/login" className="font-semibold text-emerald-600 hover:text-emerald-700">
              Sign In
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
