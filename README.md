# Summonable Bot

Summonable Bot is a high-level library for use with certain types of reddit bots written in Java through my [jReddit](https://github.com/Tjstretchalot/jReddit) library.

## Documentation

This library is documented using standard Javadoc tools. That documentation is available in the docs/ folder, or [via github-pages](http://tjstretchalot.github.io/Summonable-Bot/). This Readme is intended to show a snapshot to those considering this library. For information on how to get started using this once you've decided to, see the wiki.

## Intended Users

This library is intended for use with bots that:

1. Monitor specific subreddits
2. Respond to specific "summons" that are of a specific format (i.e. $testbot hello).
3. Extract information from the summon to perform an action and then:
  - Respond to the comment or link or pm with the summon
  - Optionally pm some number of users.
  
## Features

Besides handling the above use-case in a very straightforward way, this library also provides:

### User Configuration

This library allows for extensive configuration of the underlying bot. For example, while changing the format of a summon would almost always require changes to the logic of the bot, changing the response probably would not. Thus, this library includes utility functions to accept a format string such as:

    $testbot <target username>
    
and an actual string such as:

    $testbot /u/john1

and return a map with the key-value pair containing (in essence):

    "<target username>" => "/u/john1"
    
Additionally, there are functions to easily do the opposite (apply specific values to a format string). An example would be if the bot's configuration file has the response string 

    "Testbot called with target username <target username>"

Then it could pass in the map acquired from above to get:

    "Testbot called with target username /u/john1"

For specific configuration, this library provides a FileConfiguration class that is passed around wherever configuration might be needed. This class is expected to be overridden such that it contains functions to retrieve any configuration options relevant to the client that is not stored in the same way as the database. There is also a Database abstract class that is also passed around and is expected to be overriden to allow retrieval of whatever information the bot stores in a database. A FlatFileDatabase implementation is provided for bots that require no database except for monitoring what things it has already read.

### Exponential Backoff

This library provides the Retryable class, which allows for easy wrapping of api-calls with exponential backoff. This exponential backoff is utilized in all of the calls this library does by default (such as scanning the subreddit for links and comments) for which failure most likely indicates a reddit issue rather than a bot issue.

### Apache Logging

This library uses [Apache Log4j 2.0](https://logging.apache.org/log4j/2.x/) for all of it's logging.
