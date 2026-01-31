import React from 'react';
import clsx from 'clsx';
import { AlertCircle, CheckCircle, Info, XCircle } from 'lucide-react';

type AlertVariant = 'info' | 'success' | 'warning' | 'error';

interface AlertProps {
  variant?: AlertVariant;
  title?: string;
  children: React.ReactNode;
  className?: string;
}

const variants: Record<AlertVariant, { bg: string; border: string; icon: string; title: string }> = {
  info: {
    bg: 'bg-blue-50',
    border: 'border-blue-200',
    icon: 'text-blue-500',
    title: 'text-blue-800',
  },
  success: {
    bg: 'bg-green-50',
    border: 'border-green-200',
    icon: 'text-green-500',
    title: 'text-green-800',
  },
  warning: {
    bg: 'bg-yellow-50',
    border: 'border-yellow-200',
    icon: 'text-yellow-500',
    title: 'text-yellow-800',
  },
  error: {
    bg: 'bg-red-50',
    border: 'border-red-200',
    icon: 'text-red-500',
    title: 'text-red-800',
  },
};

const icons: Record<AlertVariant, React.FC<{ className?: string }>> = {
  info: Info,
  success: CheckCircle,
  warning: AlertCircle,
  error: XCircle,
};

export const Alert: React.FC<AlertProps> = ({
  variant = 'info',
  title,
  children,
  className,
}) => {
  const styles = variants[variant];
  const Icon = icons[variant];

  return (
    <div
      className={clsx(
        'rounded-lg border p-4',
        styles.bg,
        styles.border,
        className
      )}
    >
      <div className="flex gap-3">
        <Icon className={clsx('w-5 h-5 flex-shrink-0 mt-0.5', styles.icon)} />
        <div>
          {title && (
            <h4 className={clsx('font-medium mb-1', styles.title)}>{title}</h4>
          )}
          <div className="text-sm text-gray-700">{children}</div>
        </div>
      </div>
    </div>
  );
};
