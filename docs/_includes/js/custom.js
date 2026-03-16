// Dark mode / color-scheme toggle for bnd documentation.
// Three-state cycle: auto -> light -> dark -> auto ...
// Mirrors the pattern used in klibio/apps/_doc/_layouts/default.html
jtd.onReady(function () {
  'use strict';

  var STORAGE_KEY = 'theme-preference';
  var CYCLE = ['auto', 'light', 'dark'];
  var btn = document.getElementById('theme-toggle');
  if (!btn) return;

  function currentPref() {
    var t;
    try { t = localStorage.getItem(STORAGE_KEY); } catch (e) {}
    return (t === 'light' || t === 'dark') ? t : 'auto';
  }

  function applyPref(pref) {
    var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    var useDark = pref === 'dark' || (pref === 'auto' && prefersDark);
    // Switch just-the-docs stylesheet
    jtd.setTheme(useDark ? 'dark' : 'light');
    // Set data attribute for any CSS that targets [data-color-scheme]
    document.documentElement.setAttribute('data-color-scheme', useDark ? 'dark' : 'light');
    try { localStorage.setItem(STORAGE_KEY, pref); } catch (e) {}
    updateButton(pref);
  }

  function updateButton(pref) {
    btn.querySelector('.icon-auto').style.display  = pref === 'auto'  ? '' : 'none';
    btn.querySelector('.icon-light').style.display = pref === 'light' ? '' : 'none';
    btn.querySelector('.icon-dark').style.display  = pref === 'dark'  ? '' : 'none';
  }

  // Initialise button label (theme already applied by inline head script)
  updateButton(currentPref());

  btn.addEventListener('click', function () {
    var next = CYCLE[(CYCLE.indexOf(currentPref()) + 1) % CYCLE.length];
    applyPref(next);
  });

  // Re-evaluate when OS theme changes and user is in 'auto' mode
  if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
      if (currentPref() === 'auto') { applyPref('auto'); }
    });
  }
});
