---
layout: default
title:   syntax [options] <header|instruction> ...
summary: Access the internal bnd database of keywords and options 
---

## Description

{{page.summary}}

## Synopsis

## Options

	[ -w, --width <int> ]      - The width of the printout

## Examples

	biz.aQute.bnd (master)$ bnd syntax Bundle-Version
		
	[Bundle-Version]
		The Bundle-Version header specifies the version of this bundle.

		Pattern                               : [0-9]{1,9}(\.[0-9]{1,9}(\.[0-9]{1,9}(\.[0-9A-Za-z_-]+)?)?)?
		Example                               : Bundle-Version: 1.23.4.build200903221000

