import { Link as SpectrumLink, SpectrumLinkProps } from '@adobe/react-spectrum';
import { Link as RouterLink, useLocation } from 'react-router-dom';

interface AppLinkProps extends SpectrumLinkProps {
    to: string;
    children: React.ReactNode;
}

export function AppLink({ to, children, ...rest }: AppLinkProps) {
    const location = useLocation();
    const isHashRouter = location.hash !== '';

    return (
        <SpectrumLink {...rest}>
            <RouterLink to={isHashRouter ? `#${to}` : to}>
                {children}
            </RouterLink>
        </SpectrumLink>
    );
}
