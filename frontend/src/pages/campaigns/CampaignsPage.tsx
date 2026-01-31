import React, { useEffect, useState } from 'react';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import {
  fetchCampaigns,
  createCampaign,
  executeCampaign,
  cancelCampaign,
  fetchCampaignLogs,
} from '@/store/slices/campaignSlice';
import { fetchCVs } from '@/store/slices/cvSlice';
import { openModal, closeModal } from '@/store/slices/uiSlice';
import {
  Card,
  CardHeader,
  Button,
  Spinner,
  StatusBadge,
  EmptyState,
  Modal,
  Input,
  Select,
  Textarea,
  Alert,
} from '@/components/ui';
import {
  Mail,
  Plus,
  Play,
  XCircle,
  RefreshCw,
  Calendar,
  Send,
  AlertCircle,
  Eye,
} from 'lucide-react';
import { format } from 'date-fns';
import toast from 'react-hot-toast';
import { CreateCampaignRequest, EmailCampaignResponse, EmailLogResponse } from '@/types';

export const CampaignsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { campaigns, logs, loading, creating, executing, logsLoading, logsPagination } = useAppSelector(
    (state) => state.campaign
  );
  const { cvs } = useAppSelector((state) => state.cv);
  const { currentTenant } = useAppSelector((state) => state.tenant);
  const { account: smtpAccount } = useAppSelector((state) => state.smtp);
  const { modal } = useAppSelector((state) => state.ui);

  const [selectedCampaign, setSelectedCampaign] = useState<EmailCampaignResponse | null>(null);
  const [formData, setFormData] = useState<CreateCampaignRequest>({
    cvId: '',
    name: '',
    subject: '',
    bodyTemplate: '',
    minMatchScore: 0.5,
  });
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (currentTenant) {
      dispatch(fetchCampaigns({}));
      dispatch(fetchCVs({}));
    }
  }, [currentTenant, dispatch]);

  const cvOptions = cvs
    .filter((cv) => cv.parsingStatus === 'COMPLETED')
    .map((cv) => ({
      value: cv.id,
      label: cv.originalFilename,
    }));

  const validateForm = () => {
    const errors: Record<string, string> = {};
    if (!formData.cvId) errors.cvId = 'Please select a CV';
    if (!formData.name.trim()) errors.name = 'Campaign name is required';
    if (!formData.subject.trim()) errors.subject = 'Subject is required';
    if (!formData.bodyTemplate.trim()) errors.bodyTemplate = 'Email body is required';
    if (formData.minMatchScore < 0 || formData.minMatchScore > 1) {
      errors.minMatchScore = 'Score must be between 0 and 1';
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateCampaign = async () => {
    if (!validateForm()) return;

    const result = await dispatch(createCampaign(formData));
    if (createCampaign.fulfilled.match(result)) {
      toast.success('Campaign created successfully');
      dispatch(closeModal());
      resetForm();
    }
  };

  const handleExecute = async (campaignId: string) => {
    const result = await dispatch(executeCampaign(campaignId));
    if (executeCampaign.fulfilled.match(result)) {
      toast.success('Campaign execution started');
      dispatch(fetchCampaigns({}));
    }
  };

  const handleCancel = async (campaignId: string) => {
    const result = await dispatch(cancelCampaign(campaignId));
    if (cancelCampaign.fulfilled.match(result)) {
      toast.success('Campaign cancelled');
      dispatch(fetchCampaigns({}));
    }
  };

  const handleViewLogs = (campaign: EmailCampaignResponse) => {
    setSelectedCampaign(campaign);
    dispatch(fetchCampaignLogs({ campaignId: campaign.id }));
    dispatch(openModal({ type: 'viewLogs', data: campaign }));
  };

  const resetForm = () => {
    setFormData({
      cvId: '',
      name: '',
      subject: '',
      bodyTemplate: '',
      minMatchScore: 0.5,
    });
    setFormErrors({});
  };

  const defaultBodyTemplate = `Dear {{professor_name}},

I am writing to express my interest in potential research opportunities at {{university}}.

Based on my background in {{matched_keywords}}, I believe there could be a strong alignment with your research interests.

I would welcome the opportunity to discuss how my skills and experience could contribute to your work.

Best regards`;

  if (!currentTenant) {
    return (
      <EmptyState
        title="No organization selected"
        description="Please select an organization to manage campaigns."
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Email Campaigns</h1>
          <p className="text-sm text-gray-500 mt-1">
            Create and manage outreach campaigns to matched professors
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            icon={<RefreshCw className="w-4 h-4" />}
            onClick={() => dispatch(fetchCampaigns({}))}
            loading={loading}
          >
            Refresh
          </Button>
          <Button
            icon={<Plus className="w-4 h-4" />}
            onClick={() => dispatch(openModal({ type: 'createCampaign' }))}
          >
            New Campaign
          </Button>
        </div>
      </div>

      {/* SMTP Warning */}
      {!smtpAccount && (
        <Alert variant="warning" title="SMTP not configured">
          Please configure your SMTP settings before executing campaigns.
        </Alert>
      )}

      {/* Campaigns List */}
      {loading && campaigns.length === 0 ? (
        <div className="flex items-center justify-center h-64">
          <Spinner size="lg" />
        </div>
      ) : campaigns.length === 0 ? (
        <EmptyState
          icon={<Mail className="w-6 h-6" />}
          title="No campaigns yet"
          description="Create your first email campaign to reach out to matched professors."
          action={
            <Button
              icon={<Plus className="w-4 h-4" />}
              onClick={() => dispatch(openModal({ type: 'createCampaign' }))}
            >
              Create Campaign
            </Button>
          }
        />
      ) : (
        <div className="space-y-4">
          {campaigns.map((campaign) => (
            <Card key={campaign.id}>
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="font-semibold text-gray-900">{campaign.name}</h3>
                    <StatusBadge status={campaign.status} />
                  </div>
                  <p className="text-sm text-gray-500 mb-2">{campaign.subject}</p>
                  <div className="flex flex-wrap gap-4 text-xs text-gray-500">
                    <span className="flex items-center gap-1">
                      <Send className="w-3 h-3" />
                      {campaign.sentCount}/{campaign.totalRecipients} sent
                    </span>
                    {campaign.failedCount > 0 && (
                      <span className="flex items-center gap-1 text-red-500">
                        <AlertCircle className="w-3 h-3" />
                        {campaign.failedCount} failed
                      </span>
                    )}
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      {format(new Date(campaign.createdAt), 'MMM d, yyyy')}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {campaign.status === 'DRAFT' && (
                    <Button
                      size="sm"
                      icon={<Play className="w-4 h-4" />}
                      onClick={() => handleExecute(campaign.id)}
                      loading={executing}
                      disabled={!smtpAccount}
                    >
                      Execute
                    </Button>
                  )}
                  {campaign.status === 'SCHEDULED' && (
                    <Button
                      size="sm"
                      variant="outline"
                      icon={<XCircle className="w-4 h-4" />}
                      onClick={() => handleCancel(campaign.id)}
                    >
                      Cancel
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<Eye className="w-4 h-4" />}
                    onClick={() => handleViewLogs(campaign)}
                  >
                    Logs
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Create Campaign Modal */}
      <Modal
        isOpen={modal.isOpen && modal.type === 'createCampaign'}
        onClose={() => {
          dispatch(closeModal());
          resetForm();
        }}
        title="Create Email Campaign"
        size="lg"
      >
        <div className="space-y-4">
          <Select
            label="Select CV"
            options={cvOptions}
            value={formData.cvId}
            onChange={(e) => setFormData({ ...formData, cvId: e.target.value })}
            error={formErrors.cvId}
            placeholder="Choose a CV..."
          />

          <Input
            label="Campaign Name"
            placeholder="e.g., Fall 2024 Outreach"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            error={formErrors.name}
          />

          <Input
            label="Email Subject"
            placeholder="e.g., Research Collaboration Opportunity"
            value={formData.subject}
            onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
            error={formErrors.subject}
          />

          <Input
            label="Minimum Match Score"
            type="number"
            min="0"
            max="1"
            step="0.1"
            value={formData.minMatchScore.toString()}
            onChange={(e) =>
              setFormData({ ...formData, minMatchScore: parseFloat(e.target.value) || 0 })
            }
            error={formErrors.minMatchScore}
            hint="Only professors with match score above this will be contacted (0-1)"
          />

          <Textarea
            label="Email Body Template"
            placeholder="Enter your email template..."
            value={formData.bodyTemplate}
            onChange={(e) => setFormData({ ...formData, bodyTemplate: e.target.value })}
            error={formErrors.bodyTemplate}
            rows={8}
          />

          <Alert variant="info">
            <p className="text-xs">
              Available template variables: <code>{'{{professor_name}}'}</code>,{' '}
              <code>{'{{university}}'}</code>, <code>{'{{match_score}}'}</code>,{' '}
              <code>{'{{matched_keywords}}'}</code>
            </p>
          </Alert>

          {!formData.bodyTemplate && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setFormData({ ...formData, bodyTemplate: defaultBodyTemplate })}
            >
              Use default template
            </Button>
          )}

          <div className="flex justify-end gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => {
                dispatch(closeModal());
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button onClick={handleCreateCampaign} loading={creating}>
              Create Campaign
            </Button>
          </div>
        </div>
      </Modal>

      {/* View Logs Modal */}
      <Modal
        isOpen={modal.isOpen && modal.type === 'viewLogs'}
        onClose={() => {
          dispatch(closeModal());
          setSelectedCampaign(null);
        }}
        title={`Campaign Logs - ${selectedCampaign?.name}`}
        size="xl"
      >
        {logsLoading ? (
          <div className="flex items-center justify-center py-8">
            <Spinner size="lg" />
          </div>
        ) : logs.length === 0 ? (
          <EmptyState
            title="No logs yet"
            description="Email logs will appear here once the campaign is executed."
          />
        ) : (
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {logs.map((log) => (
              <LogEntry key={log.id} log={log} />
            ))}
            {logsPagination.totalElements > logs.length && (
              <p className="text-sm text-center text-gray-500 py-2">
                Showing {logs.length} of {logsPagination.totalElements} logs
              </p>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};

const LogEntry: React.FC<{ log: EmailLogResponse }> = ({ log }) => (
  <div className="flex items-center justify-between py-2 px-3 bg-gray-50 rounded-lg">
    <div className="flex-1 min-w-0">
      <p className="text-sm font-medium text-gray-900 truncate">{log.recipientEmail}</p>
      {log.errorMessage && (
        <p className="text-xs text-red-500 truncate">{log.errorMessage}</p>
      )}
    </div>
    <div className="flex items-center gap-3">
      {log.sentAt && (
        <span className="text-xs text-gray-500">
          {format(new Date(log.sentAt), 'MMM d, HH:mm')}
        </span>
      )}
      <StatusBadge status={log.status} />
    </div>
  </div>
);
