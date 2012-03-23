{-# LANGUAGE OverloadedStrings #-}

import Prelude hiding (id)
import Control.Category (id)
import Control.Arrow (arr, (>>>), (***))
import Data.Monoid (mempty, mconcat)

import Hakyll
import Text.Pandoc

static name = match name $ do
    route idRoute
    compile copyFileCompiler

writerOpts = defaultWriterOptions
    { writerTableOfContents = True
    , writerTemplate = "<h1 id='TOC'>Table of Contents</h1>$toc$\n$body$"
    , writerStandalone = True
    , writerNumberSections = True
    }

main :: IO ()
main = hakyll $ do
    -- Copy static resources
    static "images/**"
    static "js/*"
    static "CNAME"

    -- Copy and compress CSS
    match "css/*" $ do
        route   idRoute
        compile compressCssCompiler
    
    -- Render posts
    match "posts/*" $ do
        route $ setExtension ".html"
        compile $ pageCompiler
            >>> arr (renderDateField "date" "%e %B %Y" "Unknown date")
            >>> applyTemplateCompiler "templates/news-article.html"
            >>> processPage
    
    -- Render posts list page
    match "news.html" $ route idRoute
    create "news.html" $ constA mempty
        >>> arr (setField "title" "All Posts")
        >>> requireAllA "posts/*" addPostList
        >>> applyTemplateCompiler "templates/news.html"
        >>> processPage

    -- Render RSS feed
    match  "rss.xml" $ route idRoute
    create "rss.xml" $
        requireAll_ "posts/*" >>> renderRss feedConfiguration

    -- Compile templates
    match "templates/*" $ compile templateCompiler

    -- Compile topbar, sidebar, footer
    match (list ["topbar.html", "sidebar.html", "footer.html"]) $ compile readPageCompiler

    -- Html-sourced pages
    match (list ["index.html", "installation.html", "training.html" ]) $ do
        route idRoute
        compile $ readPageCompiler >>> processPage

    -- What's new pages
    match "whatsnew1-0-0/*.html" $ do
        route idRoute
        compile $ readPageCompiler >>> processPage
    match "whatsnew1-0-0/*.png" $ do
        route idRoute
        compile copyFileCompiler

    -- 404 page must not use relativized URLs
    match "404.html" $ do
        route idRoute
        compile $ readPageCompiler >>> processPagePartial

    -- Articles
    match (list ["tutorial.md", "development.md", "faq.md", "release-notes.md", "concepts.md"]) $ do
        route   $ setExtension ".html"
        compile $ pageCompilerWith defaultHakyllParserState writerOpts
            >>> applyTemplateCompiler "templates/article.html"
            >>> processPage


processPage :: Compiler (Page String) (Page String)
processPage = processPagePartial
          >>> relativizeUrlsCompiler

processPagePartial :: Compiler (Page String) (Page String)
processPagePartial = generatePlaceholder "sidebar.html" "sidebar"
          >>> generatePlaceholder "topbar.html" "topbar"
          >>> generatePlaceholder "footer.html" "footer"
          >>> applyTemplateCompiler "templates/default.html"

generatePlaceholder :: Identifier (Page String) -> String -> Compiler (Page String) (Page String)
generatePlaceholder input fieldName = requireA input (setFieldA fieldName $ arr pageBody)

addPostList :: Compiler (Page String, [Page String]) (Page String)
addPostList = setFieldA "posts" $
    arr (reverse . chronological)
        >>> require "templates/news-header.html" (\p t -> map (applyTemplate t) p)
        >>> arr mconcat
        >>> arr pageBody

feedConfiguration :: FeedConfiguration
feedConfiguration = FeedConfiguration
    { feedTitle       = "Bndtools News."
    , feedDescription = "Bndtools News RSS Feed."
    , feedAuthorName  = "Bndtools"
    , feedRoot        = "http://bndtools.org"
    }
