package org.droidmate.droidgram.merger.config

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

open class Config(
    var inputPath: Path,
    var outputPath: Path,
    var debug: Boolean = true,
    var simpleMerge: Boolean
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

        fun createConfig(args: Array<String>): Config {
            val inputDir: Path = Paths.get(args.getOrNull(0) ?: throw IOException("Missing input dir path"))
                .toAbsolutePath()

            val outputDir: Path = Paths.get(args.getOrNull(1) ?: throw IOException("Missing output dir path"))
                .toAbsolutePath()

            val debug: Boolean = (args.getOrNull(2) ?: throw IOException("Missing debug config")).toBoolean()

            val simpleMerge: Boolean = (args.getOrNull(3) ?: throw IOException("Missing simpleMerge config")).toBoolean()


            if(debug) {
                logger.debug("Input path: ${inputDir.toAbsolutePath()}")
                logger.debug("Output path: ${outputDir.toAbsolutePath()}")
                logger.debug("SimpleMerge: $simpleMerge")
            }

            check(Files.isDirectory(inputDir)) { "Input path is not a directory" }
            check(Files.isDirectory(outputDir)) { "Output path is not a directory" }
            return Config(inputDir, outputDir, debug, simpleMerge)
        }
    }
}