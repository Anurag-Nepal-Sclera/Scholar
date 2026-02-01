import React from 'react';
import { Outlet } from 'react-router-dom';
import AcadexisLogo from '@/assets/AcadexisLogo.png';

export const AuthLayout: React.FC = () => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 via-white to-primary-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center mb-4">
            <img src={AcadexisLogo} alt="Acadexis Logo" className="w-16 h-16 object-contain" />
          </div>
          <h1 className="text-3xl font-bold text-gray-900 tracking-tight italic">Acadexis</h1>
          <p className="text-sm text-gray-500 mt-1 uppercase tracking-widest font-medium">CV Matching & Outreach Platform</p>
        </div>
        <div className="bg-white rounded-2xl shadow-xl border border-gray-200 p-6 sm:p-8">
          <Outlet />
        </div>
      </div>
    </div>
  );
};
