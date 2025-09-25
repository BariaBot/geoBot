import { apiRequest } from './client';

export interface ProfilePayload {
  displayName: string;
  bio?: string;
  gender?: string;
  birthday?: string;
  city?: string;
  interests?: string[];
  latitude?: number;
  longitude?: number;
}

export interface ProfileLocation {
  latitude: number;
  longitude: number;
  updatedAt: string;
}

export interface ProfileResponse {
  telegramId: number;
  displayName: string;
  bio: string | null;
  gender: string | null;
  birthday: string | null;
  city: string | null;
  vip: boolean;
  vipUntil: string | null;
  location: ProfileLocation | null;
  interests: string[];
}

export async function fetchProfile(): Promise<ProfileResponse> {
  return apiRequest<ProfileResponse>('/profiles/me', {
    credentials: 'include',
  });
}

export async function updateProfile(payload: ProfilePayload): Promise<ProfileResponse> {
  return apiRequest<ProfileResponse>('/profiles/me', {
    method: 'PUT',
    body: JSON.stringify(payload),
    credentials: 'include',
  });
}
