package com.ossprj.torrent.task.locate;

import com.ossprj.commons.file.function.CalculateDirectoryContentHash;
import com.ossprj.commons.file.function.FindAllDirectoriesAtDepth;
import com.ossprj.commons.text.function.ReplaceTokens;
import com.ossprj.commons.torrent.function.CalculateTorrentContentHash;
import com.ossprj.commons.torrent.model.Torrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Component
public class LocateFilesetByTorrentTask {

    final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    final FindAllDirectoriesAtDepth findAllDirectoriesAtDepth = new FindAllDirectoriesAtDepth();
    final ReplaceTokens replaceTokens = new ReplaceTokens();

    final CalculateDirectoryContentHash calculateDirectoryContentHash = new CalculateDirectoryContentHash();
    final CalculateTorrentContentHash calculateTorrentContentHash = new CalculateTorrentContentHash();

    @Bean
    public CommandLineRunner commandLineRunner(final LocateFilesetByTorrentTaskConfiguration configuration) {

        logger.debug("configuration: " + configuration);

        return (strings) -> {

            // Collect all the fileset paths we want to process
            final List<Path> filesetPaths = new LinkedList<>();

            configuration.getFilesetSearchPaths().forEach(filesetSearchPath -> {
                filesetPaths.addAll(findAllDirectoriesAtDepth.apply(filesetSearchPath.getBasePath(), filesetSearchPath.getSearchDepth()));
            });

            if (logger.isDebugEnabled()) {
                logger.debug("filesetPaths: " + filesetPaths);
            }

            // Make them searchable by directory name
            final Map<String, Path> pathsByName = new HashMap<>();
            for (final Path filesetPath : filesetPaths) {
                // Warn about duplicate folders until we can find a reliable way to dedupe or validate fileset integrity
                // (short of validating torrent against the whole fileset, which is time consuming but 100% accurate)
                // DirectoryContentHash vs TorrentContentHash is a viable middle ground that's likely 99.99...% correct
                if (pathsByName.containsKey(filesetPath.toFile().getName())) {
                    final Path existingPath = pathsByName.get(filesetPath.toFile().getName());
                    logger.warn("Multiple filesets with same name found. At this point last found wins: " + filesetPath + " replaces " + existingPath);
                }
                pathsByName.put(filesetPath.toFile().getName(), filesetPath);
            }

            final List<LocatedFileset> locatedFilesets = new ArrayList<>();

            // Should we assume all torrents will have the extension ".torrent" (and filter as we do here)
            // OR assume the user is competent enough to only have torrents in the source directly and process all files
            // TODO: Simple check to see if there are no files in the torrent source directory (user error) and avoid NPE here
            final List<File> torrentFiles = Arrays.asList(configuration.getTorrentsDirectory().toFile().listFiles(file -> file.getName().endsWith(".torrent")));
            for (final File torrentFile : torrentFiles) {
                final Torrent torrent = new Torrent(Files.readAllBytes(torrentFile.toPath()));

                if (pathsByName.containsKey(torrent.getName())) {
                    final Path torrentPath = torrentFile.toPath();
                    final Path filesetPath = pathsByName.get(torrent.getName());
                    final Path filesetParentPath = pathsByName.get(torrent.getName()).getParent();

                    // Calculate "directory content" and "torrent content" hashes and compare
                    // If they don't match then the fileset is incomplete and should be skipped
                    // If they do match its still not a guarantee that the fileset content matches the torrent piece hashes
                    final String directoryContentHash = calculateDirectoryContentHash.apply(filesetPath);
                    final String torrentContentHash = calculateTorrentContentHash.apply(torrent);
                    final boolean hashesMatch = directoryContentHash.equals(torrentContentHash);

                    if (hashesMatch) {
                        logger.debug("Hashes Match (true) : " + directoryContentHash + " - " + torrentContentHash + " : " + filesetPath + " - " + torrentFile);
                    } else {
                        logger.warn("Hashes Match (false) : " + directoryContentHash + " - " + torrentContentHash + " : " + filesetPath + " - " + torrentFile);
                    }

                    // If skipIncompleteFilesets is true and this fileset is incomplete then skip it ...
                    if (configuration.getSkipIncompleteFilesets() && !hashesMatch) {
                        logger.info("Skipping Incomplete Fileset: " + filesetPath + " - " + torrentFile);
                    } else {
                        // ... otherwise add it to the list
                        locatedFilesets.add(new LocatedFileset(torrentPath, filesetPath, filesetParentPath));
                    }

                    // If incompleteTorrentPath is non-null AND this fileset is incomplete move the torrent
                    if (configuration.getIncompleteTorrentsDirectory() != null && !hashesMatch) {
                        final Path incompletePath = configuration.getIncompleteTorrentsDirectory().resolve(torrentFile.getName());
                        // If we don't already have it in the incomplete folder then move it
                        if (!incompletePath.toFile().exists()) {
                            Files.move(torrentFile.toPath(), incompletePath, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            // Else delete it
                            Files.deleteIfExists(torrentFile.toPath());
                        }
                    }

                } else {
                    logger.info("Skipping Missing Torrent: " + torrentFile);
                    // If a missingTorrentPath is non-null AND this fileset is missing move the torrent
                    if (configuration.getMissingTorrentsDirectory() != null) {
                        final Path missingPath = configuration.getMissingTorrentsDirectory().resolve(torrentFile.getName());
                        // If we don't already have it in missing folder then move it
                        if (!missingPath.toFile().exists()) {
                            Files.move(torrentFile.toPath(), missingPath, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            // Else delete it
                            Files.deleteIfExists(torrentFile.toPath());
                        }
                    }
                }
            }

            final List<String> generatedStrings = new LinkedList<>();

            for (final LocatedFileset locatedFileset : locatedFilesets) {

                final Map<String, String> tokenValues = new HashMap<>();
                tokenValues.put("torrentPath", locatedFileset.getTorrentPath().toString());
                tokenValues.put("filesetPath", locatedFileset.getFilesetPath().toString());
                tokenValues.put("filesetParentPath", locatedFileset.getFilesetParentPath().toString());

                generatedStrings.add(replaceTokens.apply(configuration.getReportFormat(), tokenValues));
            }

            if (configuration.getReportFile() != null) {
                final String fileContent = generatedStrings.stream().reduce((a, b) -> a + "\n" + b).orElse("");
                Files.write(configuration.getReportFile(), fileContent.getBytes(StandardCharsets.UTF_8));
            } else {
                generatedStrings.forEach(System.out::println);
            }

        };
    }
}
