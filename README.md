[![Build Status](https://jenkins.karmadev.es/job/LockLogin/badge/icon)](https://jenkins.karmadev.es/job/LockLogin/)

# LockLogin2
LockLogin2 is the new version of LockLogin. With a better API and more stability
<br>
LockLogin is a free open-source authentication plugin for minecraft, compatible with
spigot, bungeecord and "multi-bungee". LockLogin is designed to enforce the server
and the client security, with tons of premium features for free, such as pin login, TOTP
and also some other unique features, such as the panic token, or virtual IDs.
<br>
LockLogin is open source, which means anyone can contribute to it, even thought there
official authors, LockLogin is a community plugin with no real owner who has authority
over it. Any change can be voted in [RedDo discord](https://discord.gg/77p8KZNfqE).
<br>
LockLogin has also multi-language support, so you can search on the official
[plugin repository](https://reddo.es/panel/locklogin/?tag=lang) (in construction) for any language you need or even you can
search for [community modules](https://reddo.es/panel/locklogin/?tag=module) and [official modules](https://reddo.es/panel/locklogin/modules) to extend the plugin functionalities
and security.

# API
LockLogin API was made to avoid external plugins access it, for security reasons, so no external plugin is
allowed to interact with the most important parts of the API. This helps your server to still safe. Anyway,
LockLogin provides a way for developers to access its API, and that's by the use of modules.
<br>
The LockLogin Modules are an essential part of the LockLogin API as they are the only one whith access to the
plugin API, and the best is that everything that the module does gets logged, in case there's a security breach
it will be easier to determine which module (if any) caused that breach.

You can learn on how to create a module for LockLogin in the [LockLogin wiki](https://reddo.es/karmadev/wiki) or
If you need more help when developing the module, you can have a look at the [LockLogin docs](https://reddo.es/karmadev/locklogin/docs/)

You can also have a look on the [module examples](https://github.com/KarmaDeb/LockLogin2Examples/tree/master).
<details> 
  <summary>Can I use my existing plugin as a module?</summary>
    Yes, unlike in LockLoginReborn (LockLogin legacy v2), a plugin can be extended into a module, without even the need
of making your plugin have module-required files or implementing the LockLogin module class, instead, you simply need to call 
a LockLogin method which asks your plugin as parameter in order to extend your plugin into a module virtually
</details>

# Migrating from LockLoginReborn (Legacy v2) (>1.1.0.9)
[LockLoginReborn](https://github.com/KarmaDeb/LockLoginReborn) is the previous version to this version of LockLogin. Any version under LockLogin v2.0.0 will be
marked as legacy since this version releases officially. Which means that no more support will be provided for those
versions. No more dependency updates nor bug fixes, as the support will be moved to this version of LockLogin.
<br>
This also includes accounts and any kind of data managed by the plugin in the legacy version. Fortunately, LockLogin2
is compatible with the legacy data systems, even thought it has been built from scratch, LockLogin2 will do its best
migrating the detected legacy data into the new systems. There's no manual work for you, the plugin will do everything
for you, so migrating should work just like a simple update.
<br>
Even though LockLogin will try to migrate all the legacy data, there's still some information that WON'T be migrated, those are:
- Name tables
- UUID tables
- IP address tables
- 2FA scratch codes (now renamed to TOTP Scratch codes)
- Locations (last location and spawn)

Work is in progress to add support for locations and 2fa scratch codes, but those might arrive later

# Migrating from LockLogin (Legacy v1) (<1.12.16)
Unfortunately, [LockLogin Legacy v1](https://github.com/KarmaDeb/LockLogin) won't be able to be migrated to LockLogin2
as LockLogin expects the user to update the plugin when the plugin asks for it. Upgrading from a very old legacy version (v1)
to a modern legacy version (v2) is neither recommended, as there are too many changes betwen those versions. LockLogin has evolved
fast and in big steps. That's the reason of why updating LockLogin when asked is highly recommended.
<br>
The only option you have if you want to migrate from LockLogin Legacy v1 to LockLogin2 is to whipe all your LockLogin data and perform
a fresh installation.<br>
