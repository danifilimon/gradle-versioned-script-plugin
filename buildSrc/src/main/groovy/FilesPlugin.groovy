import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Delete

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class FilesPlugin implements Plugin<Project> {

/**
 * This build script contains two tasks. Main functionality is sorting files by their creation date or extension.
 *
 * Tasks are grouped into "files" group.
 *
 * Root folder containing files that have to be sorted is parametrized through Gradle properties with the key [tasks.files.folder].
 * Property has to be accessed through the project.ext map as direct reference doesn't work if property key is named with the dot notation.
 */
    @Override
    void apply(Project project) {
        def logger = project.logger as Logger
        logger.quiet("=============================================================================================")
        logger.quiet("                               Files Plugin                                                  ")
        logger.quiet("=============================================================================================")


/**
 * This method sorts files from specific directory and copies them to build/files directory.
 * Files are being sorted based on their extension or creation date. Default sorting type is by creation date.
 * One can explicitly define sorting type with setting following property key [tasks.files.sortType] to value:
 * <ul>
 *     <li>date</li>
 *     <li>extension</li>
 * </ul>
 *
 * Files are being resolved with the Project#file() method, as this way we get correct path to the relative path.
 * Only none hidden files are taken into consideration.
 *
 * Based on sorting type new subdirectory is created in build target and file is copied to matching subdirectory.
 * Name of the subdirectory is defined with FileDirectoryMapper.
 */
        project.tasks.register('sortFiles', DefaultTask) {
            group = "files"
            description = "Sorts files in given directory into build/files subdirectories based on the sorting type [date,extension]"

            dependsOn project.tasks.named("clean")

            doLast { // action that will be executed
                logger.quiet "==== Sorting Files ===="
                logger.quiet ""

                def fileMapper = getFileDirectoryMapper(project, logger as Logger)

                project.file(project.ext['tasks.files.folder'])
                        .listFiles({ file -> file.isFile() && !file.isHidden() } as FileFilter)
                        .each {
                            logger.quiet("Filename: " + it.name)

                            // Get directory for the file based on the sorting type
                            def directory = fileMapper.getDirectory(it)

                            // Create new directory inside build folder
                            project.mkdir project.layout.buildDirectory.dir("files" + "/" + directory + "/")

                            // Copy file to sorted directory inside build directory
                            Files.copy(it.toPath(), project.layout.buildDirectory.dir("files" + "/" + directory + "/" + it.getName()).get().asFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }

                logger.quiet ""
                logger.quiet "======================="
            }
        }

        project.tasks.register('clean', Delete) {
            group = "files"
            description = "Clean build directory"
            delete project.layout.buildDirectory
        }
    }

    /**
     * Fetches sorting type from [tasks.files.sortType] properties. If property is not present then files will be sorted by the date.
     *
     * @param project holding properties.
     * @param logger for logging missing properties
     * @return FileDirectoryMapper that actually defines sorting algorithm
     */
    static FileDirectoryMapper getFileDirectoryMapper(Project project, Logger logger) {
        def sortType
        if (project.hasProperty('tasks.files.sortType')) {
            sortType = project.ext['tasks.files.sortType']
            if (sortType == "extension") {
                return new FileDirectoryExtensionMapper()
            } else if (sortType == "date") {
                return new FileDirectoryDateMapper()
            } else if (sortType == "alphabet") {
                return new FileDirectoryAlphabetMapper()
            } else {
                throw new InvalidUserDataException("Invalid property tasks.files.sortType value provided [" + sortType + "]. Valid values are ['extension','date']")
            }
        } else {
            logger.quiet("Property [tasks.files.sortType] isn't set, default sorting will be done by creation date")
            return new FileDirectoryDateMapper()
        }
    }

/**
 * Implements mapping a file to specific directory name. The logic is based on the sorting algorithm.
 */
    interface FileDirectoryMapper {
        String getDirectory(File file)
    }

/**
 * Maps file creation date to directory name.
 */
    static class FileDirectoryDateMapper implements FileDirectoryMapper {

        @Override
        String getDirectory(File file) {
            FileTime creationTime = Files.getAttribute(Paths.get(file.path), "creationTime") as FileTime
            DateTimeFormatter fileCreationDateFormat = DateTimeFormatter.ofPattern("MM-YYYY")
            return fileCreationDateFormat.format(Instant.ofEpochMilli(creationTime.toMillis()).atZone(ZoneOffset.UTC).toLocalDate())
        }
    }

/**
 * Maps file extension to directory name.
 */
    static class FileDirectoryExtensionMapper implements FileDirectoryMapper {

        @Override
        String getDirectory(File file) {
            return getExtensionOf(file.getName())
        }

        /**
         * Get file extension from its file name.
         *
         * If filename is image.jpg then the method will return "jpg".
         *
         * @param filename from which extension will be extracted
         * @return extension of the file
         */
        static String getExtensionOf(String filename) {
            return filename.substring(filename.lastIndexOf(".") + 1)
        }
    }

/**
 * Maps file initial to alphabetical ordered directory
 */
    static class FileDirectoryAlphabetMapper implements FileDirectoryMapper {

        @Override
        String getDirectory(File file) {
            return file.getName().substring(0, 1)
        }
    }
}