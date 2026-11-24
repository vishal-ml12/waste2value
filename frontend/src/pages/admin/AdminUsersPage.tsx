import React, { useEffect, useState } from 'react';
import { userApi } from '../../api/client';
import { UserSummary } from '../../types';

export const AdminUsersPage: React.FC = () => {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    userApi.getAllUsers()
      .then(setUsers)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const getRoleBadge = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'GENERATOR':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'COLLECTOR':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'PROCESSOR':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      default:
        return 'bg-gray-50 text-gray-700 border-gray-200';
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Platform Users Overview</h1>
        <p className="text-xs text-gray-500 mt-1">
          Registered waste generators, collectors, processors, and platform administrators.
        </p>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="py-20 text-center text-sm text-gray-500">Loading platform users...</div>
        ) : users.length === 0 ? (
          <div className="p-12 text-center text-sm text-gray-500">No users found.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-left text-xs">
              <thead className="bg-gray-50 text-gray-500 font-medium">
                <tr>
                  <th className="px-6 py-3">User ID</th>
                  <th className="px-6 py-3">Full Name</th>
                  <th className="px-6 py-3">Email Address</th>
                  <th className="px-6 py-3">Role Authority</th>
                  <th className="px-6 py-3">Account Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-gray-50 transition">
                    <td className="px-6 py-4 font-mono text-gray-400">#{u.id}</td>
                    <td className="px-6 py-4 font-semibold text-gray-900">{u.firstName} {u.lastName}</td>
                    <td className="px-6 py-4 text-gray-600">{u.email}</td>
                    <td className="px-6 py-4">
                      <span className={`inline-block px-2.5 py-0.5 rounded text-[11px] font-semibold border ${getRoleBadge(u.role)}`}>
                        {u.role}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="inline-flex items-center text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded text-[11px] font-medium">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5"></span>
                        {u.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
