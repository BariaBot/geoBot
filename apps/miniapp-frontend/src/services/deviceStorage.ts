interface SimpleStorage {
  get: (key: string) => string | null;
  set: (key: string, value: string) => void;
  delete: (key: string) => void;
}

const localStorageAdapter: SimpleStorage = {
  get: (key) => {
    try {
      return window.localStorage.getItem(key);
    } catch (error) {
      console.warn('localStorage get failed', error);
      return null;
    }
  },
  set: (key, value) => {
    try {
      window.localStorage.setItem(key, value);
    } catch (error) {
      console.warn('localStorage set failed', error);
    }
  },
  delete: (key) => {
    try {
      window.localStorage.removeItem(key);
    } catch (error) {
      console.warn('localStorage delete failed', error);
    }
  }
};

export async function withDeviceStorage<T>(callback: (storage: SimpleStorage) => Promise<T> | T): Promise<T> {
  return await callback(localStorageAdapter);
}
