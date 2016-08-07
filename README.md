PlayerTags - A bukkit plugin to give name tags and colors to players
---
PlayerTags is a simple plugin to give permission-based name tags to players.

Features:
* Customizable player list and chat tags
* Optional yearbadge (tag given to player who have played for at least
one year)
* Player-selectable name colors
* All features controllable with configuration or permissions
* Reload-safe

Tags:
* Separate strings for player list and chat
* Custom permission node determines if tag is applied
* Priority level for sorting
* Option to show only highest matching tag

Yearbadge:
* Same settings as tags
* Can be disabled globally
* Permissions can be used to give yearbadge to legacy players (players who
have played for a year before PlayerTags was loaded)

Colored names:
* Players can set with command
* Can be disabled in config
* Option to block format codes in name
* Can mix colors and formats

Commands:
* /namecolor \<color1\> \[color2\] \[color...\] - Sets a player's name color
* /playertagsreload - reloads the plugin (and updates name colors)

Permissions:
* playertags.reload
  * If a player is allowed to reload the plugin
  * default: op
* playertags.setcolor
  * If a player is allowed to set their name color
  * default: true
* playertags.setstyle
  * If a player is allowed to include styles in their name color
  * default: true
* playertags.allowyear
  * If a player is allowed to reveive a yearbadge
  * default: true
* playertags.forceyear
  * If a player is forcibly given a yearbadge
  * default: false
  
Tag configuraton:
* Section key is the ID of tag, which must be unique
* All keys are optional
* Keys:
  * enabled: boolean - set to false to skip this tag.  Default true.
  * priority: integer - higher values are treated as higher ranks.  Defaulst
  to 0.
  * chat_tag: string - the actual tag that will be added to the player's
  name in chat.  Defaults to an empty string/
  * list_tag: string - the actual tag that will be added to the player's
  name in the player list.  Defaults to an empty string.
  * permission: string - the permission node required to apply the tag.  Defaults
  to rank.<tag_id>