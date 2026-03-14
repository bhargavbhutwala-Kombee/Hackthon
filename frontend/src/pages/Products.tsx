import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import api from '../api';
import { Search, Plus, Edit2, Trash2, X, AlertCircle } from 'lucide-react';

interface Product {
  id: number;
  sku: string;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
}

export default function Products() {
  const [products, setProducts] = useState<Product[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [filterName, setFilterName] = useState('');
  const [filterSku, setFilterSku] = useState('');
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  
  // Form State
  const [formData, setFormData] = useState({ sku: '', name: '', description: '', price: 0, stockQuantity: 0 });

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/products', {
        params: { page, size: 10, name: filterName, sku: filterSku }
      });
      setProducts(data.content);
      setTotalPages(data.totalPages);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch products');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, [page]); // Intentionally not dependent on filters until explicit search

  const handleSearch = (e: FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchProducts();
  };

  const openModal = (product?: Product) => {
    setError('');
    if (product) {
      setEditingProduct(product);
      setFormData({
        sku: product.sku,
        name: product.name,
        description: product.description || '',
        price: product.price,
        stockQuantity: product.stockQuantity,
      });
    } else {
      setEditingProduct(null);
      setFormData({ sku: '', name: '', description: '', price: 0, stockQuantity: 0 });
    }
    setIsModalOpen(true);
  };

  const saveProduct = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      if (editingProduct) {
        await api.put(`/products/${editingProduct.id}`, formData);
      } else {
        await api.post('/products', formData);
      }
      setIsModalOpen(false);
      fetchProducts();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save product. Check validation constraints.');
    }
  };

  const deleteProduct = async (id: number) => {
    if (!window.confirm('Delete this product?')) return;
    try {
      await api.delete(`/products/${id}`);
      fetchProducts();
    } catch (err: any) {
      setError('Failed to delete product.');
    }
  };

  return (
    <div className="space-y-6 animate-fade-in-up">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">Products</h1>
          <p className="text-textMuted mt-1">Manage your inventory catalog</p>
        </div>
        <button onClick={() => openModal()} className="glass-button flex items-center gap-2">
          <Plus className="w-5 h-5" /> Add Product
        </button>
      </div>

      {error && !isModalOpen && (
        <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <p>{error}</p>
        </div>
      )}

      {/* Filters */}
      <div className="glass-panel p-4">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-4 items-end">
          <div className="flex-1 space-y-1">
            <label className="text-xs font-semibold text-textMuted uppercase tracking-wider ml-1">Filter by Name</label>
            <input 
              type="text" 
              className="glass-input w-full" 
              placeholder="e.g. Laptop" 
              value={filterName}
              onChange={e => setFilterName(e.target.value)}
            />
          </div>
          <div className="flex-1 space-y-1">
            <label className="text-xs font-semibold text-textMuted uppercase tracking-wider ml-1">Filter by SKU</label>
            <input 
              type="text" 
              className="glass-input w-full" 
              placeholder="e.g. PROD-123" 
              value={filterSku}
              onChange={e => setFilterSku(e.target.value)}
            />
          </div>
          <button type="submit" className="glass-button-outline flex items-center gap-2 h-[50px]">
            <Search className="w-4 h-4" /> Search
          </button>
        </form>
      </div>

      {/* Table */}
      <div className="glass-panel overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-white/5 border-b border-white/10 text-textMuted text-sm">
                <th className="p-4 font-medium tracking-wide">ID</th>
                <th className="p-4 font-medium tracking-wide">SKU</th>
                <th className="p-4 font-medium tracking-wide">Name</th>
                <th className="p-4 font-medium tracking-wide">Price</th>
                <th className="p-4 font-medium tracking-wide">Stock</th>
                <th className="p-4 font-medium tracking-wide text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {loading ? (
                <tr>
                  <td colSpan={6} className="text-center p-8 text-textMuted">Loading products...</td>
                </tr>
              ) : products.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center p-8 text-textMuted">No products found.</td>
                </tr>
              ) : (
                products.map(p => (
                  <tr key={p.id} className="hover:bg-white/5 transition-colors group">
                    <td className="p-4 text-textMuted">#{p.id}</td>
                    <td className="p-4 font-mono text-sm">{p.sku}</td>
                    <td className="p-4 font-medium text-white">{p.name}</td>
                    <td className="p-4">${p.price.toFixed(2)}</td>
                    <td className="p-4">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${p.stockQuantity > 10 ? 'bg-green-500/20 text-green-400' : p.stockQuantity > 0 ? 'bg-yellow-500/20 text-yellow-400' : 'bg-red-500/20 text-red-400'}`}>
                        {p.stockQuantity} in stock
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button onClick={() => openModal(p)} className="p-2 text-textMuted hover:text-primary transition-colors hover:bg-white/5 rounded-lg">
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button onClick={() => deleteProduct(p.id)} className="p-2 text-textMuted hover:text-red-400 transition-colors hover:bg-white/5 rounded-lg">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        
        {/* Pagination */}
        <div className="p-4 border-t border-white/5 flex items-center justify-between text-sm text-textMuted bg-white/[0.02]">
          <span>Page {page + 1} of {Math.max(1, totalPages)}</span>
          <div className="flex gap-2">
            <button 
              disabled={page === 0} 
              onClick={() => setPage(p => p - 1)}
              className="px-3 py-1 rounded-lg bg-white/5 hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              Previous
            </button>
            <button 
              disabled={page >= totalPages - 1} 
              onClick={() => setPage(p => p + 1)}
              className="px-3 py-1 rounded-lg bg-white/5 hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {/* Modal Overlay */}
      {isModalOpen && (
        <div className="glass-overlay">
          <div className="glass-panel w-full max-w-lg p-6 animate-fade-in-up relative">
            <button onClick={() => setIsModalOpen(false)} className="absolute top-4 right-4 text-textMuted hover:text-white">
              <X className="w-5 h-5" />
            </button>
            
            <h2 className="text-2xl font-bold text-white mb-6">
              {editingProduct ? 'Edit Product' : 'New Product'}
            </h2>

            {error && (
              <div className="mb-4 bg-red-500/10 border border-red-500/20 text-red-400 p-3 rounded-lg text-sm">
                {error}
              </div>
            )}

            <form onSubmit={saveProduct} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-sm font-medium text-textMuted">SKU</label>
                  <input required type="text" className="glass-input w-full" value={formData.sku} onChange={e => setFormData({...formData, sku: e.target.value})} />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium text-textMuted">Name</label>
                  <input required type="text" className="glass-input w-full" value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} />
                </div>
              </div>
              
              <div className="space-y-1">
                <label className="text-sm font-medium text-textMuted">Description</label>
                <textarea className="glass-input w-full resize-none h-24" value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})}></textarea>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-sm font-medium text-textMuted">Price ($)</label>
                  <input required type="number" step="0.01" min="0" className="glass-input w-full" value={formData.price} onChange={e => setFormData({...formData, price: parseFloat(e.target.value)})} />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium text-textMuted">Stock Quantity</label>
                  <input required type="number" min="0" className="glass-input w-full" value={formData.stockQuantity} onChange={e => setFormData({...formData, stockQuantity: parseInt(e.target.value, 10)})} />
                </div>
              </div>

              <div className="pt-4 flex justify-end gap-3">
                <button type="button" onClick={() => setIsModalOpen(false)} className="glass-button-outline">Cancel</button>
                <button type="submit" className="glass-button">Save Product</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
