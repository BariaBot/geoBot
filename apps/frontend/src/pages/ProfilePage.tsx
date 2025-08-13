import { useQuery } from '@tanstack/react-query';
import Loader from '../components/Loader';
import { useAuthStore } from '../store/useAuthStore';

export default function ProfilePage() {
  const token = useAuthStore((s) => s.token);
  const { data, isLoading } = useQuery({
    queryKey: ['profile'],
    queryFn: async () => {
      const res = await fetch('/api/profile/me', {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!res.ok) throw new Error('Ошибка загрузки');
      return res.json();
    },
  });

  if (isLoading) return <Loader />;

  return <main>Профиль: {data?.username ?? 'гость'}</main>;
}
