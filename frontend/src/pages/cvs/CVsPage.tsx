import React, { useEffect, useCallback, useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchCVs, uploadCV, deleteCV, parseCV, computeMatches, clearParsingCVId } from '@/store/slices/cvSlice';
import { openModal, closeModal } from '@/store/slices/uiSlice';
import {
  Card,
  Button,
  StatusBadge,
  EmptyState,
  LoadingSpinner,
  Modal,
  Alert,
} from '@/components/ui';
import {
  Upload,
  FileText,
  Trash2,
  Play,
  RefreshCw,
  ChevronRight,
  Calendar,
  HardDrive,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { format } from 'date-fns';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { CVResponse } from '@/types';

export const CVsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { cvs, loading, uploading, pagination, parsingCVId } = useAppSelector((state) => state.cv);
  const { currentTenant } = useAppSelector((state) => state.tenant);
  const { modal } = useAppSelector((state) => state.ui);
  const [selectedCV, setSelectedCV] = useState<CVResponse | null>(null);

  useEffect(() => {
    if (currentTenant) {
      dispatch(fetchCVs({}));
    }
  }, [currentTenant, dispatch]);

  useEffect(() => {
    let pollInterval: number | null = null;

    if (parsingCVId) {
      // Show modal initially if not already open
      dispatch(openModal({ type: 'parsingCV', data: null }));

      pollInterval = window.setInterval(async () => {
        const result = await dispatch(fetchCVs({}));
        if (fetchCVs.fulfilled.match(result)) {
          const parsingCv = result.payload.content.find(cv => cv.id === parsingCVId);
          if (parsingCv && (parsingCv.parsingStatus === 'COMPLETED' || parsingCv.parsingStatus === 'FAILED')) {
            if (pollInterval) window.clearInterval(pollInterval);
            dispatch(clearParsingCVId());
            dispatch(openModal({ type: 'parsingComplete', data: parsingCv }));
          }
        }
      }, 3000);
    }

    return () => {
      if (pollInterval) window.clearInterval(pollInterval);
    };
  }, [parsingCVId, dispatch]);

  const onDrop = useCallback(
    async (acceptedFiles: File[]) => {
      for (const file of acceptedFiles) {
        const result = await dispatch(uploadCV(file));
        if (uploadCV.fulfilled.match(result)) {
          toast.success(`${file.name} uploaded successfully`);
        }
      }
    },
    [dispatch]
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
    },
    maxSize: 10 * 1024 * 1024, // 10MB
  });

  const handleDelete = async (cv: CVResponse) => {
    setSelectedCV(cv);
    dispatch(openModal({ type: 'deleteCV', data: cv }));
  };

  const confirmDelete = async () => {
    if (selectedCV) {
      const result = await dispatch(deleteCV(selectedCV.id));
      if (deleteCV.fulfilled.match(result)) {
        toast.success('CV deleted successfully');
      }
    }
    dispatch(closeModal());
    setSelectedCV(null);
  };

  const handleParse = async (cvId: string) => {
    dispatch(openModal({ type: 'parsingCV', data: null }));
    const result = await dispatch(parseCV(cvId));
    if (parseCV.fulfilled.match(result)) {
      toast.success('CV parsing started');
    } else {
      dispatch(closeModal());
    }
  };

  const handleComputeMatches = async (cvId: string) => {
    const result = await dispatch(computeMatches(cvId));
    if (computeMatches.fulfilled.match(result)) {
      toast.success('Match computation started');
      navigate(`/matches?cvId=${cvId}`);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  if (!currentTenant) {
    return (
      <EmptyState
        title="No student selected"
        description="Please select a student to manage CVs."
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">CV Management</h1>
          <p className="text-sm text-gray-500 dark:text-slate-400 mt-1">
            Upload and manage CVs for matching with professors
          </p>
        </div>
        <Button
          icon={<RefreshCw className="w-4 h-4" />}
          variant="outline"
          onClick={() => dispatch(fetchCVs({}))}
          loading={loading}
        >
          Refresh
        </Button>
      </div>

      {/* Upload Zone */}
      <Card
        padding="lg"
        className={`border-2 border-dashed transition-colors ${isDragActive ? 'border-primary-500 bg-primary-50' : 'border-gray-300'
          } ${uploading ? 'opacity-50 pointer-events-none' : ''}`}
      >
        <div
          {...getRootProps()}
          className="flex flex-col items-center justify-center py-8 cursor-pointer"
        >
          <input {...getInputProps()} />
          {uploading ? (
            <>
              <LoadingSpinner size="md" message="Uploading..." showMessage={true} />
            </>
          ) : (
            <>
              <div className="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mb-4">
                <Upload className="w-8 h-8 text-primary-600" />
              </div>
              <p className="text-lg font-medium text-gray-900 dark:text-white">
                {isDragActive ? 'Drop your CV here' : 'Drag & drop your CV'}
              </p>
              <p className="text-sm text-gray-500 dark:text-slate-400 mt-1">or click to browse</p>
              <p className="text-xs text-gray-400 dark:text-slate-500 mt-2">
                Supports PDF and DOCX files up to 10MB
              </p>
            </>
          )}
        </div>
      </Card>

      {/* CV List */}
      {loading && cvs.length === 0 ? (
        <div className="flex items-center justify-center h-32">
          <LoadingSpinner size="md" message="Loading CVs..." showMessage={true} />
        </div>
      ) : cvs.length === 0 ? (
        <EmptyState
          icon={<FileText className="w-6 h-6" />}
          title="No CVs uploaded yet"
          description="Upload your first CV to get started with professor matching."
        />
      ) : (
        <div className="space-y-3">
          {cvs.map((cv) => (
            <Card key={cv.id} className="flex flex-col sm:flex-row sm:items-center gap-4">
              <div className="flex items-center gap-4 flex-1 min-w-0">
                <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center flex-shrink-0">
                  <FileText className="w-5 h-5 text-gray-500" />
                </div>

                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900 dark:text-white truncate">{cv.originalFilename}</p>
                  <div className="flex items-center gap-3 text-xs text-gray-500 dark:text-slate-400 mt-1">
                    <span className="flex items-center gap-1">
                      <HardDrive className="w-3 h-3" />
                      {formatFileSize(cv.fileSizeBytes)}
                    </span>
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      {format(new Date(cv.uploadedAt), 'MMM d, yyyy')}
                    </span>
                    {cv.keywordCount !== undefined && (
                      <span>{cv.keywordCount} keywords</span>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-between sm:justify-end gap-3 pt-3 sm:pt-0 border-t sm:border-t-0 border-gray-100 dark:border-slate-800">
                <StatusBadge status={cv.parsingStatus} />

                <div className="flex items-center gap-2">
                  {(cv.parsingStatus === 'IN_PROGRESS' || (cv.parsingStatus === 'PENDING' && parsingCVId === cv.id)) ? (
                    <div className="flex items-center gap-2 text-primary-600 font-medium animate-pulse bg-primary-50 px-3 py-1 rounded-full border border-primary-100">
                      <LoadingSpinner size="sm" showMessage={false} />
                      <span className="text-xs">Parsing Resume with AI...</span>
                    </div>
                  ) : (
                    <>
                      {cv.parsingStatus === 'PENDING' && (
                        <Button
                          size="sm"
                          variant="outline"
                          icon={<Play className="w-4 h-4" />}
                          onClick={() => handleParse(cv.id)}
                        >
                          Parse
                        </Button>
                      )}
                      {cv.parsingStatus === 'COMPLETED' && (
                        <Button
                          size="sm"
                          variant="primary"
                          icon={<ChevronRight className="w-4 h-4" />}
                          onClick={() => handleComputeMatches(cv.id)}
                        >
                          Find Matches
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="ghost"
                        icon={<Trash2 className="w-4 h-4" />}
                        onClick={() => handleDelete(cv)}
                        className="text-red-600 hover:text-red-700 hover:bg-red-50"
                      />
                    </>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Pagination Info */}
      {pagination.totalElements > 0 && (
        <p className="text-sm text-gray-500 dark:text-slate-400 text-center">
          Showing {cvs.length} of {pagination.totalElements} CVs
        </p>
      )}

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={modal.isOpen && modal.type === 'deleteCV'}
        onClose={() => {
          dispatch(closeModal());
          setSelectedCV(null);
        }}
        title="Delete CV"
      >
        <Alert variant="warning" className="mb-4">
          This action cannot be undone. All associated matches and campaign data will be deleted.
        </Alert>
        <p className="text-gray-600 dark:text-slate-300 mb-6">
          Are you sure you want to delete <strong>{selectedCV?.originalFilename}</strong>?
        </p>
        <div className="flex justify-end gap-3">
          <Button
            variant="outline"
            onClick={() => {
              dispatch(closeModal());
              setSelectedCV(null);
            }}
          >
            Cancel
          </Button>
          <Button variant="danger" onClick={confirmDelete}>
            Delete
          </Button>
        </div>
      </Modal>

      {/* Parsing Modal */}
      <Modal
        isOpen={modal.isOpen && modal.type === 'parsingCV'}
        onClose={() => { }} // Prevent manual close
        title="AI Analysis in Progress"
      >
        <div className="flex flex-col items-center py-6">
          <LoadingSpinner
            message="Parsing Resume with AI"
            subMessage="Our AI is extracting research interests and technical skills."
          />
        </div>
      </Modal>

      {/* Parsing Complete Modal */}
      <Modal
        isOpen={modal.isOpen && modal.type === 'parsingComplete'}
        onClose={() => dispatch(closeModal())}
        title={(modal.data as CVResponse)?.parsingStatus === 'COMPLETED' ? "Analysis Successful" : "Analysis Failed"}
      >
        <div className="flex flex-col items-center py-4 text-center">
          {(modal.data as CVResponse)?.parsingStatus === 'COMPLETED' ? (
            <>
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-4">
                <CheckCircle2 className="w-8 h-8 text-green-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 dark:text-white">Parsing completed</h3>
              <p className="mt-2 text-gray-600 dark:text-slate-300">
                We found <strong>{(modal.data as CVResponse)?.keywordCount}</strong> keywords in <strong>{(modal.data as CVResponse)?.originalFilename}</strong>. You can now use this CV to find the best professor matches.
              </p>
            </>
          ) : (
            <>
              <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mb-4">
                <AlertCircle className="w-8 h-8 text-red-600" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 dark:text-white">Parsing failed</h3>
              <p className="mt-2 text-gray-600 dark:text-slate-300">
                We encountered an error while analyzing <strong>{(modal.data as CVResponse)?.originalFilename}</strong>. Please try again or upload a different file.
              </p>
            </>
          )}
          <div className="mt-8 w-full">
            <Button
              variant="primary"
              className="w-full"
              onClick={() => {
                dispatch(closeModal());
                if ((modal.data as CVResponse)?.parsingStatus === 'COMPLETED') {
                  // Optional: Navigate or take other action
                }
              }}
            >
              Continue
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
