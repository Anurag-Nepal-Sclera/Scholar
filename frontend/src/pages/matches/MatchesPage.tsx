import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchMatches, fetchMatchesAboveThreshold } from '@/store/slices/matchSlice';
import { createCampaign, fetchCampaignLogs } from '@/store/slices/campaignSlice';
import { fetchCVs } from '@/store/slices/cvSlice';
import {
  Card,
  Button,
  LoadingSpinner,
  EmptyState,
  Select,
  Input,
  Badge,
  Modal,
} from '@/components/ui';
import {
  Users,
  Mail,
  Building2,
  GraduationCap,
  RefreshCw,
  Search,
  Filter,
  CheckSquare,
  Square,
  Send,
  CheckCircle2,
} from 'lucide-react';
import {
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import clsx from 'clsx';
import toast from 'react-hot-toast';

export const MatchesPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { matches, loading, pagination } = useAppSelector((state) => state.match);
  const { cvs } = useAppSelector((state) => state.cv);
  const { currentTenant } = useAppSelector((state) => state.tenant);

  const [selectedCvId, setSelectedCvId] = useState<string>(searchParams.get('cvId') || '');
  const [searchTerm, setSearchTerm] = useState('');
  const [minScore, setMinScore] = useState<string>('0');

  // Phase 2: Selection & Generation State
  const [selectedMatchIds, setSelectedMatchIds] = useState<Set<string>>(new Set());
  const [showProgressModal, setShowProgressModal] = useState(false);
  const [generatedCount, setGeneratedCount] = useState(0);
  const [totalToGenerate, setTotalToGenerate] = useState(0);
  const [currentCampaignId, setCurrentCampaignId] = useState<string | null>(null);
  const [generationComplete, setGenerationComplete] = useState(false);

  useEffect(() => {
    if (currentTenant) {
      dispatch(fetchCVs({}));
    }
  }, [currentTenant, dispatch]);

  useEffect(() => {
    if (selectedCvId && currentTenant) {
      const score = parseFloat(minScore);
      if (score > 0) {
        dispatch(fetchMatchesAboveThreshold({ cvId: selectedCvId, minScore: score }));
      } else {
        dispatch(fetchMatches({ cvId: selectedCvId }));
      }
    }
  }, [selectedCvId, minScore, currentTenant, dispatch]);

  // Reset selection when matches change
  useEffect(() => {
    setSelectedMatchIds(new Set());
  }, [matches]);

  const cvOptions = cvs
    .filter((cv) => cv.parsingStatus === 'COMPLETED')
    .map((cv) => ({
      value: cv.id,
      label: cv.originalFilename,
    }));

  const filteredMatches = matches.filter((match) => {
    if (!searchTerm) return true;
    const search = searchTerm.toLowerCase();
    return (
      match.professor.firstName.toLowerCase().includes(search) ||
      match.professor.lastName.toLowerCase().includes(search) ||
      match.professor.email.toLowerCase().includes(search) ||
      match.professor.universityName.toLowerCase().includes(search) ||
      match.professor.department?.toLowerCase().includes(search) ||
      match.matchedKeywords.toLowerCase().includes(search)
    );
  });

  const getScoreColor = (score: number) => {
    if (score >= 0.7) return 'text-green-600 bg-green-100';
    if (score >= 0.4) return 'text-yellow-600 bg-yellow-100';
    return 'text-red-600 bg-red-100';
  };

  // Selection Handlers
  const toggleSelection = (id: string) => {
    const newSelection = new Set(selectedMatchIds);
    if (newSelection.has(id)) {
      newSelection.delete(id);
    } else {
      newSelection.add(id);
    }
    setSelectedMatchIds(newSelection);
  };

  const handleSelectAll = () => {
    const allIds = new Set(filteredMatches.map(m => m.id));
    setSelectedMatchIds(allIds);
  };

  const handleDeselectAll = () => {
    setSelectedMatchIds(new Set());
  };

  // Generation Logic
  const handleGenerateEmails = async () => {
    if (selectedMatchIds.size === 0) return;

    const selectedMatches = filteredMatches.filter(m => selectedMatchIds.has(m.id));
    const minSelectedScore = Math.min(...selectedMatches.map(m => m.matchScore));

    const cv = cvs.find(c => c.id === selectedCvId);
    if (!cv) return;

    const campaignName = `Outreach: ${cv.originalFilename} (${new Date().toLocaleDateString()})`;

    try {
      const result = await dispatch(createCampaign({
        cvId: selectedCvId,
        name: campaignName,
        subject: `Research Inquiry: ${cv.originalFilename}`,
        bodyTemplate: "AI_GENERATED",
        minMatchScore: minSelectedScore || 0,
        matchIds: Array.from(selectedMatchIds)
      })).unwrap();

      setCurrentCampaignId(result.id);
      setTotalToGenerate(result.totalRecipients);
      setGeneratedCount(0);
      setGenerationComplete(false);
      setShowProgressModal(true);
    } catch (error) {
      toast.error("Failed to start email generation");
    }
  };

  // Polling Effect
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (showProgressModal && currentCampaignId && !generationComplete) {
      interval = setInterval(async () => {
        const result = await dispatch(fetchCampaignLogs({ campaignId: currentCampaignId, page: 0, size: 1000 }));
        if (fetchCampaignLogs.fulfilled.match(result)) {
          const logs = result.payload.content;
          const count = logs.filter(l => l.body && l.body !== 'AI_GENERATED').length;
          setGeneratedCount(count);

          if (count >= totalToGenerate) {
            setGenerationComplete(true);
            // Optionally stop polling here, handled by dependency change or !generationComplete check
          }
        }
      }, 2000);
    }
    return () => clearInterval(interval);
  }, [showProgressModal, currentCampaignId, generationComplete, totalToGenerate, dispatch]);


  if (!currentTenant) {
    return (
      <EmptyState
        title="No student selected"
        description="Please select a student to view matches."
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Match Results</h1>
        <p className="text-sm text-gray-500 dark:text-slate-400 mt-1">
          View and filter professor matches based on CV keywords
        </p>
      </div>

      {/* Filters & Actions */}
      <Card>
        <div className="flex flex-col space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <Select
              label="Select CV"
              options={cvOptions}
              value={selectedCvId}
              onChange={(e) => setSelectedCvId(e.target.value)}
              placeholder="Choose a CV..."
            />

            <Input
              label="Minimum Score"
              type="number"
              min="0"
              max="1"
              step="0.1"
              value={minScore}
              onChange={(e) => setMinScore(e.target.value)}
              icon={<Filter className="w-4 h-4" />}
            />

            <Input
              label="Search"
              placeholder="Search professors..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              icon={<Search className="w-4 h-4" />}
            />

            <div className="flex items-end">
              <Button
                variant="outline"
                icon={<RefreshCw className="w-4 h-4" />}
                onClick={() => {
                  if (selectedCvId) {
                    dispatch(fetchMatches({ cvId: selectedCvId }));
                  }
                }}
                loading={loading}
                disabled={!selectedCvId}
              >
                Refresh
              </Button>
            </div>
          </div>

          {/* Selection Controls */}
          {filteredMatches.length > 0 && (
            <div className="flex flex-wrap items-center justify-between gap-4 pt-4 border-t border-gray-100 dark:border-slate-800">
              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={handleSelectAll}
                  disabled={loading}
                >
                  Select All
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={handleDeselectAll}
                  disabled={loading || selectedMatchIds.size === 0}
                >
                  Deselect All
                </Button>
                <span className="text-sm text-gray-500 dark:text-slate-400 ml-2">
                  {selectedMatchIds.size} selected
                </span>
              </div>

              <Button
                variant="primary"
                icon={<Send className="w-4 h-4" />}
                disabled={selectedMatchIds.size === 0}
                onClick={handleGenerateEmails}
              >
                Generate Emails
              </Button>
            </div>
          )}
        </div>
      </Card>

      {/* Results */}
      {!selectedCvId ? (
        <EmptyState
          icon={<Users className="w-6 h-6" />}
          title="Select a CV to view matches"
          description="Choose a CV from the dropdown above to see matching professors."
        />
      ) : loading ? (
        <div className="flex items-center justify-center h-64">
          <LoadingSpinner message="Finding matches..." />
        </div>
      ) : filteredMatches.length === 0 ? (
        <EmptyState
          icon={<Users className="w-6 h-6" />}
          title="No matches found"
          description={
            searchTerm
              ? 'Try adjusting your search filters.'
              : 'No professors match the criteria. Try lowering the minimum score.'
          }
        />
      ) : (
        <>
          <p className="text-sm text-gray-500 dark:text-slate-400">
            Found {filteredMatches.length} matches
          </p>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {filteredMatches.map((match) => {
              const isSelected = selectedMatchIds.has(match.id);
              return (
                <Card
                  key={match.id}
                  className={clsx(
                    "flex flex-col transition-colors border-2",
                    isSelected ? "border-primary-500 bg-primary-50/10" : "border-transparent"
                  )}
                  onClick={() => toggleSelection(match.id)} // Allow clicking card to select? Maybe just checkbox to avoid accidental clicks
                >
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between mb-3 gap-3">
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      {/* Checkbox */}
                      <div
                        className="cursor-pointer text-gray-400 hover:text-primary-600"
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleSelection(match.id);
                        }}
                      >
                        {isSelected ? (
                          <CheckSquare className="w-5 h-5 text-primary-600" />
                        ) : (
                          <Square className="w-5 h-5" />
                        )}
                      </div>

                      <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
                        <span className="text-primary-600 font-medium">
                          {match.professor.firstName[0]}
                          {match.professor.lastName[0]}
                        </span>
                      </div>
                      <div className="min-w-0">
                        <p className="font-medium text-gray-900 dark:text-white truncate">
                          {match.professor.firstName} {match.professor.lastName}
                        </p>
                        <p className="text-sm text-gray-500 dark:text-slate-400 flex items-center gap-1 truncate">
                          <Mail className="w-3 h-3 flex-shrink-0" />
                          {match.professor.email}
                        </p>
                      </div>
                    </div>

                    <div
                      className={clsx(
                        'px-3 py-1 rounded-full text-sm font-bold self-start sm:self-auto',
                        getScoreColor(match.matchScore)
                      )}
                    >
                      {(match.matchScore * 100).toFixed(0)}%
                    </div>
                  </div>

                  <div className="space-y-2 text-sm mb-4 pl-8"> {/* Indent content to align with avatar */}
                    <div className="flex items-center gap-2 text-gray-600 dark:text-slate-300">
                      <Building2 className="w-4 h-4 text-gray-400 dark:text-slate-500" />
                      {match.professor.universityName}
                      {match.professor.universityCountry && (
                        <span className="text-gray-400 dark:text-slate-500">
                          ({match.professor.universityCountry})
                        </span>
                      )}
                    </div>
                    {match.professor.department && (
                      <div className="flex items-center gap-2 text-gray-600 dark:text-slate-300">
                        <GraduationCap className="w-4 h-4 text-gray-400 dark:text-slate-500" />
                        {match.professor.department}
                      </div>
                    )}
                  </div>

                  <div className="border-t border-gray-100 dark:border-slate-800 pt-3 mt-auto pl-8">
                    <p className="text-xs text-gray-500 dark:text-slate-400 mb-2">
                      Matched Keywords ({match.totalMatchedKeywords})
                    </p>
                    <div className="flex flex-wrap gap-1">
                      {match.matchedKeywords.split(',').slice(0, 6).map((keyword, i) => (
                        <Badge key={i} variant="blue" size="sm">
                          {keyword.trim()}
                        </Badge>
                      ))}
                      {match.matchedKeywords.split(',').length > 6 && (
                        <Badge variant="gray" size="sm">
                          +{match.matchedKeywords.split(',').length - 6} more
                        </Badge>
                      )}
                    </div>
                  </div>
                </Card>
              );
            })}
          </div>

          {/* Pagination Controls */}
          {pagination.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-gray-200 dark:border-slate-800 pt-4 mt-4">
              <div className="text-sm text-gray-500 dark:text-slate-400">
                Page {pagination.page + 1} of {pagination.totalPages} ({pagination.totalElements} total items)
              </div>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    dispatch(fetchMatches({ cvId: selectedCvId, page: pagination.page - 1 }));
                  }}
                  disabled={pagination.page === 0 || loading}
                  icon={<ChevronLeft className="w-4 h-4" />}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    dispatch(fetchMatches({ cvId: selectedCvId, page: pagination.page + 1 }));
                  }}
                  disabled={pagination.page >= pagination.totalPages - 1 || loading}
                >
                  Next
                  <ChevronRight className="w-4 h-4 ml-2" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Generation Progress Modal */}
      <Modal
        isOpen={showProgressModal}
        onClose={() => { }} // Non-dismissable while generating
        title={generationComplete ? "Email Generation Completed" : "Generating Emails..."}
      >
        <div className="flex flex-col items-center py-6 text-center">
          {!generationComplete ? (
            <>
              <LoadingSpinner
                size="lg"
                message={`Emails generated: ${generatedCount} / ${totalToGenerate}`}
              />
              <p className="mt-4 text-sm text-gray-500">
                Please wait while we personalize emails for your selected professors using AI.
              </p>
            </>
          ) : (
            <>
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-4">
                <CheckCircle2 className="w-8 h-8 text-green-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 dark:text-white">
                {totalToGenerate} Emails Generated
              </h3>
              <p className="mt-2 text-gray-600 dark:text-slate-300">
                All selected emails have been drafted successfully. You can now review and send them.
              </p>
              <div className="mt-8 w-full">
                <Button
                  variant="primary"
                  className="w-full"
                  onClick={() => {
                    setShowProgressModal(false);
                    navigate('/campaigns'); // Or wherever appropriate
                  }}
                >
                  Review Emails
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>
    </div>
  );
};
