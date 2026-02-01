import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { toggleSidebar, toggleTheme } from '@/store/slices/uiSlice';
import { logout } from '@/store/slices/authSlice';
import { clearTenantState } from '@/store/slices/tenantSlice';
import clsx from 'clsx';
import {
  LayoutDashboard,
  FileText,
  Users,
  Mail,
  Settings,
  ChevronLeft,
  LogOut,
  Building2,
  Menu,
  Sun,
  Moon,
} from 'lucide-react';
import AcadexisLogo from '@/assets/AcadexisLogo.png';

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { name: 'CVs', href: '/cvs', icon: FileText },
  { name: 'Matches', href: '/matches', icon: Users },
  { name: 'Campaigns', href: '/campaigns', icon: Mail },
  { name: 'Configuration', href: '/settings', icon: Settings },
];

export const Sidebar: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { sidebarOpen, theme } = useAppSelector((state) => state.ui);
  const { user } = useAppSelector((state) => state.auth);
  const { currentTenant } = useAppSelector((state) => state.tenant);

  const handleLogout = () => {
    dispatch(logout());
    dispatch(clearTenantState());
    navigate('/login');
  };

  return (
    <>
      {/* Mobile menu button */}
      <button
        onClick={() => dispatch(toggleSidebar())}
        className="lg:hidden fixed top-4 left-4 z-50 p-2 bg-white rounded-lg shadow-md"
      >
        <Menu className="w-5 h-5" />
      </button>

      {/* Mobile backdrop */}
      {sidebarOpen && (
        <div
          className="lg:hidden fixed inset-0 bg-black/50 z-40"
          onClick={() => dispatch(toggleSidebar())}
        />
      )}

      {/* Sidebar */}
      <aside
        className={clsx(
          'fixed lg:static inset-y-0 left-0 z-50 flex flex-col bg-white dark:bg-slate-900 border-r border-gray-200 dark:border-slate-800 transition-all duration-300',
          sidebarOpen ? 'w-64' : 'w-20',
          !sidebarOpen && 'max-lg:w-0 max-lg:overflow-hidden'
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between h-16 px-4 border-b border-gray-200 dark:border-slate-800">
          {sidebarOpen && (
            <div className="flex items-center gap-2">
              <img src={AcadexisLogo} alt="Acadexis Logo" className="w-8 h-8 object-contain" />
              <span className="font-semibold text-gray-900 dark:text-white italic">Acadexis</span>
            </div>
          )}
          <button
            onClick={() => dispatch(toggleSidebar())}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
          >
            <ChevronLeft
              className={clsx('w-5 h-5 transition-transform', !sidebarOpen && 'rotate-180')}
            />
          </button>
        </div>

        {/* Tenant selector */}
        {sidebarOpen && currentTenant && (
          <div className="px-4 py-3 border-b border-gray-200 dark:border-slate-800">
            <div className="flex items-center gap-2 text-sm">
              <Building2 className="w-4 h-4 text-gray-400 dark:text-slate-500" />
              <span className="text-gray-600 dark:text-slate-400 truncate">{currentTenant.name}</span>
            </div>
          </div>
        )}

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navigation.map((item) => (
            <NavLink
              key={item.name}
              to={item.href}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-400'
                    : 'text-gray-600 dark:text-slate-400 hover:bg-gray-100 dark:hover:bg-slate-800 hover:text-gray-900 dark:hover:text-white'
                )
              }
            >
              <item.icon className="w-5 h-5 flex-shrink-0" />
              {sidebarOpen && <span>{item.name}</span>}
            </NavLink>
          ))}
        </nav>

        {/* Footer */}
        <div className="border-t border-gray-200 dark:border-slate-800 p-3">
          {sidebarOpen && user && (
            <div className="px-3 py-2 mb-2">
              <p className="text-sm font-medium text-gray-900 dark:text-white truncate">
                {user.firstName} {user.lastName}
              </p>
              <p className="text-xs text-gray-500 dark:text-slate-500 truncate">{user.email}</p>
            </div>
          )}

          <div className="space-y-1">
            <button
              onClick={() => dispatch(toggleTheme())}
              className="flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm font-medium text-gray-600 dark:text-slate-400 hover:bg-gray-100 dark:hover:bg-slate-800 hover:text-gray-900 dark:hover:text-white transition-colors"
            >
              {theme === 'light' ? (
                <>
                  <Moon className="w-5 h-5" />
                  {sidebarOpen && <span>Dark Mode</span>}
                </>
              ) : (
                <>
                  <Sun className="w-5 h-5" />
                  {sidebarOpen && <span>Light Mode</span>}
                </>
              )}
            </button>

            <button
              onClick={handleLogout}
              className="flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-900/10 transition-colors"
            >
              <LogOut className="w-5 h-5" />
              {sidebarOpen && <span>Logout</span>}
            </button>
          </div>
        </div>
      </aside>
    </>
  );
};
