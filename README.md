# Java Shell

A POSIX-style shell implemented from scratch in Java, built as part of CodeCrafters'
["Build Your Own Shell"](https://app.codecrafters.io/courses/shell/overview) challenge.
It reads commands from a REPL prompt (`$ `), parses them, and runs them either as
shell builtins or as external programs, with support for pipelines, I/O redirection,
and background job control.

The entry point is [`src/main/java/Main.java`](src/main/java/Main.java).

## Features

### Builtin commands
- `echo` — print arguments to stdout
- `type` — report whether a command is a shell builtin or an executable found on `PATH`
- `jobs` — list background jobs and their status
- `exit` — exit the shell

### External commands
Any command not recognized as a builtin is looked up on `PATH` and executed via
`ProcessBuilder`, with the child process's stdin/stdout/stderr connected to the
terminal.

### Quoting and escaping
The tokenizer supports:
- Single quotes (`'...'`) — fully literal, no escape processing inside
- Double quotes (`"..."`) — literal, except `\` before `"`, `\`, `$`, `` ` ``, or a
  newline, which is treated as an escape
- Unquoted backslash — escapes the next character

### Pipelines
`cmd1 | cmd2` chains the standard output of one command into the standard input of
the next. Any combination of builtin and external commands is supported on either
side of the pipe.

### I/O redirection
| Operator | Effect |
|---|---|
| `>` or `1>` | Redirect stdout to a file (created if missing, overwritten if it exists) |
| `>>` or `1>>` | Redirect stdout to a file, appending instead of overwriting |
| `2>` | Redirect stderr to a file (overwrite) |
| `2>>` | Redirect stderr to a file (append) |

Only the stream you redirect is affected — for example, `cmd > out.txt` still
prints errors to the terminal, since only stdout is captured.

### Background jobs and job control
Appending `&` to a command runs it in the background instead of blocking the
shell:

```
$ sleep 30 &
[1] 84470
$
```

- Job numbers start at 1 and are reused: once a job finishes and is reported, its
  number becomes available for the next background command.
- The shell checks all background jobs before each prompt and reports any that
  have finished (`Done`) exactly once before removing them from the job table.
- `jobs` lists all currently tracked jobs in the format:
  ```
  [1]+  Running                 sleep 30 &
  ```
  The `+` marks the most recently started job still in the table, `-` marks the
  next most recent, and older jobs are unmarked.

## Running it

Requires `mvn` locally.

```sh
./your_program.sh
```

This compiles and runs [`src/main/java/Main.java`](src/main/java/Main.java) as the shell's entry point.

