// Dark mode / color-scheme toggle for bnd documentation.
// Two-state toggle: light <-> dark.
// If no saved preference exists, initialise from OS/browser color-scheme.
jtd.onReady(function () {
  'use strict';

  function toArray(list) {
    return Array.prototype.slice.call(list || []);
  }

  function tryParseUrl(href, baseUrl) {
    try {
      return new URL(href, baseUrl);
    } catch (e) {
      return null;
    }
  }

  function isSearchShortcut(event) {
    var isShortcutKey = event.ctrlKey || event.metaKey;
    return isShortcutKey && event.shiftKey && String(event.key).toLowerCase() === 's';
  }

  function focusSearchInput() {
    var searchInput = document.getElementById('search-input');
    if (!searchInput) {
      return false;
    }

    searchInput.focus();
    if (typeof searchInput.select === 'function') {
      searchInput.select();
    }
    return true;
  }

  function scrollNavItemIntoView(activeLink) {
    if (!activeLink) {
      return;
    }

    var navContainer = activeLink.closest('.site-nav');
    if (!navContainer) {
      activeLink.scrollIntoView({
        block: 'nearest',
        inline: 'nearest'
      });
      return;
    }

    var containerRect = navContainer.getBoundingClientRect();
    var itemRect = activeLink.getBoundingClientRect();
    var itemAbove = itemRect.top < containerRect.top;
    var itemBelow = itemRect.bottom > containerRect.bottom;

    if (itemAbove || itemBelow) {
      navContainer.scrollTop += itemRect.top - containerRect.top - (containerRect.height / 2) + (itemRect.height / 2);
    }
  }

  function syncActiveNavPosition() {
    var activeLink = document.querySelector('.site-nav .nav-list-link.active') ||
      document.querySelector('.site-nav .nav-list-item.active > .nav-list-link');

    if (!activeLink) {
      return;
    }

    requestAnimationFrame(function () {
      scrollNavItemIntoView(activeLink);
    });
  }

  function normalizePath(pathname) {
    return (pathname || '/')
      .replace(/index\.html$/, '')
      .replace(/\.html$/, '')
      .replace(/\/$/, '') || '/';
  }

  function nestChapterSectionsUnderHome() {
    var nav = document.querySelector('.site-nav');
    if (!nav) {
      return;
    }

    var navLinks = toArray(nav.querySelectorAll('a.nav-list-link'));
    var homeLink = navLinks.find(function (link) {
      var parsed = tryParseUrl(link.getAttribute('href'), window.location.href);
      if (!parsed) {
        return false;
      }
      return normalizePath(parsed.pathname) === '/';
    });

    if (!homeLink) {
      return;
    }

    var homeItem = homeLink.closest('li.nav-list-item');
    if (!homeItem) {
      return;
    }

    var topNavLists = toArray(nav.querySelectorAll(':scope > ul.nav-list'));
    var chapterRootList = topNavLists.find(function (list) {
      var previous = list.previousElementSibling;
      return previous && previous.classList && previous.classList.contains('nav-category') && previous.textContent.trim() === '';
    });

    if (!chapterRootList) {
      return;
    }

    var chapterSectionItems = toArray(chapterRootList.querySelectorAll(':scope > li.nav-list-item'));
    if (!chapterSectionItems.length) {
      return;
    }

    var childList = homeItem.querySelector(':scope > ul.nav-list');
    if (!childList) {
      childList = document.createElement('ul');
      childList.className = 'nav-list';
      homeItem.appendChild(childList);
    }
    childList.style.display = 'block';

    var expander = homeItem.querySelector(':scope > .nav-list-expander');
    if (!expander) {
      expander = document.createElement('button');
      expander.className = 'nav-list-expander btn-reset';
      expander.setAttribute('aria-label', 'Home submenu');
      expander.setAttribute('aria-expanded', 'true');
      expander.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><use xlink:href="#svg-arrow-right"></use></svg>';
      homeItem.insertBefore(expander, homeLink);
    }

    chapterSectionItems.forEach(function (item) {
      childList.appendChild(item);
    });

    var chapterCategory = chapterRootList.previousElementSibling;
    if (chapterRootList.parentElement) {
      chapterRootList.parentElement.removeChild(chapterRootList);
    }
    if (chapterCategory && chapterCategory.parentElement) {
      chapterCategory.parentElement.removeChild(chapterCategory);
    }
  }

  function expandCurrentCollectionNavBranch() {
    var nav = document.querySelector('.site-nav');
    if (!nav) {
      return;
    }

    var currentPath = normalizePath(window.location.pathname);
    var pathParts = currentPath.split('/').filter(Boolean);
    if (pathParts.length === 0) {
      return;
    }

    var sectionRootPath = '/' + pathParts[0];

    function linkPath(link) {
      var parsed = tryParseUrl(link.getAttribute('href'), window.location.href);
      if (!parsed) {
        return '';
      }
      return normalizePath(parsed.pathname);
    }

    function markActive(item) {
      var current = item;
      while (current) {
        current.classList.add('active');
        var expander = current.querySelector(':scope > .nav-list-expander');
        if (expander) {
          expander.setAttribute('aria-expanded', 'true');
        }
        var currentChildList = current.querySelector(':scope > .nav-list');
        if (currentChildList) {
          currentChildList.style.display = 'block';
        }
        var parentList = current.parentElement;
        if (parentList && parentList.classList.contains('nav-list')) {
          parentList.style.display = 'block';
        }
        current = current.parentElement ? current.parentElement.closest('li.nav-list-item') : null;
      }
    }

    var navLinks = toArray(nav.querySelectorAll('a.nav-list-link'));
    var collectionRoot = navLinks.find(function (link) {
      return linkPath(link) === sectionRootPath;
    });

    if (collectionRoot) {
      markActive(collectionRoot.closest('li.nav-list-item'));
    }

    var currentLink = navLinks.find(function (link) {
      return linkPath(link) === currentPath;
    });

    if (currentLink) {
      currentLink.classList.add('active');
      markActive(currentLink.closest('li.nav-list-item'));
    }
  }

  nestChapterSectionsUnderHome();
  expandCurrentCollectionNavBranch();

  syncActiveNavPosition();
  window.addEventListener('hashchange', syncActiveNavPosition);

  window.addEventListener('keydown', function (event) {
    if (!isSearchShortcut(event)) {
      return;
    }

    if (!focusSearchInput()) {
      return;
    }

    event.preventDefault();
  });

  var currentOrigin = window.location.origin;
  document.querySelectorAll('a[href]').forEach(function (link) {
    var href = link.getAttribute('href');
    if (!href || href[0] === '#' || href.indexOf('mailto:') === 0 || href.indexOf('tel:') === 0 || href.indexOf('javascript:') === 0) {
      return;
    }

    var url = tryParseUrl(href, window.location.href);
    if (!url) {
      return;
    }

    if ((url.protocol === 'http:' || url.protocol === 'https:') && url.origin !== currentOrigin) {
      link.setAttribute('target', '_blank');
      link.setAttribute('rel', 'noopener noreferrer');
    }
  });

  var STORAGE_KEY = 'theme-preference';
  var btn = document.getElementById('theme-toggle');
  if (!btn) return;

  var auxList = document.querySelector('.aux-nav-list');
  if (auxList && !btn.closest('.aux-nav-list')) {
    var item = document.createElement('li');
    item.className = 'aux-nav-list-item bnd-theme-toggle-item';
    item.appendChild(btn);
    auxList.appendChild(item);
  }

  function systemPref() {
    var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    return prefersDark ? 'dark' : 'light';
  }

  function currentPref() {
    var value;
    try {
      value = localStorage.getItem(STORAGE_KEY);
    } catch (e) {
      return null;
    }
    return (value === 'light' || value === 'dark') ? value : null;
  }

  function activePref() {
    return btn.getAttribute('data-theme-mode') || currentPref() || systemPref();
  }

  function getThemeStylesheet() {
    return document.querySelector('link[href*="just-the-docs-default.css"], link[href*="just-the-docs-light.css"], link[href*="just-the-docs-dark.css"]');
  }

  function setThemeStylesheet(pref) {
    var cssFile = getThemeStylesheet();
    if (!cssFile) {
      return;
    }
    var href = cssFile.getAttribute('href') || '';
    cssFile.setAttribute('href', href.replace(/just-the-docs-(default|light|dark)\.css/, 'just-the-docs-' + pref + '.css'));
  }

  function applyPref(pref, persist) {
    // Switch just-the-docs stylesheet while preserving base path
    setThemeStylesheet(pref);
    // Set data attribute for any CSS that targets [data-color-scheme]
    document.documentElement.setAttribute('data-color-scheme', pref);
    if (persist) {
      try {
        localStorage.setItem(STORAGE_KEY, pref);
      } catch (e) {}
    }
    btn.setAttribute('data-theme-mode', pref);
    updateButton(pref);
  }

  function updateButton(pref) {
    var nextLabel = pref === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';
    btn.setAttribute('aria-label', nextLabel);
    btn.setAttribute('title', nextLabel);
  }

  // Initialise from saved preference, or from system if first access/no saved preference.
  applyPref(currentPref() || systemPref(), false);

  btn.addEventListener('click', function (event) {
    event.preventDefault();
    var next = activePref() === 'dark' ? 'light' : 'dark';
    applyPref(next, true);
  });

  document.querySelectorAll('div.highlighter-rouge[class*="language-"]').forEach(function (block) {
    if (block.querySelector('.bnd-code-language')) {
      return;
    }

    var languageClass = Array.prototype.find.call(block.classList, function (name) {
      return name.indexOf('language-') === 0;
    });

    if (!languageClass) {
      return;
    }

    var language = languageClass.substring('language-'.length).trim();
    if (!language) {
      return;
    }

    block.classList.add('bnd-codeblock-labeled');

    var label = document.createElement('span');
    label.className = 'bnd-code-language';
    label.textContent = language;
    block.insertBefore(label, block.firstChild);
  });

  document.querySelectorAll('[data-bnd-tabs]').forEach(function (tabsContainer, tabsIndex) {
    var buttons = Array.prototype.slice.call(tabsContainer.querySelectorAll('.bnd-tab-button[data-bnd-tab-target]'));
    var panels = Array.prototype.slice.call(tabsContainer.querySelectorAll('.bnd-tab-panel'));

    if (!buttons.length || !panels.length) {
      return;
    }

    buttons.forEach(function (button, index) {
      if (!button.id) {
        button.id = 'bnd-tab-' + tabsIndex + '-' + index;
      }
    });

    panels.forEach(function (panel) {
      var owningButton = buttons.find(function (button) {
        return button.getAttribute('data-bnd-tab-target') === panel.id;
      });
      if (owningButton) {
        panel.setAttribute('aria-labelledby', owningButton.id);
      }
    });

    function activate(tabId) {
      buttons.forEach(function (button) {
        var active = button.getAttribute('data-bnd-tab-target') === tabId;
        button.classList.toggle('is-active', active);
        button.setAttribute('aria-selected', active ? 'true' : 'false');
        button.setAttribute('tabindex', active ? '0' : '-1');
      });

      panels.forEach(function (panel) {
        var active = panel.id === tabId;
        panel.classList.toggle('is-active', active);
        panel.hidden = !active;
      });
    }

    buttons.forEach(function (button) {
      button.addEventListener('click', function () {
        activate(button.getAttribute('data-bnd-tab-target'));
      });

      button.addEventListener('keydown', function (event) {
        var currentIndex = buttons.indexOf(button);
        var nextIndex;

        switch (event.key) {
          case 'ArrowLeft':
          case 'ArrowUp':
            event.preventDefault();
            nextIndex = (currentIndex - 1 + buttons.length) % buttons.length;
            buttons[nextIndex].focus();
            activate(buttons[nextIndex].getAttribute('data-bnd-tab-target'));
            break;
          case 'ArrowRight':
          case 'ArrowDown':
            event.preventDefault();
            nextIndex = (currentIndex + 1) % buttons.length;
            buttons[nextIndex].focus();
            activate(buttons[nextIndex].getAttribute('data-bnd-tab-target'));
            break;
          case 'Home':
            event.preventDefault();
            buttons[0].focus();
            activate(buttons[0].getAttribute('data-bnd-tab-target'));
            break;
          case 'End':
            event.preventDefault();
            buttons[buttons.length - 1].focus();
            activate(buttons[buttons.length - 1].getAttribute('data-bnd-tab-target'));
            break;
          case 'Enter':
          case ' ':
            event.preventDefault();
            activate(button.getAttribute('data-bnd-tab-target'));
            break;
          default:
            break;
        }
      });
    });

    var initialButton = tabsContainer.querySelector('.bnd-tab-button.is-active') || buttons[0];
    activate(initialButton.getAttribute('data-bnd-tab-target'));
  });

  // fetch older releases and populate the release selector dropdown
  fetch("/releases/index.json")
    .then(response => response.json())
    .then(data => {
      var container = document.querySelector('.releases .dropdown-content');
      if (!container) {
        return;
      }

      data.forEach(function (release) {
        var a = document.createElement('a');
        a.href = release.url;
        a.textContent = release.name;

        container.appendChild(a);
      });
    })
    .catch(function (err) {
      console.error(err);
    });


});
