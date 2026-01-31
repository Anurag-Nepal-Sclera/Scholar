import React from 'react';
import clsx from 'clsx';
import { AlertTriangle } from 'lucide-react';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  action,
  className,
}) => (
  <div className={clsx('flex flex-col items-center justify-center py-12 px-4 text-center', className)}>
    <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center mb-4 text-gray-400">
      {icon || <AlertTriangle className="w-6 h-6" />}
    </div>
    <h3 className="text-lg font-medium text-gray-900 mb-1">{title}</h3>
    {description && (
      <p className="text-sm text-gray-500 max-w-sm mb-4">{description}</p>
    )}
    {action && <div>{action}</div>}
  </div>
);
