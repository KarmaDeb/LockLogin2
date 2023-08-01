# LockLogin2
LockLogin2 is the new version of LockLogin. With a better API and more stability<br>
<br>
LockLogin is a free open-source authentication plugin for minecraft, compatible with<br>
spigot, bungeecord and "multi-bungee". LockLogin is designed to enforce the server<br>
and the client security, with tons of premium features for free, such as pin login, 2fa<br>
and also some other unique features, such as the panic token, or virtual IDs.<br>
<br>
LockLogin is open source, which means anyone can contribute to it, even thought there<br>
official authors, LockLogin is a community plugin with no real owner who has authority<br>
over it. Any change can be voted in [RedDo discord](https://discord.gg/77p8KZNfqE).<br>
<br>
LockLogin has also multi-language support, so you can search on the official<br>
[plugin repository](https://reddo.es/panel/locklogin/?tag=lang) (in construction) for any language you need or even you can<br>
search for [community modules](https://reddo.es/panel/locklogin/?tag=module) and [official modules](https://reddo.es/panel/locklogin/modules) to extend the plugin functionalities<br>
and security.

# API
LockLogin API was made to avoid external plugins access it, for security reasons, so no external plugin is<br>
allowed to interact with the most important parts of the API. This helps your server to still safe. Anyway,<br>
LockLogin provides a way for developers to access its API, and that's by the use of modules.<br>
<br>
The LockLogin Modules are an essential part of the LockLogin API as they are the only one whith access to the<br>
plugin API, and the best is that everything that the module does gets logged, in case there's a security breach<br>
it will be easier to determine which module (if any) caused that breach.

You can learn on how to create a module for LockLogin in the [LockLogin wiki](https://reddo.es/karmadev/wiki) or<br>
If you need more help when developing the module, you can have a look at the [LockLogin docs](https://reddo.es/karmadev/locklogin/docs/)

You can also have a look on the [module examples](https://github.com/KarmaDeb/LockLogin2Examples/tree/master). (Spoiler) A plugin can extend to a module

# Migrating from LockLoginReborn (Legacy v2) (>1.1.0.9)
[LockLoginReborn](https://github.com/KarmaDeb/LockLoginReborn) is the previous version to this version of LockLogin. Any version under LockLogin v2.0.0 will be<br>
marked as legacy since this version releases officially. Which means that no more support will be provided for those<br>
versions. No more dependency updates nor bug fixes, as the support will be moved to this version of LockLogin.<br>
<br>
This also includes accounts and any kind of data managed by the plugin in the legacy version. Affortunately, LockLogin2<br>
is compatible with the legacy data systems, even thought it has been built from scratch, LockLogin2 will do its best<br>
migrating the detected legacy data into the new systems. There's no manual work for you, the plugin will do everything<br>
for you, so migrating should work just like a simple update.

# Migrating from LockLogin (Legacy v1) (<1.12.16)
Unfortunately, [LockLogin Legacy v1](https://github.com/KarmaDeb/LockLogin) won't be able to be migrated to LockLogin2<br>
as LockLogin expects the user to update the plugin when the plugin asks for it. Upgrading from a very old legacy version (v1)<br>
to a modern legacy version (v2) is neither recommended, as there are too many changes betwen those versions. LockLogin has evolved<br>
fast and in big steps. That's the reason of why updating LockLogin when asked is highly recommended.<br>
<br>
The only option you have if you want to migrate from LockLogin Legacy v1 to LockLogin2 is to whipe all your LockLogin data and perform<br>
a fresh installation.<br>
