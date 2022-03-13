package com.ossprj.torrent.task.locate;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

/**
 * A fileset which has been located
 */
@Data
@AllArgsConstructor
public class LocatedFileset {

    private final Path torrentPath;
    private final Path filesetPath;
    private final Path filesetParentPath;

}