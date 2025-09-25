const BASE64_PREFIX = /^data:(.*);base64,/;

export const generatePhotoId = () => `photo-${crypto.randomUUID?.() ?? Date.now().toString(16)}`;

export const readFileAsDataUrl = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(reader.error ?? new Error('Не удалось прочитать файл'));
    reader.readAsDataURL(file);
  });

const decodeBase64ToArrayBuffer = (base64: string): ArrayBuffer => {
  if (typeof window !== 'undefined' && typeof window.atob === 'function') {
    const binary = window.atob(base64);
    const buffer = new ArrayBuffer(binary.length);
    const bytes = new Uint8Array(buffer);
    for (let i = 0; i < binary.length; i += 1) {
      bytes[i] = binary.charCodeAt(i);
    }
    return buffer;
  }

  const bufferCtor = (globalThis as unknown as Record<string, unknown>).Buffer as
    | undefined
    | { from: (input: string, encoding: string) => { [key: number]: number; length: number } };
  if (bufferCtor) {
    const nodeBuffer = bufferCtor.from(base64, 'base64');
    const buffer = new ArrayBuffer(nodeBuffer.length);
    const bytes = new Uint8Array(buffer);
    for (let i = 0; i < nodeBuffer.length; i += 1) {
      bytes[i] = nodeBuffer[i] as number;
    }
    return buffer;
  }

  throw new Error('Base64 decoding is not supported in this environment');
};

export const dataUrlToFile = (dataUrl: string, fallbackName: string) => {
  const match = dataUrl.match(BASE64_PREFIX);
  if (!match) {
    throw new Error('Некорректный формат изображения');
  }
  const mimeType = match[1] || 'image/jpeg';
  const base64 = dataUrl.replace(BASE64_PREFIX, '');
  const buffer = decodeBase64ToArrayBuffer(base64);
  const extension = mimeType.split('/')[1] ?? 'jpg';
  const fileName = fallbackName.endsWith(extension) ? fallbackName : `${fallbackName}.${extension}`;
  return new File([buffer], fileName, { type: mimeType });
};

export interface DraftPhoto {
  id: string;
  name: string;
  type: string;
  size: number;
  dataUrl: string;
  remoteUrl?: string;
  status?: 'idle' | 'uploading' | 'uploaded' | 'error';
  isLocal?: boolean;
  error?: string;
}

export const createDraftPhoto = async (file: File): Promise<DraftPhoto> => {
  const dataUrl = await readFileAsDataUrl(file);
  return {
    id: generatePhotoId(),
    name: file.name,
    type: file.type || 'image/jpeg',
    size: file.size,
    dataUrl,
    status: 'idle',
    isLocal: true,
  };
};

export const maxPhotoSizeBytes = 1 * 1024 * 1024; // 1 MB
