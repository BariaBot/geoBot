import { apiRequest } from './client';

export interface MatchInvitePayload {
  matchId: string | null;
  targetTelegramId: number;
  targetName: string;
}

interface MatchInviteResponse {
  ok: boolean;
}

export async function sendMatchInvite(payload: MatchInvitePayload): Promise<void> {
  try {
    await apiRequest<MatchInviteResponse>('/notifications/match', {
      method: 'POST',
      credentials: 'include',
      body: JSON.stringify(payload),
    });
  } catch (error) {
    console.warn('Match invite webhook failed', error);
    throw error;
  }
}
