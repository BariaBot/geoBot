import { HashRouter, Route, Routes } from 'react-router-dom';
import { useTelegramTheme } from './hooks/useTelegramTheme';
import Layout from './components/Layout';
import DiscoveryPage from './pages/DiscoveryPage';
import ProfilePage from './pages/ProfilePage';
import OnboardingPage from './pages/OnboardingPage';
import ChatPage from './pages/ChatPage';
import VipPage from './pages/VipPage';

export default function App() {
  useTelegramTheme();

  return (
    <HashRouter>
      <Routes>
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route element={<Layout />}>
          <Route path="/" element={<DiscoveryPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/vip" element={<VipPage />} />
        </Route>
      </Routes>
    </HashRouter>
  );
}
