package com.ossprj.torrent.task.verify;

import com.ossprj.commons.torrent.function.VerifyTorrentContent;
import com.ossprj.commons.torrent.model.Torrent;
import com.ossprj.commons.torrent.model.TorrentVerificationReport;
import com.ossprj.commons.torrent.model.TorrentVerificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class VerifyTorrentFilesetTask {

    final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ExecutorService executorService;
    private final VerifyTorrentContent verifyTorrentContent;

    public VerifyTorrentFilesetTask(final VerifyTorrentFilesetTaskConfiguration configuration) {
        executorService = Executors.newFixedThreadPool(configuration.getNumberOfThreads());
        verifyTorrentContent = new VerifyTorrentContent(executorService);
    }

    private void indexTorrentFiles(final Map<String, List<Path>> torrentFilesByFolderName, final List<Path> torrentFiles) {

        logger.info("Indexing " + torrentFiles.size() + " torrent files");

        torrentFiles.forEach(torrentFile -> {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("Indexing: " + torrentFile.toAbsolutePath().toString());
                }
                final Torrent torrent = new Torrent(Files.readAllBytes(torrentFile));

                if (torrentFilesByFolderName.containsKey(torrent.getName())) {
                    // Add to the existing List of TorrentRecords
                    torrentFilesByFolderName.get(torrent.getName()).add(torrentFile);
                    logger.warn("Duplicate torrents with name: " + torrent.getName());
                } else {
                    // Create a new List of Path(s)
                    final List<Path> torrentPaths = new ArrayList<>();
                    torrentPaths.add(torrentFile);
                    torrentFilesByFolderName.put(torrent.getName(), torrentPaths);
                }

            } catch (Exception e) {
                logger.warn("Failed to load torrent file - " + e.getMessage() + ": " + torrentFile);
            }

        });
    }

    @Bean
    public CommandLineRunner commandLineRunner(final VerifyTorrentFilesetTaskConfiguration configuration) {

        logger.info("configuration: " + configuration);

        return (strings) -> {

            final Predicate<Path> dateFilter = configuration.getTorrentOnOrAfterDate() != null ?
                    (path) -> path.toFile().lastModified() > configuration.getTorrentOnOrAfterDate().getTime() : (path) -> true;

            final List<Path> rawPathList = Files.list(configuration.getTorrentsDirectory()).collect(Collectors.toList());
            logger.info("rawPathList.size: " + rawPathList.size());

            final List<Path> filteredPathList = rawPathList.stream().filter(dateFilter).collect(Collectors.toList());
            logger.info("filteredPathList.size: " + filteredPathList.size());

            final Map<String, List<Path>> torrentFilesByFolderName = new HashMap<>();
            indexTorrentFiles(torrentFilesByFolderName, filteredPathList);

            // Process the filesets
            final List<Path> filesets = Files.list(configuration.getFilesetsDirectory()).collect(Collectors.toList());

            filesets.forEach(fileset -> {
                try {
                    final String filesetDirectoryName = fileset.toFile().getName();

                    // If we can find a TorrentRecord(s) with the same directory name, then verify it
                    if (torrentFilesByFolderName.containsKey(filesetDirectoryName)) {

                        for (final Path torrentPath : torrentFilesByFolderName.get(filesetDirectoryName)) {

                            final Torrent torrent = new Torrent(Files.readAllBytes(torrentPath));

                            // Verify the torrent fileset
                            final TorrentVerificationReport torrentVerificationReport = verifyTorrentContent.perform(torrent, fileset);

                            // Take action based on the verification status
                            if (TorrentVerificationStatus.VERIFIED.equals(torrentVerificationReport.getStatus())) {

                                // If a verifiedFilesetsDirectory was specified move the fileset
                                final StringBuilder movedInformation = new StringBuilder();
                                if (configuration.getVerifiedFilesetsDirectory() != null) {
                                    final Path targetLocation = configuration.getVerifiedFilesetsDirectory().resolve(fileset.toFile().getName());
                                    Files.move(fileset, targetLocation);
                                    movedInformation.append(" moved to: ").append(targetLocation);
                                }
                                logger.info("VERIFIED   : " + fileset + movedInformation.toString());
                                // No need to process any more of the TorrentRecords if we got a match
                                break;

                            } else if (TorrentVerificationStatus.FAILED.equals(torrentVerificationReport.getStatus())) {
                                logger.info("FAILED     : " + fileset + " - " + torrentPath);
                            } else if (TorrentVerificationStatus.INCOMPLETE.equals(torrentVerificationReport.getStatus())) {
                                logger.info("INCOMPLETE : " + fileset + " - " + torrentPath + " - " + torrentVerificationReport.getMissingPaths());
                            }
                        }
                    } else {
                        logger.info("UNKNOWN    : " + fileset);
                    }

                } catch (IOException | InterruptedException | ExecutionException | URISyntaxException e) {
                    logger.error("Exception: ", e);
                }
            });

            executorService.shutdown();
        };
    }
}
