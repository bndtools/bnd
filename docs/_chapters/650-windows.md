---
title: Tips for Windows users
layout: default
author: Fr Jeremy Krieg
---

Bnd and Bndtools runs on any system that can run Eclipse. However, every operating system is different and has its own quirks. Windows in particular has a few that impact developers using Bnd. While the Bnd experience may never be as smooth on Windows as it is on macOS or Linux, there are a few tweaks that make it a lot smoother than what comes out of the box.

It is possible that some of the tips listed here will also help for Eclipse development work in general, or perhaps any development work on Windows. 

## Key problems on Windows 

Windows has two key architectural differences to *nix-based OSes that really impact development work in Java:

### File locking

Most \*nix systems use advisory file locking, rather than mandatory locking. Windows uses mandatory locking rather than advisory. Most Java programs are developed on \*nix and tend to assume \*nix locking semantics. This means that they are not as careful at cleaning up after themselves and ensuring that file locks are released.

In particular, there are problems when an application exits using `System.exit()` - the normal way for Java applications to clean up is to release a lock is in a `finally` clause. However, when an application calls `System.exit()`, these normal channels are bypassed, which means the JVM never gets a chance to release the locks and hence can't delete the locked files.

### Real-time protection

From version 10, Windows' built-in anti-malware program Windows Defender has become very tightly integrated with the OS - seemingly, it is written into the Windows kernel API IO functions themselves, which means it's nearly impossible to bypass. This makes it extremely well suited to catching malware in the act. Unfortunately, it means it slows down every file write. When you are writing lots of small files, this delay is significant.

## Workarounds and solutions

### Use Bnd 5+

Prior to Bnd 5.0, there was a bug that caused the launcher to pause for several seconds if you had [`-runtrace`](/instructions/runtrace.html) enabled. This bug has been fixed as of 5.0 (currently in pre-release at the time of writing), so it is worthwhile upgrading to use 5.0 if you are able.

### Manually clean up temp files

Due to file locking issues, Bnd on Windows will always copy bundles into a newly launched application rather than referencing them (see [`-runnoreferences`](/instructions/runnoreferences.html)). This copy can run to a few hundred megabytes.

Again due to file locking (and its overuse of `System.exit()`), Bnd is not very good at cleaning up temporary files on Windows after it exits. So usually, the application folders are left behind after the launched application exits.

A few hundred megabytes per launch starts to add up, and if you're running off a relatively small SSD you can find yourself running out of disk space.

It is recommended that you periodically clear out these files from your temp directory. By default, the temp directory is in `%UserHome%\AppData\Local\Temp`. OSGi temp dirs created by Felix typically have the pattern: `osgi.nnnnnnnnnnnnnnnnn.fw`. Depending on which OSGi framework and which Java utilities you are using in your app, there may be others to look out for - if you are using a custom temp dir (see below), then you can simply clear out the entire directory.

### Turn off/filter real-time malware protection

You can turn off real-time malware protection in Windows settings. However, Windows 10 will only allow this situation to persist for a short time (perhaps 24 hours) before automatically turning it back on again. Also, this approach leaves your entire system without any real-time protection.

A better solution is to disable antivirus scanning for specific directories. You can find instructions on how to do this [here](https://www.windowscentral.com/how-exclude-files-and-folders-windows-defender-antivirus-scans). For maximum benefit, you should exclude:
* your Bnd workspace;
* the corresponding Eclipse workspace; and
* your temp directory (by default: `%UserHome%\AppData\Local\Temp`).
However, excluding your temp directory might be a bit of a security hole as it is likely to be a place where malware will try and write first. If you are concerned about this, see the next section.

It is possible that excluding other directories may help performance - eg, your `.m2` directory. You need to weigh the merits of excluding each folder vs the incremental security risk.

### Create a custom temp directory

As noted above, for best performance you should exclude the temp directory, however for best safety you may not wish to. A good compromise is:

* Create a temp directory somewhere else on your machine that is specifically for your development work (eg, `C:\temp`).
* Exclude this directory from antivirus scanning.
* Configure Eclipse to use use this directory by adding the `-Djava.io.tmpdir=<path to tmp>` vm arg to the `eclipse.ini` file.
* Configure your launches' `.bndrun` files to use this temp directory by using the [`-runvm`](/instructions/runvm.html) instruction (eg: `-runvm: -Djava.io.tmpdir=C:\\temp` - noting the double-backslash).

As an added bonus, this makes cleaning up the remaining temp files much easier as you can safely delete the entire directory, knowing that no other applications are using the directory.

#### Use a RAM disk

If you've got an SSD, the above tips should suffice to get good performance. On one test, launching the Bndtools test Eclipse instance itself (with over 200 bundles) took ~2s to install all the bundles for a running Eclipse instance.

However, if you're using a slower hard disk and you have enough RAM (at least 8GB), you may also consider configuring a small RAM disk of around 1GB and putting your temp directory on the RAM disk. There are a few free RAM disk utilities available for Windows.

You can also easily clear out the accumulated temp files by unmounting and re-mounting the RAM disk.
