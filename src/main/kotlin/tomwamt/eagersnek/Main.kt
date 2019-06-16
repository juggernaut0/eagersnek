package tomwamt.eagersnek

import tomwamt.eagersnek.run.Module
import tomwamt.eagersnek.run.repl
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        repl()
    } else {
        val file = File(args[0])
        Module.appDir = file.parent
        Module.fromFile(file.name)
    }
}
