import { useCallback, useEffect, useState } from 'react';
import { init } from '@telegram-apps/sdk';
import { useTelegramBridge } from '../hooks/useTelegramBridge';
import { useProfileStore, type ProfileDraft } from '../store/profile';
import { LoaderScreen } from '../components/LoaderScreen';
import { ProfileForm } from '../components/ProfileForm';
import { useSwipeQueue } from '../hooks/useSwipeQueue';
import { SwipeDeck } from '../components/SwipeDeck';
import { useMatchStore } from '../store/match';
import { MatchModal } from '../components/MatchModal';
import { applyThemeTokens } from '../theme';

const TELEGRAM_INIT_TIMEOUT_MS = 4000;

type ActiveTab = 'discover' | 'profile';

const App = () => {
  const [ready, setReady] = useState(false);
  const [activeTab, setActiveTab] = useState<ActiveTab>('discover');
  const { initBridge, bridgeReady, themeParams } = useTelegramBridge();
  const profileStore = useProfileStore();
  const {
    status, profile, draft, updateDraft, submitDraft, initialise, error,
  } = profileStore;
  const swipeQueue = useSwipeQueue(status === 'ready' && Boolean(profile?.telegramId));
  const { activeMatch, dismissMatch, hydrate } = useMatchStore((state) => ({
    activeMatch: state.activeMatch,
    dismissMatch: state.dismissMatch,
    hydrate: state.hydrate,
  }));

  useEffect(() => {
    hydrate().catch((err) => {
      console.error('Failed to hydrate matches', err);
    });
  }, [hydrate]);

  useEffect(() => {
    const controller = new AbortController();
    const cleanupInit = init({ acceptCustomStyles: true });

    const timeout = setTimeout(() => {
      controller.abort();
      setReady(true);
    }, TELEGRAM_INIT_TIMEOUT_MS);

    const initialiseBridge = async () => {
      try {
        await initBridge(controller.signal);
        if (!controller.signal.aborted) {
          await initialise();
          setReady(true);
        }
      } catch (caughtError) {
        console.error('Failed to initialise mini app', caughtError);
      } finally {
        clearTimeout(timeout);
      }
    };

    initialiseBridge();

    return () => {
      controller.abort();
      cleanupInit();
      clearTimeout(timeout);
    };
  }, [initBridge, initialise]);

  useEffect(() => {
    applyThemeTokens(themeParams);
  }, [themeParams]);

  const loading = !ready || !bridgeReady || status === 'loading';

  const handleDraftChange = useCallback(
    (partial: Partial<ProfileDraft>) => {
      updateDraft(partial).catch((err) => {
        console.error('Failed to update draft', err);
      });
    },
    [updateDraft],
  );

  const handleOpenChat = useCallback(() => {
    if (!activeMatch) return;

    const chatUrl = new URL('/chat', window.location.origin);
    if (activeMatch.matchId) {
      chatUrl.searchParams.set('matchId', activeMatch.matchId);
    }
    chatUrl.searchParams.set('user', String(activeMatch.targetTelegramId));
    const telegram = (window as any)?.Telegram?.WebApp;

    if (telegram?.openLink) {
      // TODO(issue-58): replace deep link once чат страница доступна внутри mini app.
      telegram.openLink(chatUrl.toString(), { try_instant_view: false });
    } else {
      window.open(chatUrl.toString(), '_blank', 'noopener');
    }

    dismissMatch();
  }, [activeMatch, dismissMatch]);

  if (loading) {
    return <LoaderScreen />;
  }

  const showOnboarding = !profile || !profile.displayName?.trim();

  return (
    <>
      <div className="app-shell">
        <header className="app-shell__header">
          <h1>WAU Dating</h1>
          <p>Свайпай, знакомься и встречай тех, кто рядом.</p>
        </header>
        <main className="app-shell__content">
          {showOnboarding ? (
            <section className="onboarding">
              <h2>Расскажи о себе</h2>
              <p>Заполним профиль, чтобы рекомендации стали точнее.</p>
              <ProfileForm
                draft={draft}
                status={status}
                error={error}
                ctaLabel="Продолжить"
                onChange={handleDraftChange}
                onSubmit={submitDraft}
              />
            </section>
          ) : (
            <section className="app-tabs">
              <nav className="app-tabs__nav">
                <button
                  type="button"
                  className={activeTab === 'discover' ? 'app-tabs__button app-tabs__button--active' : 'app-tabs__button'}
                  onClick={() => setActiveTab('discover')}
                >
                  Лента
                </button>
                <button
                  type="button"
                  className={activeTab === 'profile' ? 'app-tabs__button app-tabs__button--active' : 'app-tabs__button'}
                  onClick={() => setActiveTab('profile')}
                >
                  Профиль
                </button>
              </nav>
              <div className="app-tabs__body">
                {activeTab === 'discover' ? (
                  <SwipeDeck
                    items={swipeQueue.items}
                    loading={swipeQueue.loading}
                    error={swipeQueue.error ?? null}
                    undoAvailable={swipeQueue.undoAvailable}
                    onRefresh={() => {
                      swipeQueue.refresh().catch((err) => {
                        console.error('Failed to refresh swipe queue', err);
                      });
                    }}
                    onLike={(item) => {
                      swipeQueue.swipeLike(item).catch((err) => {
                        console.error('Failed to send like', err);
                      });
                    }}
                    onDislike={(item) => {
                      swipeQueue.swipeDislike(item).catch((err) => {
                        console.error('Failed to send dislike', err);
                      });
                    }}
                    onUndo={() => {
                      swipeQueue.undo().catch((err) => {
                        console.error('Failed to undo swipe', err);
                      });
                    }}
                  />
                ) : (
                  <ProfileForm
                    draft={draft}
                    status={status}
                    error={error}
                    ctaLabel="Сохранить"
                    onChange={handleDraftChange}
                    onSubmit={submitDraft}
                  />
                )}
              </div>
            </section>
          )}
        </main>
      </div>
      <MatchModal
        match={activeMatch}
        visible={Boolean(activeMatch)}
        onClose={dismissMatch}
        onOpenChat={handleOpenChat}
      />
    </>
  );
};

export default App;
