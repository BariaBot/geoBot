import { useToastStore } from '../store/useToastStore';

export default function Toast() {
  const message = useToastStore((s) => s.message);
  return (
    <div aria-live="polite" className="toast-container">
      {message && <div className="toast">{message}</div>}
    </div>
  );
}
