// daynight.js — auto day/night mode with manual override
// Included on every page. Uses localStorage for state sync across iframes.
(function(){
  // Sunrise/sunset approximation (civil twilight)
  function getSunTimes(){
    var now = new Date();
    var month = now.getMonth(); // 0-11
    // Approximate sunrise/sunset hours for ~42°N latitude (Boston area)
    // These shift through the year
    var sunData = [
      [7,0, 16,30],  // Jan
      [6,45, 17,15],  // Feb
      [6,0, 17,45],  // Mar
      [5,30, 19,15],  // Apr
      [5,15, 19,45],  // May
      [5,0, 20,15],  // Jun
      [5,15, 20,15],  // Jul
      [5,45, 19,45],  // Aug
      [6,15, 19,0],  // Sep
      [6,45, 18,15],  // Oct
      [6,15, 16,30],  // Nov
      [7,0, 16,15],  // Dec
    ];
    var s = sunData[month];
    return { riseH:s[0], riseM:s[1], setH:s[2], setM:s[3] };
  }

  function isDay(){
    var now = new Date();
    var mins = now.getHours()*60 + now.getMinutes();
    var sun = getSunTimes();
    var rise = sun.riseH*60 + sun.riseM;
    var set = sun.setH*60 + sun.setM;
    return mins >= rise && mins < set;
  }

  function getState(){
    try {
      return JSON.parse(localStorage.getItem('hondadash-daynight') || '{}');
    } catch(e){ return {}; }
  }

  function saveState(state){
    localStorage.setItem('hondadash-daynight', JSON.stringify(state));
  }

  function apply(){
    var state = getState();
    var mode = state.mode || 'auto'; // 'auto', 'day', 'night'
    var dayMode;
    if(mode === 'auto'){
      dayMode = isDay();
    } else {
      dayMode = mode === 'day';
    }

    if(dayMode){
      document.documentElement.classList.add('day-mode');
      document.documentElement.classList.remove('night-mode');
    } else {
      document.documentElement.classList.remove('day-mode');
      document.documentElement.classList.add('night-mode');
    }

    // Update toggle button if it exists (only in index.html)
    var btn = document.getElementById('daynight-btn');
    if(btn){
      btn.textContent = dayMode ? '\u263C' : '\u263E'; // ☼ or ☾
      btn.style.color = dayMode ? '#ffaa00' : '#cc110066';
    }
  }

  // Expose for manual toggle from index.html
  window._dayNightToggle = function(){
    var state = getState();
    var mode = state.mode || 'auto';
    // Cycle: auto -> day -> night -> auto
    if(mode === 'auto'){
      state.mode = isDay() ? 'night' : 'day'; // flip from current auto state
    } else if(mode === 'day'){
      state.mode = 'night';
    } else {
      state.mode = 'auto';
    }
    saveState(state);
    apply();
    // Notify iframes
    var msg = JSON.stringify({type:'daynight'});
    var frames = document.querySelectorAll('iframe');
    for(var i=0;i<frames.length;i++){
      try{ frames[i].contentWindow.postMessage(msg,'*'); }catch(e){}
    }
  };

  window._getDayNightMode = function(){
    var state = getState();
    var mode = state.mode || 'auto';
    if(mode === 'auto') return isDay() ? 'day' : 'night';
    return mode;
  };

  apply();
  // Re-check every 60s for auto mode time transitions
  setInterval(apply, 60000);
  // Listen for changes from other frames
  window.addEventListener('storage', function(e){ if(e.key === 'hondadash-daynight') apply(); });
  window.addEventListener('message', function(e){
    try {
      var msg = typeof e.data === 'string' ? JSON.parse(e.data) : e.data;
      if(msg.type === 'daynight') apply();
    } catch(ex){}
  });
})();
