(function (jtd, undefined) {

// Event handling

jtd.addEvent = function(el, type, handler) {
  if (el.attachEvent) el.attachEvent('on'+type, handler); else el.addEventListener(type, handler);
}
jtd.removeEvent = function(el, type, handler) {
  if (el.detachEvent) el.detachEvent('on'+type, handler); else el.removeEventListener(type, handler);
}
jtd.onReady = function(ready) {
  // in case the document is already rendered
  if (document.readyState!='loading') ready();
  // modern browsers
  else if (document.addEventListener) document.addEventListener('DOMContentLoaded', ready);
  // IE <= 8
  else document.attachEvent('onreadystatechange', function(){
      if (document.readyState=='complete') ready();
  });
}

// Show/hide mobile menu

function initNav() {
  jtd.addEvent(document, 'click', function(e){
    var target = e.target;
    while (target && !(target.classList && target.classList.contains('nav-list-expander'))) {
      target = target.parentNode;
    }
    if (target) {
      e.preventDefault();
      target.ariaExpanded = target.parentNode.classList.toggle('active');
    }
  });

  const siteNav = document.getElementById('site-nav');
  const mainHeader = document.getElementById('main-header');
  const menuButton = document.getElementById('menu-button');

  disableHeadStyleSheets();

  jtd.addEvent(menuButton, 'click', function(e){
    e.preventDefault();

    if (menuButton.classList.toggle('nav-open')) {
      siteNav.classList.add('nav-open');
      mainHeader.classList.add('nav-open');
      menuButton.ariaExpanded = true;
    } else {
      siteNav.classList.remove('nav-open');
      mainHeader.classList.remove('nav-open');
      menuButton.ariaExpanded = false;
    }
  });
}

// The <head> element is assumed to include the following stylesheets:
// - a <link> to /assets/css/just-the-docs-head-nav.css,
//             with id 'jtd-head-nav-stylesheet'
// - a <style> containing the result of _includes/css/activation.scss.liquid.
// To avoid relying on the order of stylesheets (which can change with HTML
// compression, user-added JavaScript, and other side effects), stylesheets
// are only interacted with via ID

function disableHeadStyleSheets() {
  const headNav = document.getElementById('jtd-head-nav-stylesheet');
  if (headNav) {
    headNav.disabled = true;
  }

  const activation = document.getElementById('jtd-nav-activation');
  if (activation) {
    activation.disabled = true;
  }
}
// Site search

function initSearch() {
  var request = new XMLHttpRequest();
  request.open('GET', '/releases/7.4.0/assets/js/search-data.json', true);

  request.onload = function(){
    if (request.status >= 200 && request.status < 400) {
      var docs = JSON.parse(request.responseText);

      lunr.tokenizer.separator = /[\s\-/]+/

      var index = lunr(function(){
        this.ref('id');
        this.field('title', { boost: 200 });
        this.field('content', { boost: 2 });
        this.field('relUrl');
        this.metadataWhitelist = ['position']

        for (var i in docs) {
          
          this.add({
            id: i,
            title: docs[i].title,
            content: docs[i].content,
            relUrl: docs[i].relUrl
          });
        }
      });

      searchLoaded(index, docs);
    } else {
      console.log('Error loading ajax request. Request status:' + request.status);
    }
  };

  request.onerror = function(){
    console.log('There was a connection error');
  };

  request.send();
}

function searchLoaded(index, docs) {
  var index = index;
  var docs = docs;
  var searchInput = document.getElementById('search-input');
  var searchResults = document.getElementById('search-results');
  var mainHeader = document.getElementById('main-header');
  var currentInput;
  var currentSearchIndex = 0;

  function showSearch() {
    document.documentElement.classList.add('search-active');
  }

  function hideSearch() {
    document.documentElement.classList.remove('search-active');
  }

  function update() {
    currentSearchIndex++;

    var input = searchInput.value;
    if (input === '') {
      hideSearch();
    } else {
      showSearch();
      // scroll search input into view, workaround for iOS Safari
      window.scroll(0, -1);
      setTimeout(function(){ window.scroll(0, 0); }, 0);
    }
    if (input === currentInput) {
      return;
    }
    currentInput = input;
    searchResults.innerHTML = '';
    if (input === '') {
      return;
    }

    var results = index.query(function (query) {
      var tokens = lunr.tokenizer(input)
      query.term(tokens, {
        boost: 10
      });
      query.term(tokens, {
        wildcard: lunr.Query.wildcard.TRAILING
      });
    });

    if ((results.length == 0) && (input.length > 2)) {
      var tokens = lunr.tokenizer(input).filter(function(token, i) {
        return token.str.length < 20;
      })
      if (tokens.length > 0) {
        results = index.query(function (query) {
          query.term(tokens, {
            editDistance: Math.round(Math.sqrt(input.length / 2 - 1))
          });
        });
      }
    }

    if (results.length == 0) {
      var noResultsDiv = document.createElement('div');
      noResultsDiv.classList.add('search-no-result');
      noResultsDiv.innerText = 'No results found';
      searchResults.appendChild(noResultsDiv);

    } else {
      var resultsList = document.createElement('ul');
      resultsList.classList.add('search-results-list');
      searchResults.appendChild(resultsList);

      addResults(resultsList, results, 0, 10, 100, currentSearchIndex);
    }

    function addResults(resultsList, results, start, batchSize, batchMillis, searchIndex) {
      if (searchIndex != currentSearchIndex) {
        return;
      }
      for (var i = start; i < (start + batchSize); i++) {
        if (i == results.length) {
          return;
        }
        addResult(resultsList, results[i]);
      }
      setTimeout(function() {
        addResults(resultsList, results, start + batchSize, batchSize, batchMillis, searchIndex);
      }, batchMillis);
    }

    function addResult(resultsList, result) {
      var doc = docs[result.ref];

      var resultsListItem = document.createElement('li');
      resultsListItem.classList.add('search-results-list-item');
      resultsList.appendChild(resultsListItem);

      var resultLink = document.createElement('a');
      resultLink.classList.add('search-result');
      resultLink.setAttribute('href', doc.url);
      resultsListItem.appendChild(resultLink);

      var resultTitle = document.createElement('div');
      resultTitle.classList.add('search-result-title');
      resultLink.appendChild(resultTitle);

      // note: the SVG svg-doc is only loaded as a Jekyll include if site.search_enabled is true; see _includes/icons/icons.html
      var resultDoc = document.createElement('div');
      resultDoc.classList.add('search-result-doc');
      resultDoc.innerHTML = '<svg viewBox="0 0 24 24" class="search-result-icon" aria-hidden="true"><use xlink:href="#svg-doc"></use></svg>';
      resultTitle.appendChild(resultDoc);

      var resultDocTitle = document.createElement('div');
      resultDocTitle.classList.add('search-result-doc-title');
      resultDocTitle.innerHTML = doc.doc;
      resultDoc.appendChild(resultDocTitle);
      var resultDocOrSection = resultDocTitle;

      if (doc.doc != doc.title) {
        resultDoc.classList.add('search-result-doc-parent');
        var resultSection = document.createElement('div');
        resultSection.classList.add('search-result-section');
        resultSection.innerHTML = doc.title;
        resultTitle.appendChild(resultSection);
        resultDocOrSection = resultSection;
      }

      var metadata = result.matchData.metadata;
      var titlePositions = [];
      var contentPositions = [];
      for (var j in metadata) {
        var meta = metadata[j];
        if (meta.title) {
          var positions = meta.title.position;
          for (var k in positions) {
            titlePositions.push(positions[k]);
          }
        }
        if (meta.content) {
          var positions = meta.content.position;
          for (var k in positions) {
            var position = positions[k];
            var previewStart = position[0];
            var previewEnd = position[0] + position[1];
            var ellipsesBefore = true;
            var ellipsesAfter = true;
            for (var k = 0; k < 5; k++) {
              var nextSpace = doc.content.lastIndexOf(' ', previewStart - 2);
              var nextDot = doc.content.lastIndexOf('. ', previewStart - 2);
              if ((nextDot >= 0) && (nextDot > nextSpace)) {
                previewStart = nextDot + 1;
                ellipsesBefore = false;
                break;
              }
              if (nextSpace < 0) {
                previewStart = 0;
                ellipsesBefore = false;
                break;
              }
              previewStart = nextSpace + 1;
            }
            for (var k = 0; k < 10; k++) {
              var nextSpace = doc.content.indexOf(' ', previewEnd + 1);
              var nextDot = doc.content.indexOf('. ', previewEnd + 1);
              if ((nextDot >= 0) && (nextDot < nextSpace)) {
                previewEnd = nextDot;
                ellipsesAfter = false;
                break;
              }
              if (nextSpace < 0) {
                previewEnd = doc.content.length;
                ellipsesAfter = false;
                break;
              }
              previewEnd = nextSpace;
            }
            contentPositions.push({
              highlight: position,
              previewStart: previewStart, previewEnd: previewEnd,
              ellipsesBefore: ellipsesBefore, ellipsesAfter: ellipsesAfter
            });
          }
        }
      }

      if (titlePositions.length > 0) {
        titlePositions.sort(function(p1, p2){ return p1[0] - p2[0] });
        resultDocOrSection.innerHTML = '';
        addHighlightedText(resultDocOrSection, doc.title, 0, doc.title.length, titlePositions);
      }

      if (contentPositions.length > 0) {
        contentPositions.sort(function(p1, p2){ return p1.highlight[0] - p2.highlight[0] });
        var contentPosition = contentPositions[0];
        var previewPosition = {
          highlight: [contentPosition.highlight],
          previewStart: contentPosition.previewStart, previewEnd: contentPosition.previewEnd,
          ellipsesBefore: contentPosition.ellipsesBefore, ellipsesAfter: contentPosition.ellipsesAfter
        };
        var previewPositions = [previewPosition];
        for (var j = 1; j < contentPositions.length; j++) {
          contentPosition = contentPositions[j];
          if (previewPosition.previewEnd < contentPosition.previewStart) {
            previewPosition = {
              highlight: [contentPosition.highlight],
              previewStart: contentPosition.previewStart, previewEnd: contentPosition.previewEnd,
              ellipsesBefore: contentPosition.ellipsesBefore, ellipsesAfter: contentPosition.ellipsesAfter
            }
            previewPositions.push(previewPosition);
          } else {
            previewPosition.highlight.push(contentPosition.highlight);
            previewPosition.previewEnd = contentPosition.previewEnd;
            previewPosition.ellipsesAfter = contentPosition.ellipsesAfter;
          }
        }

        var resultPreviews = document.createElement('div');
        resultPreviews.classList.add('search-result-previews');
        resultLink.appendChild(resultPreviews);

        var content = doc.content;
        for (var j = 0; j < Math.min(previewPositions.length, 3); j++) {
          var position = previewPositions[j];

          var resultPreview = document.createElement('div');
          resultPreview.classList.add('search-result-preview');
          resultPreviews.appendChild(resultPreview);

          if (position.ellipsesBefore) {
            resultPreview.appendChild(document.createTextNode('... '));
          }
          addHighlightedText(resultPreview, content, position.previewStart, position.previewEnd, position.highlight);
          if (position.ellipsesAfter) {
            resultPreview.appendChild(document.createTextNode(' ...'));
          }
        }
      }
      var resultRelUrl = document.createElement('span');
      resultRelUrl.classList.add('search-result-rel-url');
      resultRelUrl.innerText = doc.relUrl;
      resultTitle.appendChild(resultRelUrl);
    }

    function addHighlightedText(parent, text, start, end, positions) {
      var index = start;
      for (var i in positions) {
        var position = positions[i];
        var span = document.createElement('span');
        span.innerHTML = text.substring(index, position[0]);
        parent.appendChild(span);
        index = position[0] + position[1];
        var highlight = document.createElement('span');
        highlight.classList.add('search-result-highlight');
        highlight.innerHTML = text.substring(position[0], index);
        parent.appendChild(highlight);
      }
      var span = document.createElement('span');
      span.innerHTML = text.substring(index, end);
      parent.appendChild(span);
    }
  }

  jtd.addEvent(searchInput, 'focus', function(){
    setTimeout(update, 0);
  });

  // When the search bar is *not* focused, it should be hidden. This code
  // manages that - which is a bit tricky given that we can't just rely on
  // focusout, since we could be re-focusing within the search itself.
  const updateSearchFocus = function(evt) {
    const nextFocusedElement = evt.relatedTarget;

    // Re-focusing on search bar - "keep focus"
    if (nextFocusedElement.id === 'search-input') return;

    // Re-focusing on the next search result element - "keep focus"
    if (nextFocusedElement.classList.contains('search-result')) return;

    // Otherwise, we're not focused on the search bar anymore. Hide!
    hideSearch();
  }

  searchInput.addEventListener('focusout', updateSearchFocus);
  searchResults.addEventListener('focusout', updateSearchFocus);

  jtd.addEvent(searchInput, 'keyup', function(e){
    switch (e.keyCode) {
      case 27: // When esc key is pressed, hide the results and clear the field
        searchInput.value = '';
        break;
      case 38: // arrow up
      case 40: // arrow down
      case 13: // enter
        e.preventDefault();
        return;
    }
    update();
  });

  jtd.addEvent(searchInput, 'keydown', function(e){
    switch (e.keyCode) {
      case 38: // arrow up
        e.preventDefault();
        var active = document.querySelector('.search-result.active');
        if (active) {
          active.classList.remove('active');
          if (active.parentElement.previousSibling) {
            var previous = active.parentElement.previousSibling.querySelector('.search-result');
            previous.classList.add('active');
          }
        }
        return;
      case 40: // arrow down
        e.preventDefault();
        var active = document.querySelector('.search-result.active');
        if (active) {
          if (active.parentElement.nextSibling) {
            var next = active.parentElement.nextSibling.querySelector('.search-result');
            active.classList.remove('active');
            next.classList.add('active');
          }
        } else {
          var next = document.querySelector('.search-result');
          if (next) {
            next.classList.add('active');
          }
        }
        return;
      case 13: // enter
        e.preventDefault();
        var active = document.querySelector('.search-result.active');
        if (active) {
          active.click();
        } else {
          var first = document.querySelector('.search-result');
          if (first) {
            first.click();
          }
        }
        return;
    }
  });

  jtd.addEvent(document, 'click', function(e){
    if (e.target != searchInput) {
      hideSearch();
    }
  });
}

// Switch theme

jtd.getTheme = function() {
  var cssFileHref = document.querySelector('[rel="stylesheet"]').getAttribute('href');
  return cssFileHref.substring(cssFileHref.lastIndexOf('-') + 1, cssFileHref.length - 4);
}

jtd.setTheme = function(theme) {
  var cssFile = document.querySelector('[rel="stylesheet"]');
  cssFile.setAttribute('href', '/releases/7.4.0/assets/css/just-the-docs-' + theme + '.css');
}

// Note: pathname can have a trailing slash on a local jekyll server
// and not have the slash on GitHub Pages

function navLink() {
  var pathname = document.location.pathname;

  var navLink = document.getElementById('site-nav').querySelector('a[href="' + pathname + '"]');
  if (navLink) {
    return navLink;
  }

  // The `permalink` setting may produce navigation links whose `href` ends with `/` or `.html`.
  // To find these links when `/` is omitted from or added to pathname, or `.html` is omitted:

  if (pathname.endsWith('/') && pathname != '/') {
    pathname = pathname.slice(0, -1);
  }

  if (pathname != '/') {
    navLink = document.getElementById('site-nav').querySelector('a[href="' + pathname + '"], a[href="' + pathname + '/"], a[href="' + pathname + '.html"]');
    if (navLink) {
      return navLink;
    }
  }

  return null; // avoids `undefined`
}

// Scroll site-nav to ensure the link to the current page is visible

function scrollNav() {
  const targetLink = navLink();
  if (targetLink) {
    targetLink.scrollIntoView({ block: "center" });
    targetLink.removeAttribute('href');
  }
}

// Find the nav-list-link that refers to the current page
// then make it and all enclosing nav-list-item elements active.

function activateNav() {
  var target = navLink();
  if (target) {
    target.classList.toggle('active', true);
  }
  while (target) {
    while (target && !(target.classList && target.classList.contains('nav-list-item'))) {
      target = target.parentNode;
    }
    if (target) {
      target.classList.toggle('active', true);
      target = target.parentNode;
    }
  }
}

// Document ready

jtd.onReady(function(){
  if (document.getElementById('site-nav')) {
    initNav();
    activateNav();
    scrollNav();
  }
  initSearch();
});

// Accessibility: set tabindex=0 on each code highlight block, so screenreaders
// can focus over (particularly important if there's horizontal scroll)
// see: https://dequeuniversity.com/rules/axe/4.9/scrollable-region-focusable?application=axeAPI

jtd.onReady(() => {
  document
    .querySelectorAll("div.highlight")
    .forEach(codeBlock => codeBlock.setAttribute("tabindex", "0"));
});

// Copy button on code

jtd.onReady(function(){

  if (!window.isSecureContext) {
    console.log('Window does not have a secure context, therefore code clipboard copy functionality will not be available. For more details see https://web.dev/async-clipboard/#security-and-permissions');
    return;
  }

  var codeBlocks = document.querySelectorAll('div.highlighter-rouge, div.listingblock > div.content, figure.highlight');

  // note: the SVG svg-copied and svg-copy is only loaded as a Jekyll include if site.enable_copy_code_button is true; see _includes/icons/icons.html
  var svgCopied =  '<svg viewBox="0 0 24 24" class="copy-icon"><use xlink:href="#svg-copied"></use></svg>';
  var svgCopy =  '<svg viewBox="0 0 24 24" class="copy-icon"><use xlink:href="#svg-copy"></use></svg>';

  codeBlocks.forEach(codeBlock => {
    var copyButton = document.createElement('button');
    var timeout = null;
    copyButton.type = 'button';
    copyButton.ariaLabel = 'Copy code to clipboard';
    copyButton.innerHTML = svgCopy;
    codeBlock.append(copyButton);

    copyButton.addEventListener('click', function () {
      if(timeout === null) {
        var code = (codeBlock.querySelector('pre:not(.lineno, .highlight)') || codeBlock.querySelector('code')).innerText;
        window.navigator.clipboard.writeText(code);

        copyButton.innerHTML = svgCopied;

        var timeoutSetting = 4000;

        timeout = setTimeout(function () {
          copyButton.innerHTML = svgCopy;
          timeout = null;
        }, timeoutSetting);
      }
    });
  });

});

})(window.jtd = window.jtd || {});

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
  fetch("/releases/7.4.0/index.json")
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

  // ── Tocbot sticky right-side "On this page" TOC ──────────────────────────
  // Only initialised when tocbot is loaded and the page has at least 2 headings.
  (function () {
    if (typeof tocbot === 'undefined') {
      return;
    }

    var mainContent = document.querySelector('.main-content');
    if (!mainContent) {
      return;
    }

    var headings = mainContent.querySelectorAll('h2, h3');
    if (headings.length < 2) {
      return;
    }

    // Build the container that will hold the generated TOC links.
    var wrapper = document.createElement('nav');
    wrapper.id = 'bnd-toc';
    wrapper.setAttribute('aria-label', 'On this page');

    var title = document.createElement('p');
    title.className = 'bnd-toc-title';
    title.textContent = 'On this page';
    wrapper.appendChild(title);

    document.body.appendChild(wrapper);

    tocbot.init({
      tocSelector: '#bnd-toc',
      contentSelector: '.main-content',
      headingSelector: 'h2, h3',
      orderedList: false,
      scrollSmooth: false,
      scrollSmoothDuration: 420,
      headingsOffset: 80,
      scrollSmoothOffset: -80,
      collapseDepth: 6,
      activeLinkClass: 'is-active-link',
    });
  }());

});

