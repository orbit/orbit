---
title: "Submission Policy"
---

Overview 
----------
This document describes the submission policy for the Orbit project.

Licensing & CLA
----------
- In order to clarify the intellectual property license granted for contributions to EA open source projects you must sign the [Contributor License Agreement (CLA)](https://go.ea.com/cla).

Adhere to Coding Standards
----------
- All changes must adhere to the Orbit [[Coding Standards]].

Small and Independent Changelists
----------
-  Changelists should be as small and independent as possible. Avoid multi-feature changelists. Ideally, you should be able to remove each changelist by itself.
-  Do not work for weeks at a time and then check in all your work. Account for the possibility that you might get called away unexpectedly and somebody will have to pick up your work. Check your work in incrementally. As a general guideline, a changelist should contain no more than three days worth of work, ideally one day.


Useful Changelist Descriptions
----------
-  All changelists should identify the task or the bug that corresponds to the work. They should also describe the actual work done. This is crucial when looking for bugs or managing integrations.


All Missing Logic Must Be Specified in TODOs
----------
-  Enforces a complete understanding of the feature.
-  Help reviewers identify what use cases were not covered by the author.
-  Easier to hand-over to other developers.

All Changes Must Be Reviewed
----------
-  All changelists must be reviewed via a pull request. 

All Changes Must Be Tested
----------
-  Functional tests that exercise your code must accompany each check in
-  You should manually exercise any tests that may be relevant


Every Check-In Makes the Product Better (or no worse)
----------
-  You cannot check in any code that decreases the quality of the product.
-  In cases where you are replacing one system with another, it is preferable that either the replacement is equal to or greater than the system it is replacing, or that you leave both systems in place and allow users to switch to your replacement.