# VOTL
 [![Build status](https://github.com/FileEditor97/VOTL/actions/workflows/build.yml/badge.svg)](https://github.com/FileEditor97/VOTL/actions/workflows/build.yml)  
 Voice of the Lord - discord bot written in Java using JDA library.  
 Functions: server moderation and sync blacklists, custom voice channels and verification, ticketing.  

Visit https://votl.fileeditor.dev/ to learn more about available commands and to view documentation.

## Contribute
- [**Help translate!**](https://crowdin.com/project/voice-of-the-lord)
- [Suggest changes to the documentation](https://github.com/FileEditor97/VOTL-docs)

## Download or building
 **Java 21 required!**  
 Stable JAR file can be downloaded from latest Release [here](https://github.com/FileEditor97/VOTL/releases/latest).  
 Additional Snapshot builds can be accessed [here](https://github.com/FileEditor97/VOTL/actions/workflows/build.yml).
 
 Build from source using `.\gradlew build`.

## Installation
1. `git clone https://github.com/FiLe-group/VOTL.git`
2. `cd VOTL`
3. `.\gradlew build`
4. Finally `.\linux-start.sh`

## Config file
 data/config.json:
 ```json
 {
	"bot-token": "",
	"owner-id": "owner's ID",
	"dev-servers": [
		"dev server's IDs"
	],
	"webhook": "link to webhook, if you want to receive ERROR level logs"
 }
 ```

## Inspiration/Credits
 Thanks to Chew (JDA-Chewtils and Chewbotcca bot) and jagrosh (JDA-Utilities)  
 [PurrBot](https://github.com/purrbot-site/PurrBot) by Andre_601 (purrbot.site)  
 [AvaIre](https://github.com/avaire/avaire) by Senither  
 Ryzeon & Inkception for [Discord (JDA) HTML Transcripts](https://github.com/Ryzeon/discord-html-transcripts)
