# Topaz - A simple FOSS Anti-VPN plugin for Velocity
### Inspired by [egg82's/Laarryy's Anti-VPN plugin](https://github.com/Laarryy/Anti-VPN).

## Installation guide:
You need:
* Velocity 3.1.2+
* Java 17+
* Email address
* A custom plan from GetIPIntel **(required only if you will be making more than 500 requests a day)**

1. Download the plugin from the [releases tab](https://github.com/MrZomka/Topaz/releases).
1. Install the plugin on your Velocity proxy.
1. Start the proxy.
1. In the config file and enter your email, __***OTHERWISE THE PLUGIN WILL NOT WORK!!!***__
1. Restart the proxy.

And you're good to go! :)

## FAQ:
Q: Can I change the clear cache interval?
A: Yeah! Just keep in mind that the interval there is in seconds, not minutes or hours. ;)

Q: I get error -5 when trying to join my server!
A: You ran out of requests! You need to buy more from GetIPIntel or set the clear cache interval to something higher.

Q: What are the permissions that Topaz checks for?
A: `topaz.reload` for the config reloading command (`/topaz`) and `topaz.bypass` for the IP quality score check bypass.

Q: What permission plugin do you recommend?
A: [LuckPerms never failed me. :)](https://luckperms.net)

## Support is offered on my [Discord server](https://discord.gg/y3VdCeJaC2).