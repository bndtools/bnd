---
layout: default
class: Project
title: -upto VERSION 
summary: Specify the highest compatibility version, will disable any incompatible features added after this version. 
---

The `-upto` instruction specifies the highest version of bnd features that your project should be compatible with. When you set this instruction, any features or behaviors introduced after the specified version will be disabled, ensuring compatibility with earlier versions of bnd.

This is useful for maintaining backward compatibility or for projects that need to avoid adopting new behaviors automatically. If `-upto` is not set, bnd assumes the latest version and enables all features by default.

In summary, use `-upto` to control the introduction of new features and maintain a stable build environment as bnd evolves.