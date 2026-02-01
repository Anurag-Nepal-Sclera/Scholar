import { useEffect } from 'react';
import { useAppSelector } from '@/store/hooks';

export const ThemeWatcher = () => {
    const theme = useAppSelector((state) => state.ui.theme);

    useEffect(() => {
        if (theme === 'dark') {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
        }
    }, [theme]);

    return null;
};
