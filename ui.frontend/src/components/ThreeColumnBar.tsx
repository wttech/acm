import { ReactNode } from 'react';
import './ThreeColumnBar.css';

interface ThreeColumnBarProps {
  left?: ReactNode;
  center?: ReactNode;
  right?: ReactNode;
}

const ThreeColumnBar = ({ left, center, right }: ThreeColumnBarProps) => (
  <div className="three-column-bar">
    <div className="three-column-bar__left">{left}</div>
    <div className="three-column-bar__center">{center}</div>
    <div className="three-column-bar__right">{right}</div>
  </div>
);

export default ThreeColumnBar;
