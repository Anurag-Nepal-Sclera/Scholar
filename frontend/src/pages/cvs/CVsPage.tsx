import React, { useEffect, useCallback, useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchCVs, uploadCV, deleteCV, parseCV, computeMatches } from '@/store/slices/cvSlice';
import { openModal, closeModal } from '@/store/slices/uiSlice';
import {
  Card,
  Button,
  Spinner,
  StatusBadge,
  EmptyState,
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
} from 'lucide-react';
import { format } from 'date-fns';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { CVResponse } from '@/types';

export const CVsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { cvs, loading, uploading, pagination } = useAppSelector((state) => state.cv);
  const { currentTenant } = useAppSelector((state) => state.tenant);
  const { modal } = useAppSelector((state) => state.ui);
  const [selectedCV, setSelectedCV] = useState<CVResponse | null>(null);

  useEffect(() => {
    if (currentTenant) {
      dispatch(fetchCVs({}));
    }
  }, [currentTenant, dispatch]);

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
    const result = await dispatch(parseCV(cvId));
    if (parseCV.fulfilled.match(result)) {
      toast.success('CV parsing started');
      // Refresh list after a delay
      setTimeout(() => dispatch(fetchCVs({})), 2000);
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
          <h1 className="text-2xl font-bold text-gray-900">CV Management</h1>
          <p className="text-sm text-gray-500 mt-1">
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
        className={`border-2 border-dashed transition-colors ${
          isDragActive ? 'border-primary-500 bg-primary-50' : 'border-gray-300'
        } ${uploading ? 'opacity-50 pointer-events-none' : ''}`}
      >
        <div
          {...getRootProps()}
          className="flex flex-col items-center justify-center py-8 cursor-pointer"
        >
          <input {...getInputProps()} />
          {uploading ? (
            <>
              <Spinner size="lg" />
              <p className="mt-4 text-sm text-gray-600">Uploading...</p>
            </>
          ) : (
            <>
              <div className="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mb-4">
                <Upload className="w-8 h-8 text-primary-600" />
              </div>
              <p className="text-lg font-medium text-gray-900">
                {isDragActive ? 'Drop your CV here' : 'Drag & drop your CV'}
              </p>
              <p className="text-sm text-gray-500 mt-1">or click to browse</p>
              <p className="text-xs text-gray-400 mt-2">
                Supports PDF and DOCX files up to 10MB
              </p>
            </>
          )}
        </div>
      </Card>

      {/* CV List */}
      {loading && cvs.length === 0 ? (
        <div className="flex items-center justify-center h-32">
          <Spinner size="lg" />
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
            <Card key={cv.id} className="flex items-center gap-4">
              <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center flex-shrink-0">
                <FileText className="w-5 h-5 text-gray-500" />
              </div>
              
              <div className="flex-1 min-w-0">
                <p className="font-medium text-gray-900 truncate">{cv.originalFilename}</p>
                <div className="flex items-center gap-3 text-xs text-gray-500 mt-1">
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

              <StatusBadge status={cv.parsingStatus} />

              <div className="flex items-center gap-2">
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
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Pagination Info */}
      {pagination.totalElements > 0 && (
        <p className="text-sm text-gray-500 text-center">
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
        <p className="text-gray-600 mb-6">
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
    </div>
  );
};
