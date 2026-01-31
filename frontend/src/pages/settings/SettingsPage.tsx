import React, { useEffect, useState } from 'react';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchSmtpAccount, saveSmtpAccount, deactivateSmtpAccount } from '@/store/slices/smtpSlice';
import { createTenant, deleteTenant, setCurrentTenant } from '@/store/slices/tenantSlice';
import { openModal, closeModal } from '@/store/slices/uiSlice';
import {
  Card,
  CardHeader,
  Button,
  Input,
  Spinner,
  Alert,
  StatusBadge,
  Modal,
  EmptyState,
} from '@/components/ui';
import {
  Mail,
  Server,
  Lock,
  User,
  Shield,
  Building2,
  Plus,
  Trash2,
  Check,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { SmtpAccountRequest, TenantResponse } from '@/types';
import clsx from 'clsx';

export const SettingsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { account: smtpAccount, loading: smtpLoading, saving: smtpSaving } = useAppSelector(
    (state) => state.smtp
  );
  const { tenants, currentTenant, loading: tenantsLoading } = useAppSelector(
    (state) => state.tenant
  );
  const { modal } = useAppSelector((state) => state.ui);

  const [smtpForm, setSmtpForm] = useState<SmtpAccountRequest>({
    email: '',
    smtpHost: '',
    smtpPort: 587,
    username: '',
    password: '',
    useTls: true,
    useSsl: false,
    fromName: '',
  });
  const [smtpErrors, setSmtpErrors] = useState<Record<string, string>>({});

  const [tenantForm, setTenantForm] = useState({ name: '', email: '' });
  const [tenantErrors, setTenantErrors] = useState<Record<string, string>>({});
  const [deletingTenant, setDeletingTenant] = useState<TenantResponse | null>(null);

  useEffect(() => {
    if (currentTenant) {
      dispatch(fetchSmtpAccount());
    }
  }, [currentTenant, dispatch]);

  useEffect(() => {
    if (smtpAccount) {
      setSmtpForm({
        email: smtpAccount.email,
        smtpHost: smtpAccount.smtpHost,
        smtpPort: smtpAccount.smtpPort,
        username: smtpAccount.username,
        password: '', // Don't show password
        useTls: smtpAccount.useTls,
        useSsl: smtpAccount.useSsl,
        fromName: smtpAccount.fromName || '',
      });
    }
  }, [smtpAccount]);

  const validateSmtpForm = () => {
    const errors: Record<string, string> = {};
    if (!smtpForm.email) errors.email = 'Email is required';
    if (!smtpForm.smtpHost) errors.smtpHost = 'SMTP host is required';
    if (!smtpForm.smtpPort) errors.smtpPort = 'Port is required';
    if (!smtpForm.username) errors.username = 'Username is required';
    if (!smtpForm.password && !smtpAccount) errors.password = 'Password is required';
    setSmtpErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSaveSmtp = async () => {
    if (!validateSmtpForm()) return;

    const result = await dispatch(saveSmtpAccount(smtpForm));
    if (saveSmtpAccount.fulfilled.match(result)) {
      toast.success('SMTP settings saved successfully');
    }
  };

  const handleDeactivateSmtp = async () => {
    const result = await dispatch(deactivateSmtpAccount());
    if (deactivateSmtpAccount.fulfilled.match(result)) {
      toast.success('SMTP account deactivated');
    }
  };

  const validateTenantForm = () => {
    const errors: Record<string, string> = {};
    if (!tenantForm.name.trim()) errors.name = 'Organization name is required';
    if (!tenantForm.email) errors.email = 'Email is required';
    setTenantErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateTenant = async () => {
    if (!validateTenantForm()) return;

    const result = await dispatch(createTenant(tenantForm));
    if (createTenant.fulfilled.match(result)) {
      toast.success('Organization created successfully');
      dispatch(closeModal());
      setTenantForm({ name: '', email: '' });
    }
  };

  const handleDeleteTenant = async () => {
    if (!deletingTenant) return;

    const result = await dispatch(deleteTenant(deletingTenant.id));
    if (deleteTenant.fulfilled.match(result)) {
      toast.success('Organization deleted');
    }
    setDeletingTenant(null);
    dispatch(closeModal());
  };

  return (
    <div className="space-y-8 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
        <p className="text-sm text-gray-500 mt-1">
          Manage your organizations and SMTP configuration
        </p>
      </div>

      {/* Organizations Section */}
      <section>
        <Card>
          <CardHeader
            title="Organizations"
            description="Manage your organizations and switch between them"
            action={
              <Button
                size="sm"
                icon={<Plus className="w-4 h-4" />}
                onClick={() => dispatch(openModal({ type: 'createTenant' }))}
              >
                Add Organization
              </Button>
            }
          />

          {tenantsLoading && tenants.length === 0 ? (
            <div className="flex items-center justify-center py-8">
              <Spinner />
            </div>
          ) : tenants.length === 0 ? (
            <EmptyState
              icon={<Building2 className="w-6 h-6" />}
              title="No organizations"
              description="Create your first organization to get started."
            />
          ) : (
            <div className="space-y-2">
              {tenants.map((tenant) => (
                <div
                  key={tenant.id}
                  className={clsx(
                    'flex items-center justify-between p-3 rounded-lg border transition-colors',
                    currentTenant?.id === tenant.id
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  )}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={clsx(
                        'w-10 h-10 rounded-lg flex items-center justify-center',
                        currentTenant?.id === tenant.id
                          ? 'bg-primary-500 text-white'
                          : 'bg-gray-100 text-gray-500'
                      )}
                    >
                      <Building2 className="w-5 h-5" />
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">{tenant.name}</p>
                      <p className="text-sm text-gray-500">{tenant.email}</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <StatusBadge status={tenant.status} />
                    {currentTenant?.id !== tenant.id && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => dispatch(setCurrentTenant(tenant))}
                      >
                        Select
                      </Button>
                    )}
                    {currentTenant?.id === tenant.id && (
                      <span className="flex items-center gap-1 text-sm text-primary-600">
                        <Check className="w-4 h-4" />
                        Active
                      </span>
                    )}
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Trash2 className="w-4 h-4" />}
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                      onClick={() => {
                        setDeletingTenant(tenant);
                        dispatch(openModal({ type: 'deleteTenant' }));
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </section>

      {/* SMTP Section */}
      <section>
        <Card>
          <CardHeader
            title="SMTP Configuration"
            description="Configure email sending settings for campaigns"
          />

          {!currentTenant ? (
            <Alert variant="info">
              Please select an organization to configure SMTP settings.
            </Alert>
          ) : smtpLoading ? (
            <div className="flex items-center justify-center py-8">
              <Spinner />
            </div>
          ) : (
            <div className="space-y-6">
              {smtpAccount && (
                <Alert
                  variant={smtpAccount.status === 'ACTIVE' ? 'success' : 'warning'}
                  title={`SMTP Status: ${smtpAccount.status}`}
                >
                  {smtpAccount.status === 'ACTIVE'
                    ? 'Your SMTP is configured and ready to send emails.'
                    : 'SMTP is not active. Please check your configuration.'}
                </Alert>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="From Email"
                  type="email"
                  placeholder="noreply@example.com"
                  icon={<Mail className="w-4 h-4" />}
                  value={smtpForm.email}
                  onChange={(e) => setSmtpForm({ ...smtpForm, email: e.target.value })}
                  error={smtpErrors.email}
                />
                <Input
                  label="From Name"
                  placeholder="Your Name"
                  icon={<User className="w-4 h-4" />}
                  value={smtpForm.fromName}
                  onChange={(e) => setSmtpForm({ ...smtpForm, fromName: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="SMTP Host"
                  placeholder="smtp.gmail.com"
                  icon={<Server className="w-4 h-4" />}
                  value={smtpForm.smtpHost}
                  onChange={(e) => setSmtpForm({ ...smtpForm, smtpHost: e.target.value })}
                  error={smtpErrors.smtpHost}
                />
                <Input
                  label="SMTP Port"
                  type="number"
                  placeholder="587"
                  value={smtpForm.smtpPort.toString()}
                  onChange={(e) =>
                    setSmtpForm({ ...smtpForm, smtpPort: parseInt(e.target.value) || 587 })
                  }
                  error={smtpErrors.smtpPort}
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="Username"
                  placeholder="your-email@gmail.com"
                  icon={<User className="w-4 h-4" />}
                  value={smtpForm.username}
                  onChange={(e) => setSmtpForm({ ...smtpForm, username: e.target.value })}
                  error={smtpErrors.username}
                />
                <Input
                  label="Password"
                  type="password"
                  placeholder={smtpAccount ? '••••••••' : 'Enter password'}
                  icon={<Lock className="w-4 h-4" />}
                  value={smtpForm.password}
                  onChange={(e) => setSmtpForm({ ...smtpForm, password: e.target.value })}
                  error={smtpErrors.password}
                  hint={smtpAccount ? 'Leave blank to keep existing password' : undefined}
                />
              </div>

              <div className="flex items-center gap-6">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={smtpForm.useTls}
                    onChange={(e) => setSmtpForm({ ...smtpForm, useTls: e.target.checked })}
                    className="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                  />
                  <span className="text-sm text-gray-700">Use TLS</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={smtpForm.useSsl}
                    onChange={(e) => setSmtpForm({ ...smtpForm, useSsl: e.target.checked })}
                    className="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                  />
                  <span className="text-sm text-gray-700">Use SSL</span>
                </label>
              </div>

              <div className="flex justify-between pt-4 border-t">
                {smtpAccount?.status === 'ACTIVE' && (
                  <Button
                    variant="outline"
                    onClick={handleDeactivateSmtp}
                    className="text-red-600 hover:text-red-700"
                  >
                    Deactivate
                  </Button>
                )}
                <div className="ml-auto">
                  <Button
                    icon={<Shield className="w-4 h-4" />}
                    onClick={handleSaveSmtp}
                    loading={smtpSaving}
                  >
                    Save SMTP Settings
                  </Button>
                </div>
              </div>
            </div>
          )}
        </Card>
      </section>

      {/* Create Tenant Modal */}
      <Modal
        isOpen={modal.isOpen && modal.type === 'createTenant'}
        onClose={() => {
          dispatch(closeModal());
          setTenantForm({ name: '', email: '' });
          setTenantErrors({});
        }}
        title="Create Organization"
      >
        <div className="space-y-4">
          <Input
            label="Organization Name"
            placeholder="My Company"
            icon={<Building2 className="w-4 h-4" />}
            value={tenantForm.name}
            onChange={(e) => setTenantForm({ ...tenantForm, name: e.target.value })}
            error={tenantErrors.name}
          />
          <Input
            label="Contact Email"
            type="email"
            placeholder="contact@company.com"
            icon={<Mail className="w-4 h-4" />}
            value={tenantForm.email}
            onChange={(e) => setTenantForm({ ...tenantForm, email: e.target.value })}
            error={tenantErrors.email}
          />
          <div className="flex justify-end gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => {
                dispatch(closeModal());
                setTenantForm({ name: '', email: '' });
                setTenantErrors({});
              }}
            >
              Cancel
            </Button>
            <Button onClick={handleCreateTenant}>Create</Button>
          </div>
        </div>
      </Modal>

      {/* Delete Tenant Modal */}
      <Modal
        isOpen={modal.isOpen && modal.type === 'deleteTenant'}
        onClose={() => {
          dispatch(closeModal());
          setDeletingTenant(null);
        }}
        title="Delete Organization"
      >
        <Alert variant="error" className="mb-4">
          This action cannot be undone. All CVs, matches, and campaigns will be permanently deleted.
        </Alert>
        <p className="text-gray-600 mb-6">
          Are you sure you want to delete <strong>{deletingTenant?.name}</strong>?
        </p>
        <div className="flex justify-end gap-3">
          <Button
            variant="outline"
            onClick={() => {
              dispatch(closeModal());
              setDeletingTenant(null);
            }}
          >
            Cancel
          </Button>
          <Button variant="danger" onClick={handleDeleteTenant}>
            Delete Organization
          </Button>
        </div>
      </Modal>
    </div>
  );
};
