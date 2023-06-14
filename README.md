# ProtoHax

<img align="right" width="159px" src="https://raw.githubusercontent.com/hax0r31337/ProtoHax/stable/icon.png">

ProtoHax is an open-source cheat for Minecraft: Bedrock Edition that works through the network layer.   
This repository contains the abstract layer of the cheat, and is designed to be platform-agnostic.

## Features
1. No modifications to Minecraft client
2. Seamless switching/adapting multiple versions
3. Full control of the packet layer

## Issues
If you notice any bugs or missing features, you can let us know by opening an issue [here](https://github.com/hax0r31337/ProtoHax/issues).   
Please notice that this is an **English-only** repository, so all issues and pull requests must be in English, if you can't speak English, please use a [translator](https://translate.google.com/).

## License
This project is subject to the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html). This does only apply for source code located directly in this clean repository. During the development and compilation process, additional source code may be used to which we have obtained no rights. Such code is not covered by the GPL license.

For those who are unfamiliar with the license, here is a summary of its main points. This is by no means legal advice nor legally binding.

*Actions that you are allowed to do:*

- Use
- Share
- Modify

*If you do decide to use ANY code from the source:*

- **You must disclose the source code of your modified work and the source code you took from this project. This means you are not allowed to use code from this project (even partially) in a closed-source (or even obfuscated) application.**
- **Your modified application must also be licensed under the GPL** 

## Example Code
Check out the [example](https://github.com/hax0r31337/ProtoHax/tree/neko-ribbon/src/test) for sample usage.

## Platform-specific Implementations

To use ProtoHax on a specific platform, you will need to use the platform-specific implementation of the cheat.   

| Platform | Repository           |
|----------|----------------------|
| Android  | [ProtoHax-Android](https://github.com/hax0r31337/ProtoHax-Android) |

## Setting up a Workspace
ProtoHax uses Gradle and JDK11, please make sure it is installed properly if you're facing an build failure.

### Main
1. Clone the repository using `git clone --recurse-submodules https://github.com/hax0r31337/ProtoHax.git`. 
2. CD into the local repository.
3. Run `gradlew publishToMavenLocal`.
4. Open the folder as a Gradle project in your preferred IDE.

## Contributing
We welcome contributions to ProtoHax! If you would like to contribute, please fork the repository and make changes as you'd like. Pull requests are welcome.

## Disclaimer
Please use ProtoHax at your own risk. We **DO NOT** take responsibility for any bans or punishments that may occur as a result of using this cheat.
