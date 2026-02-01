import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchMatches, fetchMatchesAboveThreshold, setMinScoreFilter } from '@/store/slices/matchSlice';
import { fetchCVs } from '@/store/slices/cvSlice';
import {
  Card,
  Button,
  Spinner,
  EmptyState,
  Select,
  Input,
  Badge,
} from '@/components/ui';
import {
  Users,
  Mail,
  Building2,
  GraduationCap,
  RefreshCw,
  Search,
  Filter,
} from 'lucide-react';
import clsx from 'clsx';

export const MatchesPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const [searchParams] = useSearchParams();
  const { matches, loading, pagination, filters } = useAppSelector((state) => state.match);
  const { cvs } = useAppSelector((state) => state.cv);
  const { currentTenant } = useAppSelector((state) => state.tenant);
  
  const [selectedCvId, setSelectedCvId] = useState<string>(searchParams.get('cvId') || '');
  const [searchTerm, setSearchTerm] = useState('');
  const [minScore, setMinScore] = useState<string>('0');

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
        <h1 className="text-2xl font-bold text-gray-900">Match Results</h1>
        <p className="text-sm text-gray-500 mt-1">
          View and filter professor matches based on CV keywords
        </p>
      </div>

      {/* Filters */}
      <Card>
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
          <Spinner size="lg" />
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
          <p className="text-sm text-gray-500">
            Found {filteredMatches.length} matches
          </p>
          
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {filteredMatches.map((match) => (
              <Card key={match.id} className="flex flex-col">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
                      <span className="text-primary-600 font-medium">
                        {match.professor.firstName[0]}
                        {match.professor.lastName[0]}
                      </span>
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">
                        {match.professor.firstName} {match.professor.lastName}
                      </p>
                      <p className="text-sm text-gray-500 flex items-center gap-1">
                        <Mail className="w-3 h-3" />
                        {match.professor.email}
                      </p>
                    </div>
                  </div>
                  
                  <div
                    className={clsx(
                      'px-3 py-1 rounded-full text-sm font-bold',
                      getScoreColor(match.matchScore)
                    )}
                  >
                    {(match.matchScore * 100).toFixed(0)}%
                  </div>
                </div>

                <div className="space-y-2 text-sm mb-4">
                  <div className="flex items-center gap-2 text-gray-600">
                    <Building2 className="w-4 h-4 text-gray-400" />
                    {match.professor.universityName}
                    {match.professor.universityCountry && (
                      <span className="text-gray-400">
                        ({match.professor.universityCountry})
                      </span>
                    )}
                  </div>
                  {match.professor.department && (
                    <div className="flex items-center gap-2 text-gray-600">
                      <GraduationCap className="w-4 h-4 text-gray-400" />
                      {match.professor.department}
                    </div>
                  )}
                </div>

                <div className="border-t border-gray-100 pt-3 mt-auto">
                  <p className="text-xs text-gray-500 mb-2">
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
            ))}
          </div>
        </>
      )}
    </div>
  );
};
