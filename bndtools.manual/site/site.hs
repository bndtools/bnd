{-# LANGUAGE OverloadedStrings #-}

import Prelude hiding (id)
import Control.Category (id)
import Control.Arrow (arr, (>>>), (***))
import Data.Monoid (mappend)

import Hakyll
import Text.Pandoc

htmlPages = ["index.html"
            , "installation.html"
            , "training.html"
            , "licence.html"
            , "acknowledge.html"
            , "community.html"
            ] 

articles = ["concepts.md"
           , "development.md"
           , "faq.md"
           , "tutorial.md"
           ]

staticPatterns = ["images/**"
                 , "js/*"
                 , "CNAME"
                 ]

-- Pandoc processing options
readerOpts = def :: ReaderOptions
writerOpts = def
    { writerTableOfContents = True
    , writerTemplate = "<h1 id='TOC'>Table of Contents</h1>$toc$\n$body$"
    , writerStandalone = True
    , writerNumberSections = True
    } :: WriterOptions


main = hakyll $ do
    -- Copy static resources
    match (fromList staticPatterns) $ do
        route idRoute
        compile copyFileCompiler

    -- Copy and compress CSS
    match "css/*" $ do
        route idRoute
        compile compressCssCompiler

    -- Compile templates
    match "templates/*" $ compile templateCompiler

    -- Articles in Markdown
    match (fromList articles) $ do
        route   $ setExtension ".html"
        compile $ pandocCompiler
            >>= loadAndApplyTemplate "templates/article.html" defaultContext

    -- Html-sourced pages
    match (fromList htmlPages) $ do
        route idRoute
        compile $ getResourceString
            >>= loadAndApplyTemplate "templates/default.html" defaultContext
            >>= relativizeUrls

    -- 404.html must not relativize URLs
    match "404.html" $ do
        route idRoute
        compile $ getResourceString
            >>= loadAndApplyTemplate "templates/default.html" defaultContext

