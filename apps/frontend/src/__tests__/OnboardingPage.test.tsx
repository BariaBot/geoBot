import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';
import OnboardingPage from '../pages/OnboardingPage';
import { useToastStore } from '../store/useToastStore';

const LocationDisplay = () => {
  const location = useLocation();
  return <div data-testid="location-display">{location.pathname}</div>;
};

const renderOnboarding = () =>
  render(
    <MemoryRouter initialEntries={['/onboarding']}>
      <Routes>
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/" element={<div>Home</div>} />
      </Routes>
      <LocationDisplay />
    </MemoryRouter>,
  );

describe('OnboardingPage', () => {
  const originalFetch = globalThis.fetch;
  const originalGeolocation = navigator.geolocation;

  beforeEach(() => {
    vi.restoreAllMocks();
    window.localStorage.clear();
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({}) });
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: {
        getCurrentPosition: (_success?: PositionCallback, error?: PositionErrorCallback) => {
          error?.({} as GeolocationPositionError);
        },
      },
    });
  });

  afterEach(() => {
    cleanup();
    globalThis.fetch = originalFetch;
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: originalGeolocation,
    });
  });

  it('validates first step before continuing', async () => {
    const toastSpy = vi.spyOn(useToastStore.getState(), 'show');
    renderOnboarding();
    await screen.findByText('Основные данные');

    fireEvent.click(screen.getByRole('button', { name: 'Далее' }));

    await waitFor(() => expect(toastSpy).toHaveBeenCalled());
    expect(screen.getByText('Основные данные')).toBeInTheDocument();
  });

  it('walks through steps with valid data', async () => {
    renderOnboarding();
    await screen.findByText('Основные данные');

    fireEvent.change(screen.getByLabelText('Имя или никнейм'), { target: { value: 'Анна' } });
    const birthDate = new Date();
    birthDate.setFullYear(birthDate.getFullYear() - 25);
    fireEvent.change(screen.getByLabelText('Дата рождения'), {
      target: { value: birthDate.toISOString().split('T')[0] },
    });
    fireEvent.change(screen.getByLabelText('Пол'), { target: { value: 'female' } });

    fireEvent.click(screen.getByRole('button', { name: 'Далее' }));
    expect(await screen.findByText('Добавьте фото')).toBeInTheDocument();

    const file = new File(['data'], 'photo.png', { type: 'image/png' });
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [file] } });

    await waitFor(() => expect(screen.getByAltText('photo.png')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Далее' }));
    expect(await screen.findByText('Интересы и цели')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('О себе'), { target: { value: 'Люблю путешествовать и фотографировать.' } });
    fireEvent.change(screen.getByLabelText('Интересы (через запятую)'), { target: { value: 'путешествия,музыка' } });
    fireEvent.change(screen.getByLabelText('Цели знакомства'), { target: { value: 'Серьёзные отношения' } });
    fireEvent.click(screen.getByRole('button', { name: 'Далее' }));

    expect(await screen.findByText('География знакомств')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Указать вручную' }));
    fireEvent.change(screen.getByLabelText('Город'), { target: { value: 'Москва' } });
    fireEvent.change(screen.getByLabelText('Радиус поиска (км)'), { target: { value: '15' } });
    await waitFor(() => {
      expect(screen.getByDisplayValue('Москва')).toBeInTheDocument();
      expect(screen.getByDisplayValue('15')).toBeInTheDocument();
    });

    const fetchMock = globalThis.fetch as unknown as Mock;
    fetchMock.mockReset();
    fetchMock.mockResolvedValueOnce({ ok: true });
    fetchMock.mockResolvedValueOnce({ ok: true });

    fireEvent.click(screen.getByRole('button', { name: 'Завершить' }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.getByTestId('location-display').textContent).toBe('/'));
  });
});
