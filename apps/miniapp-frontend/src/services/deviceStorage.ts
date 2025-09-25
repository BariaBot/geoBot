import { CloudStorage as TelegramCloudStorage, initCloudStorage } from '@tma.js/sdk';

export const DEVICE_STORAGE_LIMIT_BYTES = 1024 * 1024;
const DEVICE_STORAGE_LIMIT_MB = DEVICE_STORAGE_LIMIT_BYTES / (1024 * 1024);

export type DeviceStorageErrorCode = 'PAYLOAD_TOO_LARGE';

interface DeviceStorageErrorOptions extends ErrorOptions {
  code?: DeviceStorageErrorCode;
}

export class DeviceStorageError extends Error {
  readonly code?: DeviceStorageErrorCode;

  constructor(message: string, options?: DeviceStorageErrorOptions) {
    super(message, options);
    this.name = 'DeviceStorageError';
    this.code = options?.code;
  }
}

let cachedCloudStorage: TelegramCloudStorage | undefined;
let disposeCloudStorage: (() => void) | undefined;

const resetCloudStorageCache = () => {
  cachedCloudStorage = undefined;
  if (disposeCloudStorage) {
    disposeCloudStorage();
    disposeCloudStorage = undefined;
  }
};

const ensureCloudStorage = (): TelegramCloudStorage | undefined => {
  if (typeof window === 'undefined') {
    return undefined;
  }

  if (cachedCloudStorage) {
    return cachedCloudStorage;
  }

  const telegram = (window as any)?.Telegram;
  if (!telegram?.WebApp) {
    return undefined;
  }

  try {
    const [cloudStorage, cleanup] = initCloudStorage();
    if (!cloudStorage.supports('set')
      || !cloudStorage.supports('get')
      || !cloudStorage.supports('delete')) {
      cleanup();
      return undefined;
    }

    cachedCloudStorage = cloudStorage;
    disposeCloudStorage = cleanup;
  } catch (error) {
    logWarning('CloudStorage init failed', error);
  }

  return cachedCloudStorage;
};

const validatePayloadSize = (value: string) => {
  const bytes = new TextEncoder().encode(value);
  const { length } = bytes;
  if (length > DEVICE_STORAGE_LIMIT_BYTES) {
    const limitMessage = `Размер данных превышает ${DEVICE_STORAGE_LIMIT_MB} MB лимит CloudStorage`;
    throw new DeviceStorageError(limitMessage, { code: 'PAYLOAD_TOO_LARGE' });
  }
};

const localStorageAdapter = () => {
  if (typeof window === 'undefined' || !('localStorage' in window)) {
    throw new DeviceStorageError('localStorage недоступен');
  }
  return window.localStorage;
};

const logWarning = (message: string, error: unknown) => {
  if (error instanceof Error) {
    console.warn(message, error);
  } else {
    console.warn(message, new Error(String(error)));
  }
};

const normalizeError = (error: unknown, fallbackMessage: string): Error => {
  if (error instanceof Error) {
    return error;
  }

  const message = typeof error === 'string' && error.length > 0
    ? error
    : fallbackMessage;

  return new Error(message);
};

const isCloudStorageAvailableInternal = (): boolean => Boolean(getCloudStorage());
const getCloudStorage = (): TelegramCloudStorage | undefined => ensureCloudStorage();

const toDeviceStorageError = (message: string, error: unknown): DeviceStorageError => {
  if (error instanceof DeviceStorageError) {
    return error;
  }

  if (error instanceof Error) {
    const maybeCode = (error as DeviceStorageError).code;
    return new DeviceStorageError(message, {
      cause: error,
      code: maybeCode,
    });
  }

  return new DeviceStorageError(message);
};

const DeviceStorage = {
  async setItem(key: string, value: string): Promise<void> {
    validatePayloadSize(value);
    const storage = getCloudStorage();
    const writeLocal = () => localStorageAdapter().setItem(key, value);
    if (storage) {
      try {
        await storage.set(key, value);
        return;
      } catch (error) {
        logWarning('CloudStorage setItem failed', error);
        try {
          writeLocal();
          return;
        } catch (localError) {
          logWarning('localStorage set failed', localError);
          const aggregate = new AggregateError(
            [normalizeError(error, 'CloudStorage setItem failed'), normalizeError(localError, 'localStorage set failed')],
            'Не удалось сохранить значение',
          );
          throw new DeviceStorageError('Не удалось сохранить значение', { cause: aggregate });
        }
      }
    }

    try {
      writeLocal();
    } catch (error) {
      logWarning('localStorage set failed', error);
      throw toDeviceStorageError('localStorage set failed', error);
    }
  },

  async getItem(key: string): Promise<string | null> {
    const storage = getCloudStorage();
    const readLocal = () => localStorageAdapter().getItem(key);
    if (storage) {
      try {
        const values = await storage.get([key]);
        if (Object.prototype.hasOwnProperty.call(values, key)) {
          return values[key];
        }
        return null;
      } catch (error) {
        logWarning('CloudStorage getItem failed', error);
        try {
          return readLocal();
        } catch (localError) {
          logWarning('localStorage get failed', localError);
          const aggregate = new AggregateError(
            [normalizeError(error, 'CloudStorage getItem failed'), normalizeError(localError, 'localStorage get failed')],
            'Не удалось получить значение',
          );
          throw new DeviceStorageError('Не удалось получить значение', { cause: aggregate });
        }
      }
    }

    try {
      return readLocal();
    } catch (error) {
      logWarning('localStorage get failed', error);
      throw toDeviceStorageError('localStorage get failed', error);
    }
  },

  async removeItem(key: string): Promise<void> {
    const storage = getCloudStorage();
    const removeLocal = () => localStorageAdapter().removeItem(key);
    if (storage) {
      try {
        await storage.delete(key);
        return;
      } catch (error) {
        logWarning('CloudStorage removeItem failed', error);
        try {
          removeLocal();
          return;
        } catch (localError) {
          logWarning('localStorage delete failed', localError);
          const aggregate = new AggregateError(
            [normalizeError(error, 'CloudStorage removeItem failed'), normalizeError(localError, 'localStorage delete failed')],
            'Не удалось удалить значение',
          );
          throw new DeviceStorageError('Не удалось удалить значение', { cause: aggregate });
        }
      }
    }

    try {
      removeLocal();
    } catch (error) {
      logWarning('localStorage delete failed', error);
      throw toDeviceStorageError('localStorage delete failed', error);
    }
  },

  async setJSON<T>(key: string, payload: T): Promise<void> {
    const value = JSON.stringify(payload ?? null);
    await this.setItem(key, value);
  },

  async getJSON<T>(key: string): Promise<T | null> {
    const raw = await this.getItem(key);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as T;
    } catch (error) {
      logWarning('DeviceStorage JSON parse failed', error);
      throw new DeviceStorageError('Ошибка парсинга JSON из DeviceStorage');
    }
  },

  async clear(keys: string[]): Promise<void> {
    await Promise.all(keys.map((key) => this.removeItem(key)));
  },

  isCloudStorageAvailable(): boolean {
    return isCloudStorageAvailableInternal();
  },
};

export default DeviceStorage;

export const __internal = {
  resetCloudStorageCache,
};

interface AsyncStorageAdapter {
  get: (key: string) => Promise<string | null>;
  set: (key: string, value: string) => Promise<void>;
  delete: (key: string) => Promise<void>;
}

const asyncAdapter: AsyncStorageAdapter = {
  get: (key) => DeviceStorage.getItem(key),
  set: (key, value) => DeviceStorage.setItem(key, value),
  delete: (key) => DeviceStorage.removeItem(key),
};

export async function withDeviceStorage<T>(
  callback: (storage: AsyncStorageAdapter) => Promise<T>,
): Promise<T> {
  return callback(asyncAdapter);
}
