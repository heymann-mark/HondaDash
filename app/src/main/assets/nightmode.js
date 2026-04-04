// nightmode.js — include on any page to auto-apply night dimming
// Reads state from localStorage (set by settings.html)
(function(){
  function apply(){
    try {
      const state = JSON.parse(localStorage.getItem('hondadash-night') || '{}');
      if(state.active){
        const sepia = state.redshift ? 0.2 : 0;
        document.documentElement.style.filter = 'brightness('+state.brightness+') sepia('+sepia+')';
      } else {
        document.documentElement.style.filter = '';
      }
    } catch(e){}
  }
  apply();
  // Re-check every 60s
  setInterval(apply, 60000);
  // Also listen for storage changes from settings page
  window.addEventListener('storage', e => { if(e.key === 'hondadash-night') apply(); });
})();
