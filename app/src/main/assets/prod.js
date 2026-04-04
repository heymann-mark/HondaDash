// PRODUCTION: Touch-only mode for Android head unit
(function(){
  // Load prod CSS
  const link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = 'prod.css';
  document.head.appendChild(link);

  // Block right-click context menu
  document.addEventListener('contextmenu', e => e.preventDefault(), true);

  // Block mouse events on elements that should be touch-only
  // (pointer events still work for both mouse and touch, so we keep those)

  // Prevent double-tap zoom
  let lastTap = 0;
  document.addEventListener('touchend', e => {
    const now = Date.now();
    if (now - lastTap < 300) e.preventDefault();
    lastTap = now;
  }, { passive: false });

  // Prevent pinch zoom on the page (not on canvases that need it)
  document.addEventListener('gesturestart', e => e.preventDefault(), true);
  document.addEventListener('gesturechange', e => e.preventDefault(), true);
})();
