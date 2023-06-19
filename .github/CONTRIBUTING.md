# How to contribute

You want help me make BlueMap better? Awesome!<br>
Here you learn how it works and what you need to know before contributing.

>**Please read this before creating an Issue or a PullRequest!** Issues/PR's that don't follow these instructions will be closed!<br>
>**Issues are NOT for asking questions!** If you have a question, please use our [Discord](https://discord.gg/zmkyJa3) or [Reddit](https://www.reddit.com/r/BlueMap/)!

### Table of Contents
- [Reporting a Bug](#reporting-a-bug)
- [Suggesting a new feature or change](#suggesting-a-new-feature-or-change)
- [Creating a Pull-Request](#creating-a-pull-request)

## Reporting a Bug
The first thing you need to do, is to make sure what you found is actually a bug:<br>
- A bug is an unintended behaviour of an implemented feature.
BlueMap is not "done", there are quite a lot of features missing!
So, if something doesn't work because it is not implemented yet, its not a bug.
If you are not sure, you can briefly ask about it in our [Discord](https://discord.gg/zmkyJa3) before creating an Issue. :)
- Make sure you tested it well enough to be sure it's not an issue on your end. If something doesn't work for you but for everyone else, its probably **not** a bug!

Also, please make sure no one else has already reported the same or a very similar bug!
If you have additional information for an existing bug-report, you can add a comment to the already existing Issue :)

To report your bug, please open a [new Issue](https://github.com/BlueMap-Minecraft/BlueMap/issues/new?template=bug_report.md) with the `Bug report`-template and follow these guidlines:

### Guidlines for a good Bug-Issue
**A short, informative Title**<br>
Your Issue should have a short but informative title, which makes it possible to distinquish it from- and easily recognize it in-between- other Issues.
If someone else finds the same bug, they should be able to find your Issue only based on the title!

**A detailed description**<br>
Describe your bug in as much detail as possible:
- What did you do before it happened? (How can the bug be reproduced?)
- What did you expect to happen?
- What happened instead?
- CONTEXT!!
  - The exact BlueMap-Version (e.g. the name of the used .jar file)
  - The used os and platform (Windows/Linux, Spigot/Paper/Forge/Fabric/Sponge)
  - Has the world been generated using any minecraft-mods?
  - etc..
- Is there a log- or a config-file that might help? Include it.
- Maybe add a screenshot or video for illustration.

**Well formatted and structured**<br>
Make sure your Issue is easy to read and not a mess:
- Use paragraphs to structure your issue.
- Use [Markdown](https://guides.github.com/features/mastering-markdown/) to add headings and formatting.
- Use codeblocks for log-snippets.
- Upload full logs as file-attachments or use a paste-site like [GitHub Gists](https://gist.github.com/) or [Pastebin](https://pastebin.com/).

**One Issue, one bug**<br>
Create a separate Issue for each bug you find! Issues that contain more than one bug will be closed!

## Suggesting a new feature or change
Please use our [discord](https://discord.gg/zmkyJa3)s #suggestions channel to pitch new ideas. 
We will discuss them there and if they are considered, I'll add an issue/note to out [TODO](https://github.com/orgs/BlueMap-Minecraft/projects/2/views/1)-Board!

## Creating a Pull-Request
If you want to develop a new PR, please run your Idea by me first in our [discord](https://discord.gg/zmkyJa3)!
We can discuss details there, since I have a lot of future plans in my head that are not written anywhere, and they might need to be considered
when implementing your feature!  
*(Also, I tend to be quite picky about certain implementation styles and details ^^')*

**Please keep in mind that any feature you implement will need to be maintained in the future by me.
For this reason I will only accept PR's for features that I deem to be useful, maintainable, in-scope of the project and
worth it's maintenance-workload!**

Ofc the usual "good code quality..." stuff, i think that's common sense.  
Try to match the existing code-style.  
Don't add new libraries/dependencies without my ok.  
Hacky stuff is not allowed =)
