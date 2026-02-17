# migrate the erte to Vaadin 25

## wording
erte = enhanced rich text editor - this project basically
rte = vaadin's built-in "core" rich text editor (commercial component)
rte 1 = the old version of the rte. in 24 or less. uses quill 1.
rte 2 or rte 25 = the new version of the rte. starting from Vaadin 25. uses quill 2
erte 1 = the old / unmigrated erte, based on rte 1
erte 2 = the new / migrated erte, based on rte 2

## description
the erte 1 is a fork of the rte 1 from some while ago now. due to the fork nature, the code base gap between erte 1 and rte 1 has
grown and new features from the rte 1 have not been moved into the erte 1. also quill 1 is outdated and rte 2 uses quill 2 now.

target of this document is to describe the migration from erte 1 to erte 2, using rte 2 as its new base.

## migration
there are multiple steps, that we have to accomplish for a migration
- update the project base to vaadin 25 (vaadin 25.0.x, no prereleases or anything unstable)
- recreate the erte from scratch using the latest version of the rte 2 
- migrating any additional features from the erte 1 to the erte 2, that are not already included in the rte 2
- migrating the table extension

### features
I cannot say for sure, which features of the erte 1 are maybe available in rte 2. at least list indention should be built in in quill 2.
So to ensure non duplicates, it has to be analysed in quill 2 and the rte 2, which features of the erte are not to be migrated.

### table extension
the source of the table addon is a bit tricky. the base was a fork of a quill 2 addon for quill 1, which has then be modified to work with the
erte 1. therefore it would make sense to not simply migrate that extension code, but instead use the original addon for quill 2 and work the
vaadin / erte modifications into that addon. But I am not sure, if that works, so that has to be analyized carefully before hand. 

## ensure non regression / use cases
any features, that are available in the erte have to be available in the rte. 

to accomplish this, before any plans are done regarding migration a full description of all use cases of the erte has to be worked out
and written down, so that agents can compare the migration result with the use cases.

## ensure updatability
we do not want to make the same mistake as before by having
a pure copy of the rte, on which the erte is based. Instead we will try to extend the rte 2, so that future updates / fixes / new features,
that are made for the rte 2 are automatically available for the erte 2.

this means, that we will not simply copy all the code and merge in the features of the erte. instead the erte should check, where it can
hook into existing code base at runtime. 

For instance the toolbar of the rte is not dynamically modifiable with new items. the erte instead provides slots for each category.
These slots are currently hardcoded in the erte toolbar, which is a full copy of the rte toolbar. when the rte toolbar gets new content,
the erte toolbar does not. So an alternative solution would be to inject those erte slots at runtime into the toolbar of the rte by using
selectors and js code (just an idea, has to be checked or come up with a better one).

in a similar way i would imagine the erte updatability and new architecture. This will be the hardest part, since it means a lot of
working around the inflexibility of the rte 2. 

