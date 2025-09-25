import { fireEvent, render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { MatchModal } from '../components/MatchModal';
import type { MatchDetails } from '../store/match';

const baseMatch: MatchDetails = {
  matchId: 'match-1',
  name: 'Алиса',
  createdAt: new Date().toISOString(),
  targetTelegramId: 123,
};

describe('MatchModal', () => {
  it('renders match info when visible', () => {
    render(
      <MatchModal
        match={baseMatch}
        visible
        onClose={() => {}}
        onOpenChat={() => {}}
      />,
    );

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Это взаимно/i)).toBeInTheDocument();
    expect(screen.getByText(/Алиса/)).toBeInTheDocument();
  });

  it('fires callbacks when buttons clicked', () => {
    const handleOpenChat = vi.fn();
    const handleClose = vi.fn();

    render(
      <MatchModal
        match={baseMatch}
        visible
        onClose={handleClose}
        onOpenChat={handleOpenChat}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /Перейти в чат/i }));
    fireEvent.click(screen.getByRole('button', { name: /Продолжить знакомиться/i }));

    expect(handleOpenChat).toHaveBeenCalledTimes(1);
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('does not render when not visible', () => {
    render(
      <MatchModal
        match={baseMatch}
        visible={false}
        onClose={() => {}}
        onOpenChat={() => {}}
      />,
    );

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});

