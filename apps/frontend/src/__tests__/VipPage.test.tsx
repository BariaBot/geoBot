import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import VipPage from '../pages/VipPage';
import { useVipStore } from '../store/useVipStore';

const createClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

const originalFetch = globalThis.fetch;

function renderVipPage() {
  const client = createClient();
  return render(
    <QueryClientProvider client={client}>
      <VipPage />
    </QueryClientProvider>,
  );
}

describe('VipPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    useVipStore.getState().reset();
    globalThis.fetch = originalFetch;
  });

  afterEach(() => {
    cleanup();
  });

  it('renders paywall content with features when status is inactive', async () => {
    globalThis.fetch = vi.fn().mockResolvedValueOnce({
      ok: true,
      json: async () => ({ active: false }),
    });

    renderVipPage();

    expect(await screen.findByText('VIP на 30 дней')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Купить за/ })).toBeInTheDocument();
    expect(screen.getByText(/Мгновенные суперконтакты/i)).toBeInTheDocument();
  });

  it('indicates active subscription and disables purchase', async () => {
    globalThis.fetch = vi.fn().mockResolvedValueOnce({
      ok: true,
      json: async () => ({ active: true, expiresAt: '2030-01-02T00:00:00Z' }),
    });

    renderVipPage();

    expect(await screen.findByText('VIP активен')).toBeInTheDocument();
    const purchaseButton = await screen.findByRole('button', { name: 'Вы уже VIP' });
    expect(purchaseButton).toBeDisabled();
    expect(useVipStore.getState().isVip).toBe(true);
  });

  it('calls purchase endpoint and updates vip status', async () => {
    const mockFetch = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ active: false }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ active: true, expiresAt: '2030-02-01T00:00:00Z' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ active: true, expiresAt: '2030-02-01T00:00:00Z' }),
      });

    globalThis.fetch = mockFetch as typeof fetch;

    renderVipPage();

    const button = await screen.findByRole('button', { name: /^Купить за/ });
    await waitFor(() => expect(button).not.toBeDisabled());

    fireEvent.click(button);

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        '/api/stars/purchase',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ planId: 'vip-monthly' }),
        }),
      );
    });

    await waitFor(() => {
      expect(useVipStore.getState().isVip).toBe(true);
    });
  });
});
