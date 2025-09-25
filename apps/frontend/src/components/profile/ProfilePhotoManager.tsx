import { DragEvent, useRef, useState } from 'react';
import styles from './ProfilePhotoManager.module.css';
import { DraftPhoto } from '../../utils/photos';

interface ProfilePhotoManagerProps {
  photos: DraftPhoto[];
  maxPhotos?: number;
  isUploading?: boolean;
  onUpload: (files: File[]) => void;
  onRemove: (photo: DraftPhoto) => void;
  onReorder: (photos: DraftPhoto[]) => void;
}

const defaultMaxPhotos = 6;

export function ProfilePhotoManager({
  photos,
  maxPhotos = defaultMaxPhotos,
  isUploading,
  onUpload,
  onRemove,
  onReorder,
}: ProfilePhotoManagerProps) {
  const [dragSourceId, setDragSourceId] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);

  const handleDragStart = (event: DragEvent<HTMLDivElement>, id: string) => {
    setDragSourceId(id);
    event.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (event: DragEvent<HTMLDivElement>) => {
    if (dragSourceId) {
      event.preventDefault();
      event.dataTransfer.dropEffect = 'move';
    }
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>, targetId: string) => {
    event.preventDefault();
    if (!dragSourceId || dragSourceId === targetId) return;
    const sourceIndex = photos.findIndex((photo) => photo.id === dragSourceId);
    const targetIndex = photos.findIndex((photo) => photo.id === targetId);
    if (sourceIndex === -1 || targetIndex === -1) return;
    const next = [...photos];
    const [moved] = next.splice(sourceIndex, 1);
    next.splice(targetIndex, 0, moved);
    onReorder(next);
    setDragSourceId(null);
  };

  const handleUploadClick = () => {
    inputRef.current?.click();
  };

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const fileList = event.target.files;
    if (!fileList) return;
    await onUpload(Array.from(fileList));
    event.target.value = '';
  };

  return (
    <div className={styles.container}>
      <div className={styles.grid}>
        {photos.map((photo) => (
          <div
            key={photo.id}
            className={styles.tile}
            draggable
            onDragStart={(event) => handleDragStart(event, photo.id)}
            onDragOver={handleDragOver}
            onDrop={(event) => handleDrop(event, photo.id)}
          >
            <img src={photo.remoteUrl ?? photo.dataUrl} alt={photo.name} />
            <div className={styles.overlay}>
              {photo.status === 'uploading' && <span>Загрузка…</span>}
              {photo.status === 'error' && <span>Ошибка</span>}
            </div>
            <button type="button" className={styles.remove} onClick={() => onRemove(photo)}>
              ✕
            </button>
          </div>
        ))}
        {photos.length < maxPhotos && (
          <button
            type="button"
            className={styles.upload}
            onClick={handleUploadClick}
            disabled={isUploading}
          >
            {isUploading ? 'Загрузка…' : 'Добавить фото'}
          </button>
        )}
      </div>
      <input
        ref={inputRef}
        className={styles.hiddenInput}
        type="file"
        accept="image/*"
        multiple
        onChange={handleFileChange}
      />
    </div>
  );
}

export default ProfilePhotoManager;
