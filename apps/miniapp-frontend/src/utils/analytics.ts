export type AnalyticsEvent =
  | 'profile_loaded'
  | 'profile_saved'
  | 'swipe_like'
  | 'swipe_dislike'
  | 'swipe_undo'
  | 'swipe_match'
  | 'match_shown'
  | 'device_storage_error';

export function trackEvent(event: AnalyticsEvent, payload?: Record<string, unknown>) {
  console.info('[analytics]', event, payload ?? {});
}
