import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import UserCard from '../components/UserCard';

describe('UserCard', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders skeleton state when loading', () => {
    const { container } = render(<UserCard isLoading onLike={() => {}} onSkip={() => {}} />);
    expect(container.querySelector('[data-loading="true"]')).toBeInTheDocument();
  });

  it('renders placeholder when username missing', () => {
    render(<UserCard onLike={() => {}} onSkip={() => {}} />);
    expect(screen.getByText('Нет анкет поблизости')).toBeInTheDocument();
  });

  it('renders photos with pagination and allows manual selection', () => {
    render(
      <UserCard
        username="Мария"
        age={27}
        photos={[
          'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80',
          'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=80',
        ]}
        bio="Люблю путешествия и кофе."
        interests={['еда', 'кино']}
        onLike={() => {}}
        onSkip={() => {}}
      />,
    );

    const dots = screen.getAllByRole('button', { name: /Фото \d+ из 2/ });
    expect(dots).toHaveLength(2);
    expect(dots[0].getAttribute('data-active')).toBe('true');

    fireEvent.click(dots[1]);
    expect(dots[1].getAttribute('data-active')).toBe('true');
    expect(dots[0].getAttribute('data-active')).toBe('false');
  });

  it('triggers like and skip handlers', () => {
    const handleLike = vi.fn();
    const handleSkip = vi.fn();

    render(
      <UserCard
        username='Олег'
        photos={['https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80']}
        onLike={handleLike}
        onSkip={handleSkip}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Лайк' }));
    fireEvent.click(screen.getByRole('button', { name: 'Пропустить' }));

    expect(handleLike).toHaveBeenCalledTimes(1);
    expect(handleSkip).toHaveBeenCalledTimes(1);
  });
});
