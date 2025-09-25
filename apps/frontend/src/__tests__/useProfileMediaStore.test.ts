import { describe, expect, it } from 'vitest';
import { useProfileMediaStore } from '../store/useProfileMediaStore';

describe('useProfileMediaStore', () => {
  it('sets and clears upload status', () => {
    const unsubscribe = useProfileMediaStore.subscribe(() => {});
    useProfileMediaStore.getState().clear();

    useProfileMediaStore.getState().setStatus('photo-1', 'uploading', { progress: 0.4 });
    expect(useProfileMediaStore.getState().uploads['photo-1']).toEqual({ status: 'uploading', progress: 0.4 });

    useProfileMediaStore.getState().setStatus('photo-1', 'error', { error: 'failed' });
    expect(useProfileMediaStore.getState().uploads['photo-1']).toEqual({ status: 'error', error: 'failed' });

    useProfileMediaStore.getState().clear('photo-1');
    expect(useProfileMediaStore.getState().uploads['photo-1']).toBeUndefined();

    unsubscribe();
  });
});

