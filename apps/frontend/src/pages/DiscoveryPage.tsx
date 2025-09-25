import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import UserCard from '../components/UserCard';
import { useAuthStore } from '../store/useAuthStore';
import { useToastStore } from '../store/useToastStore';

interface Profile {
  userId: number;
  user: {
    username: string;
    age?: number;
    photos?: string[];
    interests?: string[];
  };
  bio?: string;
  age?: number;
  interests?: string[];
  photos?: string[];
  distanceKm?: number;
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

  if (isLoading || !data) {
    return (
      <div className="discovery-card">
        <UserCard
          isLoading
          onLike={() => {}}
          onSkip={() => {}}
        />
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="discovery-card">
        <UserCard onLike={() => {}} onSkip={refetch} />
      </div>
    );
  }

  const profile = data[0];
  const photos = profile.photos?.length
    ? profile.photos
    : profile.user.photos?.length
      ? profile.user.photos
      : [];
  const interests = profile.interests?.length
    ? profile.interests
    : profile.user.interests?.length
      ? profile.user.interests
      : [];

  const handleSkip = () => {
    showToast('Пропущено');
    refetch();
  };

  return (
    <div className="discovery-card">
      <UserCard
        username={profile.user.username}
        age={profile.age ?? profile.user.age}
        bio={profile.bio}
        interests={interests}
        photos={photos}
        distanceKm={profile.distanceKm}
        onLike={() => like.mutate(profile.userId)}
        onSkip={handleSkip}
      />
    </div>
  );
}
