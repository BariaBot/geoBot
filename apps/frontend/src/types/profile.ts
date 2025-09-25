export interface ProfilePhoto {
  id: string;
  url: string;
  order: number;
  isPrimary?: boolean;
}

export interface ProfilePreferences {
  radiusKm: number;
  city?: string;
  locationMode: 'auto' | 'manual';
  geo?: {
    lat: number;
    lon: number;
  };
}

export interface ProfileData {
  username: string;
  displayName?: string;
  bio?: string;
  interests?: string;
  goals?: string;
  photos?: ProfilePhoto[];
  preferences?: ProfilePreferences;
}

export interface ProfileUpdatePayload {
  displayName: string;
  bio: string;
  interests: string;
  goals: string;
  preferences: ProfilePreferences;
}

export interface UploadPhotoResponse {
  id: string;
  url: string;
  order: number;
}

