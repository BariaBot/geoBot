import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './pages/App';
import './shared/styles.css';

const appElement = document.getElementById('root');

if (!appElement) {
  throw new Error('Mini App root element not found');
}

createRoot(appElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
