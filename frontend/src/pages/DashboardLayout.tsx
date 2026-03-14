import { Outlet, NavLink } from 'react-router-dom';
import { Package, LogOut, Activity } from 'lucide-react';
import { useAuth } from '../AuthContext';

export default function DashboardLayout() {
  const { logout } = useAuth();

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* Sidebar */}
      <aside className="w-64 glass-panel m-4 flex flex-col justify-between hidden md:flex">
        <div>
          <div className="p-6 flex items-center space-x-3 mb-6">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary to-accent flex items-center justify-center shadow-lg shadow-primary/30">
              <Activity className="text-white w-6 h-6" />
            </div>
            <h1 className="text-2xl font-bold bg-gradient-to-r from-white to-textMuted bg-clip-text text-transparent">
              Orderly
            </h1>
          </div>
          
          <nav className="space-y-2 px-4">
            <NavLink
              to="/dashboard/products"
              className={({ isActive }) =>
                `flex items-center space-x-3 px-4 py-3 rounded-xl transition-all ${
                  isActive ? 'bg-primary/20 text-primary border border-primary/20' : 'text-textMuted hover:bg-white/5 hover:text-white'
                }`
              }
            >
              <Package className="w-5 h-5" />
              <span className="font-medium">Products</span>
            </NavLink>
          </nav>
        </div>

        <div className="p-4">
          <button
            onClick={logout}
            className="flex items-center space-x-3 w-full px-4 py-3 rounded-xl text-textMuted hover:bg-red-500/10 hover:text-red-400 transition-all border border-transparent hover:border-red-500/20"
          >
            <LogOut className="w-5 h-5" />
            <span className="font-medium">Logout</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-4 md:p-8 overflow-y-auto">
        <div className="max-w-7xl mx-auto h-full relative z-10">
          <Outlet />
        </div>
        
        {/* Decorative background blurs inside main area */}
        <div className="fixed top-[20%] right-[10%] w-[500px] h-[500px] bg-secondary/10 rounded-full blur-[100px] pointer-events-none" />
      </main>
    </div>
  );
}
