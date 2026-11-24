import React, { useEffect, useState } from 'react';
import { categoryApi } from '../../api/client';
import { Category, CategoryTree } from '../../types';

export const AdminCategoriesPage: React.FC = () => {
  const [tree, setTree] = useState<CategoryTree[]>([]);
  const [flatCategories, setFlatCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  // Modal states
  const [modalOpen, setModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);

  const [parentId, setParentId] = useState<string>('');
  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [unit, setUnit] = useState('kg');
  const [description, setDescription] = useState('');
  const [active, setActive] = useState(true);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [treeData, flatData] = await Promise.all([
        categoryApi.getTree(false),
        categoryApi.getFlat(false),
      ]);
      setTree(treeData);
      setFlatCategories(flatData);
    } catch (err) {
      console.error('Failed to load categories', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleOpenAddModal = (defaultParentId?: number) => {
    setEditingCategory(null);
    setParentId(defaultParentId ? defaultParentId.toString() : '');
    setName('');
    setSlug('');
    setUnit('kg');
    setDescription('');
    setActive(true);
    setError(null);
    setModalOpen(true);
  };

  const handleOpenEditModal = (cat: Category) => {
    setEditingCategory(cat);
    setParentId(cat.parentId ? cat.parentId.toString() : '');
    setName(cat.name);
    setSlug(cat.slug);
    setUnit(cat.unit);
    setDescription(cat.description || '');
    setActive(cat.active);
    setError(null);
    setModalOpen(true);
  };

  const handleToggleStatus = async (id: number, currentActive: boolean) => {
    try {
      await categoryApi.setStatus(id, !currentActive);
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update category status');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSaving(true);

    try {
      const payload = {
        parentId: parentId ? Number(parentId) : null,
        name: name.trim(),
        slug: slug.trim() || undefined,
        unit: unit.trim(),
        description: description ? description.trim() : undefined,
        active,
      };

      if (editingCategory) {
        await categoryApi.updateCategory(editingCategory.id, payload);
      } else {
        await categoryApi.createCategory(payload);
      }

      setModalOpen(false);
      fetchData();
    } catch (err: any) {
      const resp = err.response?.data;
      setError(resp?.message || 'Failed to save category');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Category Hierarchy Management</h1>
          <p className="text-xs text-gray-500 mt-1">
            Maintain standardized parent categories and subcategories with unique naming constraints.
          </p>
        </div>

        <button
          onClick={() => handleOpenAddModal()}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
        >
          + Add Root Category
        </button>
      </div>

      {loading ? (
        <div className="py-20 text-center text-sm text-gray-500">Loading category tree...</div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="p-4 bg-gray-50 border-b border-gray-200 flex justify-between items-center text-xs text-gray-500 font-semibold uppercase">
            <span>Category Name & Subcategories</span>
            <span>Unit / Status / Actions</span>
          </div>

          <div className="divide-y divide-gray-100">
            {tree.map((root) => (
              <div key={root.id} className="p-4 sm:p-5 hover:bg-gray-50/50 transition">
                {/* Root Category Row */}
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="font-bold text-gray-900 text-sm">{root.name}</span>
                      <span className="text-[10px] bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded font-mono">
                        slug: {root.slug}
                      </span>
                      {!root.active && (
                        <span className="text-[10px] bg-rose-50 text-rose-700 px-1.5 py-0.5 rounded font-medium">
                          Inactive
                        </span>
                      )}
                    </div>
                    {root.description && (
                      <p className="text-xs text-gray-500 mt-0.5">{root.description}</p>
                    )}
                  </div>

                  <div className="flex items-center space-x-3 text-xs">
                    <span className="text-gray-500 font-medium">Default Unit: {root.unit}</span>
                    <button
                      onClick={() => handleToggleStatus(root.id, root.active)}
                      className={`px-2 py-1 rounded text-[11px] font-semibold ${
                        root.active ? 'text-amber-700 bg-amber-50 hover:bg-amber-100' : 'text-emerald-700 bg-emerald-50 hover:bg-emerald-100'
                      }`}
                    >
                      {root.active ? 'Deactivate' : 'Activate'}
                    </button>
                    <button
                      onClick={() => {
                        const cat = flatCategories.find((c) => c.id === root.id);
                        if (cat) handleOpenEditModal(cat);
                      }}
                      className="text-gray-600 hover:text-gray-900 font-semibold"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleOpenAddModal(root.id)}
                      className="text-emerald-600 hover:text-emerald-700 font-semibold"
                    >
                      + Subcategory
                    </button>
                  </div>
                </div>

                {/* Subcategories list */}
                {root.subCategories && root.subCategories.length > 0 && (
                  <div className="mt-3 ml-4 sm:ml-6 pl-3 border-l-2 border-emerald-100 space-y-2">
                    {root.subCategories.map((sub) => (
                      <div
                        key={sub.id}
                        className="flex flex-col sm:flex-row justify-between items-start sm:items-center py-1.5 text-xs text-gray-700"
                      >
                        <div className="flex items-center space-x-2">
                          <span className="text-gray-400">↳</span>
                          <span className="font-semibold text-gray-800">{sub.name}</span>
                          <span className="text-[10px] text-gray-400 font-mono">({sub.slug})</span>
                          {!sub.active && (
                            <span className="text-[10px] bg-rose-50 text-rose-700 px-1 py-0.2 rounded font-medium">
                              Inactive
                            </span>
                          )}
                        </div>

                        <div className="flex items-center space-x-3">
                          <span className="text-gray-400 text-[11px]">{sub.unit}</span>
                          <button
                            onClick={() => handleToggleStatus(sub.id, sub.active)}
                            className={`text-[11px] ${sub.active ? 'text-amber-600 hover:underline' : 'text-emerald-600 hover:underline'}`}
                          >
                            {sub.active ? 'Deactivate' : 'Activate'}
                          </button>
                          <button
                            onClick={() => {
                              const cat = flatCategories.find((c) => c.id === sub.id);
                              if (cat) handleOpenEditModal(cat);
                            }}
                            className="text-gray-500 hover:text-gray-800"
                          >
                            Edit
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Modal: Add/Edit Category */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex justify-between items-center border-b border-gray-100 pb-3">
              <h3 className="font-bold text-base text-gray-900">
                {editingCategory ? 'Edit Category' : 'Create Category'}
              </h3>
              <button onClick={() => setModalOpen(false)} className="text-gray-400 hover:text-gray-600 text-lg">
                ✕
              </button>
            </div>

            {error && (
              <div className="bg-rose-50 border border-rose-200 text-rose-700 px-3 py-2 rounded text-xs">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Parent Category</label>
                <select
                  value={parentId}
                  onChange={(e) => setParentId(e.target.value)}
                  className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 bg-white outline-none focus:ring-1 focus:ring-emerald-500"
                >
                  <option value="">None (Top-Level Root Category)</option>
                  {flatCategories
                    .filter((c) => !editingCategory || c.id !== editingCategory.id)
                    .map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Category Name <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Polyethylene (HDPE)"
                  className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Slug (Optional)</label>
                  <input
                    type="text"
                    value={slug}
                    onChange={(e) => setSlug(e.target.value)}
                    placeholder="auto-generated"
                    className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">
                    Standard Unit <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={unit}
                    onChange={(e) => setUnit(e.target.value)}
                    placeholder="kg, items, ton"
                    className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Description</label>
                <textarea
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Standardized grading criteria and description"
                  className="w-full text-xs rounded-md border border-gray-300 p-2.5 outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>

              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="catActive"
                  checked={active}
                  onChange={(e) => setActive(e.target.checked)}
                  className="rounded border-gray-300 text-emerald-600 focus:ring-emerald-500"
                />
                <label htmlFor="catActive" className="text-xs text-gray-700">
                  Active (available in marketplace listings)
                </label>
              </div>

              <div className="flex justify-end space-x-3 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-xs font-semibold text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm disabled:opacity-50"
                >
                  {saving ? 'Saving...' : 'Save Category'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
