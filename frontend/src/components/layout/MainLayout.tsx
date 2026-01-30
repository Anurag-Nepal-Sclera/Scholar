import React, { useEffect } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchTenants } from '@/store/slices/tenantSlice';
import { Sidebar } from './Sidebar';
import { LoadingPage } from '@/components/ui';
import clsx from 'clsx';

export const MainLayout: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { isAuthenticated } = useAppSelector((state) => state.auth);
  const { loading: tenantsLoading, tenants } = useAppSelector((state) => state.tenant);
  const { sidebarOpen } = useAppSelector((state) => state.ui);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    dispatch(fetchTenants());
  }, [isAuthenticated, dispatch, navigate]);

  if (tenantsLoading && tenants.length === 0) {
    return <LoadingPage message="Loading your workspace..." />;
  }

  return (
    <div className="min-h-screen bg-gray-50 flex">
      <Sidebar />
      <main
        className={clsx(
          'flex-1 transition-all duration-300',
          sidebarOpen ? 'lg:ml-0' : 'lg:ml-0'
        )}
      >
        <div className="p-4 sm:p-6 lg:p-8 pt-16 lg:pt-4">
          <Outlet />
        </div>
      </main>
    </div>
  );
};
