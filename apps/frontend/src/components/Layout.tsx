import { NavLink, Outlet } from 'react-router-dom';
import Toast from './Toast';

const navItems = [
  { to: '/', label: 'Поиск', icon: '🔍' },
  { to: '/profile', label: 'Профиль', icon: '👤' },
  { to: '/vip', label: 'VIP', icon: '💎' },
];

export default function Layout() {
  const tg = (window as any).Telegram?.WebApp;
  const haptic = () => tg?.HapticFeedback?.impactOccurred('light');

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
            <span className="label">{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </>
  );
}
