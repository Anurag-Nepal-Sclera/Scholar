import React, { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchDashboard } from '@/store/slices/tenantSlice';
import { Card, Spinner, Alert, EmptyState } from '@/components/ui';
import {
  FileText,
  Users,
  Mail,
  Send,
  AlertTriangle,
  CheckCircle,
  Clock,
  XCircle,
  Settings,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { currentTenant, dashboard, dashboardLoading, error } = useAppSelector(
    (state) => state.tenant
  );

  useEffect(() => {
    if (currentTenant) {
      dispatch(fetchDashboard(currentTenant.id));
    }
  }, [currentTenant, dispatch]);

  if (!currentTenant) {
    return (
      <EmptyState
        title="No organization selected"
        description="Please create or select an organization to get started."
        action={
          <Link
            to="/settings"
            className="text-primary-600 hover:text-primary-700 font-medium"
          >
            Go to Settings
          </Link>
        }
      />
    );
  }

  if (dashboardLoading && !dashboard) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="error" title="Error loading dashboard">
        {error}
      </Alert>
    );
  }

  const stats = [
    {
      name: 'Total CVs',
      value: dashboard?.totalCvs ?? 0,
      icon: FileText,
      color: 'bg-blue-500',
      href: '/cvs',
    },
    {
      name: 'Total Matches',
      value: dashboard?.totalMatches ?? 0,
      icon: Users,
      color: 'bg-purple-500',
      href: '/matches',
    },
    {
      name: 'Campaigns',
      value: dashboard?.totalCampaigns ?? 0,
      icon: Mail,
      color: 'bg-green-500',
      href: '/campaigns',
    },
    {
      name: 'Emails Sent',
      value: dashboard?.totalEmailsSent ?? 0,
      icon: Send,
      color: 'bg-orange-500',
      href: '/campaigns',
    },
  ];

  const campaignStats = dashboard?.campaignStatusCounts || {};

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-500 mt-1">
          Welcome back! Here's an overview of {currentTenant.name}
        </p>
      </div>

      {/* SMTP Warning */}
      {!dashboard?.smtpConfigured && (
        <Alert variant="warning" title="SMTP not configured">
          You need to configure your SMTP settings before you can send email campaigns.{' '}
          <Link to="/settings" className="font-medium underline">
            Configure now
          </Link>
        </Alert>
      )}

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat) => (
          <Link key={stat.name} to={stat.href}>
            <Card hover className="flex items-center gap-4">
              <div
                className={`w-12 h-12 ${stat.color} rounded-xl flex items-center justify-center`}
              >
                <stat.icon className="w-6 h-6 text-white" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
                <p className="text-sm text-gray-500">{stat.name}</p>
              </div>
            </Card>
          </Link>
        ))}
      </div>

      {/* Campaign Status */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Campaign Status</h3>
          <div className="space-y-3">
            <StatusItem
              icon={Clock}
              label="Draft"
              value={campaignStats.DRAFT || 0}
              color="text-gray-500"
            />
            <StatusItem
              icon={Clock}
              label="Scheduled"
              value={campaignStats.SCHEDULED || 0}
              color="text-purple-500"
            />
            <StatusItem
              icon={Send}
              label="In Progress"
              value={campaignStats.IN_PROGRESS || 0}
              color="text-blue-500"
            />
            <StatusItem
              icon={CheckCircle}
              label="Completed"
              value={campaignStats.COMPLETED || 0}
              color="text-green-500"
            />
            <StatusItem
              icon={XCircle}
              label="Failed"
              value={(campaignStats.FAILED || 0) + (campaignStats.CANCELLED || 0)}
              color="text-red-500"
            />
          </div>
        </Card>

        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
          <div className="space-y-2">
            <QuickAction
              icon={FileText}
              label="Upload a new CV"
              href="/cvs"
            />
            <QuickAction
              icon={Mail}
              label="Create email campaign"
              href="/campaigns"
            />
            <QuickAction
              icon={Settings}
              label="Configure SMTP settings"
              href="/settings"
            />
          </div>
        </Card>
      </div>

      {/* Email Stats */}
      {(dashboard?.totalEmailsSent ?? 0) > 0 && (
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Email Performance</h3>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-green-600">
                {dashboard?.totalEmailsSent ?? 0}
              </p>
              <p className="text-sm text-gray-500">Sent</p>
            </div>
            <div className="text-center">
              <p className="text-3xl font-bold text-red-600">
                {dashboard?.totalEmailsFailed ?? 0}
              </p>
              <p className="text-sm text-gray-500">Failed</p>
            </div>
            <div className="text-center">
              <p className="text-3xl font-bold text-primary-600">
                {dashboard?.totalEmailsSent && dashboard.totalEmailsSent > 0
                  ? (
                      ((dashboard.totalEmailsSent - (dashboard?.totalEmailsFailed ?? 0)) /
                        dashboard.totalEmailsSent) *
                      100
                    ).toFixed(1)
                  : 0}
                %
              </p>
              <p className="text-sm text-gray-500">Success Rate</p>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
};

const StatusItem: React.FC<{
  icon: React.FC<{ className?: string }>;
  label: string;
  value: number;
  color: string;
}> = ({ icon: Icon, label, value, color }) => (
  <div className="flex items-center justify-between">
    <div className="flex items-center gap-2">
      <Icon className={`w-4 h-4 ${color}`} />
      <span className="text-sm text-gray-600">{label}</span>
    </div>
    <span className="font-medium text-gray-900">{value}</span>
  </div>
);

const QuickAction: React.FC<{
  icon: React.FC<{ className?: string }>;
  label: string;
  href: string;
}> = ({ icon: Icon, label, href }) => (
  <Link
    to={href}
    className="flex items-center gap-3 px-4 py-3 rounded-lg border border-gray-200 hover:border-primary-300 hover:bg-primary-50 transition-colors"
  >
    <Icon className="w-5 h-5 text-gray-400" />
    <span className="text-sm font-medium text-gray-700">{label}</span>
  </Link>
);
