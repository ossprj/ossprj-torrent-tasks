package com.ossprj.torrent.task.locate;

import com.ossprj.commons.file.model.SearchPath;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.List;

@Component
@ConfigurationProperties("locate-fileset-by-torrent")
@Data
@Validated
public class LocateFilesetByTorrentTaskConfiguration {

    @NotNull
    private List<SearchPath> filesetSearchPaths;

    @NotNull
    private Path torrentsDirectory;

    private Path missingTorrentsDirectory;

    private Path incompleteTorrentsDirectory;

    private Boolean skipIncompleteFilesets = Boolean.TRUE;

    private String reportFormat = "\"{torrentPath}\",\"{filesetPath}\",\"{filesetParentPath}\"";

    private Path reportFile;

}
