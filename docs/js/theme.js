function toggleTheme() {
  var html = document.documentElement;
  var isDark = html.getAttribute('data-theme') === 'dark';
  var newTheme = isDark ? 'light' : 'dark';
  html.setAttribute('data-theme', newTheme);
  try {
    localStorage.setItem('theme', newTheme);
  } catch (e) {
    console.warn('Failed to save theme preference:', e);
  }
  updateThemeToggle();
}

function updateThemeToggle() {
  var btn = document.getElementById('theme-toggle');
  if (!btn) return;
  var isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  btn.title = isDark ? 'Switch to light mode' : 'Switch to dark mode';
  btn.innerHTML = isDark
    ? '<span class="fa fa-sun-o"></span>'
    : '<span class="fa fa-moon-o"></span>';
}

$(document).ready(function () {
  updateThemeToggle();
});
