# COLOTOK
COLOTOK; Cocoa LogTool for Kotlin  

![](https://img.shields.io/static/v1?label=kotlin-jvm&message=1.8.10&color=magenta)
![](https://img.shields.io/static/v1?label=jdk&message=11&color=magenta)
[![](https://jitpack.io/v/milkcocoa0902/colotok.svg)](https://jitpack.io/#milkcocoa0902/colotok)


# Feature
✅ Print log with color  
✅ Formatter  
✅ Print log where you want  
　🌟 ConsoleProvider  
　🌟 FileProvider    
　🌟 StreamProvider  
✅ Log Rotation  
　🌟 SizeBaseRotation  
　🌟 DateBaseRotation(; DurationBase)    
✅ Customize output location  
　🌟 example [print log into slack](https://github.com/milkcocoa0902/colotok_slack_integration_sample)  
✅ Structure Logging  


# Integration
basic dependency

```kotlin
repositories {
    mavenCentral()
    // add this line
    maven(url =  "https://jitpack.io" )
}

dependencies {
    // add this line
    implementation("com.github.milkcocoa0902:colotok:0.1.9")
}
```

if you use structure logging or create your own provider, you need to add `kotlinx-serialization`

```kotlin

plugins {
    // add this.
    // set version for your use
    kotlin("plugin.serialization") version "1.9.21"
}

dependencies {
    // add this line to use KSerializer<T> and @Serializable
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")
}

```


# Usage
## Configuration
configure colotok with code.  
see below.

```kotlin
val logger = LoggerFactory()
    .addProvider(ConsoleProvider())
    .addProvider(FileProvider(Path.of("./test.log")))
    .getLogger()

```

more details config
```Kotlin
val fileProvider: FileProvider
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        // show above info level in console
        logLevel = LogLevel.INFO
    })
    .addProvider(FileProvider(Path.of("./test.log")){
        // write above trace level for file
        logLevel = LogLevel.TRACE
        
        // memory buffering to save i/o
        enableBuffer = true
        
        // memory buffer size, if buffer excced this, append to file
        bufferSize = 2048
        
        // use size base rotation
        rotation = SizeBaseRotation(size = 4096)
    }.apply {
        fileProvider = this
    })
    .getLogger()

logger.trace("TRACE LEVEL LOG")
logger.debug("DEBUG LEVEL LOG")
logger.info("INFO LEVEL LOG")
logger.warn("WARN LEVEL LOG")
logger.error("ERROR LEVEL LOG")

```

## Print
now, you can print log into your space.

```kotlin
logger.trace("TRACE LEVEL LOG")
logger.debug("DEBUG LEVEL LOG")
logger.info("INFO LEVEL LOG")
logger.warn("WARN LEVEL LOG")
logger.error("ERROR LEVEL LOG")

logger.atInfo {
    print("in this block")
    print("all of logs are printed out with INFO level")
}

// or you can add additional parameters
logger.info("INFO LEVEL LOG", mapOf("param1" to "a custom attr"))


// you may need to flash, if log cache is enabled for `FileProvider`
fileProvider.flush()

```

## Formatter(Text)
colotok has builtin text formatter.
1. PlainTextFormatter
2. SimpleTextFormatter
3. DetailTextFormatter

### 1. PlainFormatter
this formatter shows as below style's log

```
logger.info("message what happen")

// message what happen
```

### 2. SimpleFormatter
this formatter shows as below style's log

```Kotlin
logger.info("message what happen")

// 2023-12-29 12:23:14.220383  [INFO] - message what happen
```

### 3. DetailFormatter
this formatter shows as below style's log

```Kotlin
logger.ingo("message what happen", mapOf("param1" to "a custom attribute"))

// 2023-12-29T12:21:13.354328+09:00 (main)[INFO] - message what happen, additional = {param1=a custom attribute}
```

## Formatter(Structure)
colotok has builtin structured formatter.
1. SimpleStructureFormatter
2. DetailStructureFormatter

if you has a class
```kotlin
@Serializable
class LogDetail(val scope: String, val message: String): LogStructure

@Serializable
class Log(val name: String, val logDetail: LogDetail): LogStructure
```

### 1. SimpleStructureFormatter
this formatter shows bellow style's log

```Kotlin
logger.info(
    Log(
        name = "illegal state",
        LogDetail(
            "args",
            "argument must be greater than zero"
        )
    ),
    Log.serializer()
)

// {"name":"illegal state","logDetail.scope":"args","logDetail.message":"argument must be greater than zero","level":"INFO","date":"2023-12-29"}


logger.info("message what happen")

// {"msg":"message what happen","level":"INFO","date":"2023-12-29"}
```
### 2. DetailStructureFormatter
this formatter shows bellow style's log

```Kotlin
logger.info(
    Log(
        name = "illegal state",
        LogDetail(
            "args",
            "argument must be greater than zero"
        )
    ),
    Log.serializer(),
    // you can pass additional attrs
    mapOf("a" to "afeafseaf")
)

// {"name":"illegal state","logDetail.scope":"args","logDetail.message":"argument must be greater than zero","level":"INFO","thread":"main","a":"afeafseaf","date":"2023-12-29T12:27:22.559733+09:00"}


logger.info("message what happen")

// {"msg":"message what happen","level":"INFO","thread":"main","a":"BBBBB","date":"2023-12-29T12:27:22.5908+09:00"}
```



## Provider
colotok has builtin provider. Provider is used for output log.

1. ConsoleProvider
2. FileProvider
3. StreamProvider

### 1. ConsoleProvider
this provider outputs log into console with ansi-color

### 2. FileProvider
this provider output log into file without ansi-color.

### 3. StreamProvider
this provider output log into stream where you specified.  

### 4. Customize
you can also output to remote or sql or others by create own provider.  
example

#### define custom provider
if you want to write log into slack, you create a SlackProvider like this

```kotlin
class SlackProvider(config: SlackProviderConfig): Provider {
    constructor(config: SlackProviderConfig.() -> Unit): this(SlackProviderConfig().apply(config))

    class SlackProviderConfig() : ProviderConfig {
        var webhook_url: String = ""

        override var logLevel: LogLevel = LogLevel.DEBUG

        override var formatter: Formatter = DetailTextFormatter
    }

    private val webhookUrl = config.webhook_url
    private val logLevel = config.logLevel
    private val formatter = config.formatter

    override fun write(name: String, msg: String, level: LogLevel, attr: Map<String, String>) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }
        kotlin.runCatching {
            webhookUrl.httpPost()
                .appendHeader("Content-Type" to "application/json")
                .body("""
            {"text": "${formatter.format(msg, level, attr)}"}
        """.trimIndent())
                .response()
        }.getOrElse { println(it) }
    }

    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: LogLevel,
        attr: Map<String, String>
    ) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }
        kotlin.runCatching {
            webhookUrl.httpPost()
                .appendHeader("Content-Type" to "application/json")
                .body("""
            {"text": "${formatter.format(msg, serializer, level, attr)}"}
        """.trimIndent())
                .response()
        }.getOrElse { println(it) }
    }
}
```

#### use your provider
now you can use SlackProvider to write the log into slack.

```kotlin
val logger = LoggerFactory()
        .addProvider(ConsoleProvider{
            formatter = DetailTextFormatter
            logLevel = LogLevel.DEBUG
        })
        .addProvider(SlackProvider{
            webhook_url = "your slack webhook url"
            formatter = SimpleTextFormatter
            logLevel = LogLevel.WARN
        })
        .getLogger()
```

#### print the log
```kotlin
logger.info("info level log")
// written the log only console

logger.error("error level log")
// written the log both of console and slack
```



## LogLevel
1. TRACE (all log)
2. DEBUG (ignore TRACE)
3. INFO (ignore DEBUG and TRACE)
4. WARN (only WARN or ERROR)
5. ERROR (only one)