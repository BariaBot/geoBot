import { NavLink, Outlet } from 'react-router-dom';
import Toast from './Toast';
import { useVipStore } from '../store/useVipStore';

const navItems = [
  { to: '/', label: 'Поиск', icon: '🔍' },
  { to: '/profile', label: 'Профиль', icon: '👤' },
  { to: '/vip', label: 'VIP', icon: '💎' },
];

export default function Layout() {
  const tg = (window as any).Telegram?.WebApp;
  const haptic = () => tg?.HapticFeedback?.impactOccurred('light');
  const isVip = useVipStore((s) => s.isVip);

  return (
    <>
      <Toast />
      <main>
        <Outlet />
      </main>
      <nav>
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end
            onClick={haptic}
            aria-label={item.label}
            className={({ isActive }) => (isActive ? 'active' : undefined)}
          >
            <span className="icon" aria-hidden>{item.icon}</span>
            <span className="label">
              {item.label}
              {item.to === '/vip' && isVip && <span className="nav-badge">VIP</span>}
            </span>
          </NavLink>
        ))}
      </nav>
    </>
  );
}
