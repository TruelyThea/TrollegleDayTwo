# Trollegle Day Two #

This library is an extension to the standard code. You can add it by copying the files into `anon/trollegle` and compiling normally. Unless there are *extreme* changes in the standard code, this library should remain compatible.

## Selected Table of Contents ##

[Query Commands](#query)

* [with](#with)
* [simulate](#simulate)
* [allWho](#allwho)
* [if](#if)

[Labels](#labels)

Higher-order Commands

* [then](#then)
* [addCommand](#addcommand)
  * [commands](#commands)
* [forEach](#foreach)
* [repeat](#repeat)

Misc. Commands

* [tell](#tell)
* [say](#say)
* [noop](#noop)

[Timers](#timers)

[Examples](#examples)

[General Information](#general)


## <a name="query"></a> Query Commands ##

The query commands all query users and provide other commands access to their properties.

Some of these commands use expressions to test which users satisfy a predicate test. The expressions are of the Polish kind, also known as prefix notation, see [Polish Notation](https://en.wikipedia.org/wiki/Polish_notation). `true` or `1` and `false` or `0`, along with a number of tokens whose value is dependent on the user, are immediately available in the environment. To see a full listing, run `/.labelHelp`. These tokens may be combined (in the prefix way) with expression-building operators `!`, `||`, `&&`, `->`, and `<->` (*not*, *or*, *and*, *only if*, and *if and only if*/*equals*) to make more complex expressions.

These commands take another command as an argument and occurrences of `$[value]` will be replaced with the selected user's `value`. A listing of possible values is available in `/.labelHelp`. Some values are special:

* `$[$]` always yields `$`, so `$[$][text]` can be used to escape `$[text]`. Also, placing only space in the brackets will produce a `$`.
* `$[choose ARGS...]` randomly chooses one of the space-delimited arguments to interpolate.
* `$[interpolate <text>]` will simply interpolate the given text.
* `$[name USER]` will try to look up the nickname of the user.
* `$[id USER]` will try to look up the numerical id of the user

The `$[value]`s are replaced by the properties of the user in the outermost scope (because they are replaced immediately before inner commands run), unless an inner scope is placed within a custom `/.addCommand` command, which "starts the inner command afresh" before interpolations have been made. So

    /.with NRP Thea /.sim 0 /.a $[nick] has $[patCount] pats.

will make the admin announce NRP's and Thea's properties.

#### <a name="with"></a> `/.with USER... <command>` ####

This command queries the user(s) with USER nick or id number, then for each given user, the caller will perform the given command after replacing the `$[value]`'s. There are two uses of this command:

1. Do a command after substituting user's properties
2. Provide one query command with another user's properties by prepending a `/.with USER` to an `/.allWho` or a `/.simulate`.

#### <a name="simulate"></a> `/.simulate USER... <command>` ####

Like `/.with`, `/.simulate` queries user(s) by nick or id. However, `/.simulate` has the selected users perform the command, after replacing `$[value]`'s with their own properties.

*Aliases:* `/.sim`

#### <a name="allwho"></a> `/.allWho <predicate> <command>` ####

This command queries all users who satisfy the predicate, then has the caller perform the command after replacing `$[value]`'s.

*Aliases:* `/.all`, `/.filter`, `/.withAll`, `/.withAllWho`

#### <a name="if"></a> `/.if USER <predicate> <command> [/.else <command>]` ####

This command will check whether the specified user satisfies the predicate. If they exist and do, it will have the caller perform the command after replacing `$[value]`'s. Else if the optional `/.else` and the user exists, it will have the caller perform the second command after replacing `$[value]`s. This command can also be used to test whether a user exists:

    /.if USER true /tell $[nick] exists
    
The easiest way to do such a test, though is to simply `/id` the user.

`/.else` is a pseudo-command. To escape `/.else` type `/..else`, to escape `/..else` type `/...else`, and so on.

*Note:* The `/.else` will not bind tightly, which is not preferable, actually. So,

    /.if NRP isAdmin /.if Thea isAdmin /tell both /.else /tell NRP /.else /tell neither

won't work as expected. It will assume that the first `/.else` is associated with the first `/.if`. Regular `/.if ... /.else /.if ... /.else /.if ...` nesting should work as expected, though. 
*Todo:* Fix this.

*Aliases:* `/.onlyIf`, `/.ifThen`, `/.withIf`, `/.ifElse`

## <a name="labels"></a> Labels ##

See the note about Polish expressions in Query Commands.

#### `/.setLabel NAME <expression>` ####

This command sets `NAME` as a token meaning the given expression. Of course the expression must be in proper Polish form, as described above.

*Aliases:* `/.label`, `/.addLabel`

#### `/.labels [filter]` ####

This command lists the labels that admins have added. Optionally if filter is provided, only labels whose names or values contain the filter will be listed.

*Aliases:* `/.listLabels`

#### `/.labelHelp` ####

This command tells which tokens (labels) and `$[value]`'s are available, and tells some (incomplete) documentation.

*Aliases:* `/.labelsHelp`

## Other Higher-Order Commands ##

A higher-order command takes another command as an argument. The query commands and timer commands are also higher order commands.


#### <a name="then"></a> `/.then <command> <command>` ####

This command will perform the the two commands in order, after parsing the commands to determine where they are separated. This command may be stacked: `/.then <command> /.then <command> <command>` or `/.then /.then <command> <command> <command>`.

This command is mostly useful in other higher-order commands. You might want to perform multiple commands per queried user, interval, or added command.

With `/.then /.with 0 /tell $[patCount] /tell done.`, there is a unique way to parse the commands: `/.with` takes another command, so the first `/tell` must be an argument to `/.with` and the second must be the start of the second command.

*You don't need t read the following details, necessarily, unless something doesn't work as expected. This is an explanation of the current limitations of `/.then`*

*Note:* There isn't always a unique way to parse commands. 

1. Textual `/word` forms in the first command will be treated as part of the next command. Consider `/.then /.with 0 /tell $[patCount] /tell hello /tell done.`. Either the `/tell hello` must be a textual element of the first `/tell` or the second `/tell` is the start of the second command, and the `/tell done.` is a textual element of the second `/tell`. The later way is what is interpreted. 
2. In commands added at run-time, currently there is no way to determine how many commands they take, so they are assumed to take no commands, if they are given as the first argument.
3. Additionally, currently there is no check for whether an `/.if` has an `/.else`, which changes how many commands `/.if` takes. So, `/.else` cannot be used in the first argument.

*Todo:* 

* Add back tracking if an `/.else` is found to allow `/.else` pseudo-commands in the first command.
* Maybe add a way to escape textual elements starting with a `/`. 
* Maybe add a way to allow multiple commands in a single `/.then` statement, but problems usually arise when considering nested `/.then`, so this solution would need to be clever. If you added a statement for force the separation of commands, you get problems with nesting. If you just keep on parsing commands pass the second, you ruin the `indexOfSecondCommand()`'s arity for `/.then`.

*Aliases:* `/.andThen`

#### <a name="addcommand"></a> `/.addCommand NAME <command>` ####

In the simplest case, command is an entire command, without argument interpolation. Then `/!COMMAND` and `/.COMMAND` are simply a shorthand for `<command>`.

If arguments are provided to `/!COMMAND`, they are, by default, appended at the end of  `<command>`, allowing `/!COMMAND` to be an initial segment of a `<command>`.

If a number following a `$` symbol is found, such as `$0` or `$22`, they are replaced by the associated argument, and only arguments following the highest number are appended by default. Zero-based indexing is used. So,

    /.addCommand test /tell $3 $1
    /.test one two three four five six seven eight
    => | four two five six seven eight
    
Sometimes you want to simply include `$7` as text. You can escape any `$` followed by a sequence of numbers by prepending a `0` to the sequence. So, `$7` is produced by `$07` and `$007` is produced by `$0007`.

Additionally, a rest operator (`...`) is available to get all the arguments that start at the specified index. So `$0...` will insert all the arguments in place, and `$12...` will insert all the arguments *after* the twelfth argument. Again you can escape `$12...` by `$012...` and `$012...` by `$0012...`. If the rest operator appears, no arguments are appended to the command by default.

*Aliases:* `/.command`, `/.setCommand`

#### <a name="commands"></a> `/.commands [filter]` ####

This command lists the commands that were added through `/.addCommand`. Optionally if filter is provided, only commands whose names or values contain the filter will be listed.

*Aliases:* `.listCommands`

#### <a name="foreach"></a> `/.forEach LIST... <command>` ####

This command makes the caller preform the command on each element of the list. `$[value]`'s are filled like a query command, but only functional values (`$`, `choose`, `interpolate`, `name`, `id`) and the special values: `index`, `value`, `collection` are accepted.

* `$[index]` gives the integer representation of the current index in the loop iteration.
* `$[value]` gives the current value in the loop iteration.
* `$[collection]` interpolates all the LIST values, separated by a space.

*Aliases:* `/.each`

#### <a name="repeat"></a> `/.repeat TIMES <command>` ####

This command simply repeats the command TIMES times.

*Aliases:* `/.rep`

## Misc. Commands ##

#### <a name="tell"></a> `/.tell <phrase>` ####

This command relays the phrase to the caller. This is useful within a `/.with` or `/.allWho` to see a user's properties.

*Aliases:* `/tell`

#### <a name="say"></a> `/.say <phrase>` ####

This command makes the caller say the phrase, regardless of whether it looks like a command. This is useful for allowing a simulation of a normal message in `/.simulate USER /say` or just for use in higher-order commands in general. Also it can be used for saying messages that would otherwise be interpreted as a command.

*Aliases:* `/say`

#### <a name="noop"></a> `/.noop ARGS...` ####

Does nothing and simply ignores the given arguments. This is useful in `/.addCommand` when you want to ignore the appended arguments by using `/.addCommand NAME /.then <actual command> /.noop`.

*Aliases:* `/say`

#### `/.extendedHelp` ####

Tells you all the secrets.

## <a name="timers"></a> Timer Commands ##

#### `/.defer TIME <command>` ####

Waits TIME before executing the command. TIME may be a number of milliseconds or of the form `1s`, `2m`, `10h`, etc.

*Aliases:* `/.delay`, `/.setDefer`

#### `/.defers` ####

List the currently scheduled defers with their ids.

*Aliases:* `/.listDefers`

#### `/.cancelDefer [ID]` #### 

Cancels the defer at id if given, or all the defers if not.

*Aliases:* `/.cancelDefers`, `/.clearDefer`, `/.clearDefers`

#### `/.interval TIME <command>` ####

Repeats the command every TIME time. TIME may be a number of milliseconds or of the form `1s`, `2m`, `10h`, etc. It also tells you the id of the interval.

*Aliases:* `/.setInterval`, `/.int`

#### `/.intervals` ####

List currently scheduled intervals with their ids.

*Aliases:* `/.listIntervals`

#### `/.cancelInterval [ID]` ####

Cancels the interval at id if given, or all the intervals if not.

*Aliases:* `/.cancelIntervals`, `/.clearInterval`, `/.clearIntervals`

## <a name="examples"></a> Examples ##

*TODO* (For now you can look at the examples in `ExtendedAdminCommands.java` if you want.)

## <a name="general"></a> General Information ##

*TODO* allow `hugCount`, commands, and labels to be saved in `/.saveAndKill` and then loaded in `--json FILE`

#### About this repository ####

I want this project to be a public endeavor. ^^

* If you spot a typo or bug, you can add an issue to the repository.
* If you tackle one of the TODOs, by all means make a pull request, and I will look at it. 
* If you want to add a new feature, add an issue to this repository and we can discuss a pull request.
