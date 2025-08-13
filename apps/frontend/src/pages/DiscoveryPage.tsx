import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import Loader from '../components/Loader';
import UserCard from '../components/UserCard';
import { useAuthStore } from '../store/useAuthStore';
import { useToastStore } from '../store/useToastStore';

interface Profile {
  userId: number;
  user: { username: string };
  bio?: string;
}

export default function DiscoveryPage() {
  const token = useAuthStore((s) => s.token);
  const [coords, setCoords] = useState<{ lat: number; lon: number } | null>(null);
  const showToast = useToastStore((s) => s.show);

  useEffect(() => {
    navigator.geolocation.getCurrentPosition(
      (p) => setCoords({ lat: p.coords.latitude, lon: p.coords.longitude }),
      () => setCoords({ lat: 0, lon: 0 })
    );
  }, []);

  const { data, isLoading, refetch } = useQuery({
    enabled: !!coords,
    queryKey: ['nearby', coords?.lat, coords?.lon],
    queryFn: async () => {
      const url = `/api/discovery/nearby?lat=${coords!.lat}&lon=${coords!.lon}`;
      const res = await fetch(url, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!res.ok) throw new Error('Ошибка загрузки');
      return (await res.json()) as Profile[];
    },
  });

  const like = useMutation({
    mutationFn: async (userId: number) => {
      await fetch(`/api/like/${userId}`, {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
    },
    onSuccess: () => {
      showToast('Лайк отправлен');
      refetch();
    },
  });

  if (isLoading || !data) return <Loader />;
  if (data.length === 0) return <main>Никого рядом 😔</main>;

  const profile = data[0];

  const handleSkip = () => {
    showToast('Пропущено');
    refetch();
  };

  return (
    <main>
      <UserCard
        username={profile.user.username}
        bio={profile.bio}
        onLike={() => like.mutate(profile.userId)}
        onSkip={handleSkip}
      />
    </main>
  );
}
