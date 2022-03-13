# Locate Fileset by Torrent

Locate fileset(s) that match up to torrent(s) and generate a report 

### Search Path

[Search Path](https://github.com/ossprj/ossprj-commons/tree/main/ossprj-commons-file#searchpath--getpathsfromsearchpaths) consists of a base path and a search depth. 

The base path will be searched at the specified depth and all directories found at that level will be included in the final list of candidate filesets.

Only directories will be included.

### Tokens Available For Use in a Report

* torrentPath
* filesetPath
* filesetParentPath

### Token Format

&nbsp;&nbsp;Tokens can be used in the report format by surrounding them with curly brackets as such
* {torrentPath}
* {filesetPath}
* {filesetParentPath}

### Default Report Format

&nbsp;&nbsp;The default report format is a CSV of all the available tokens

    "{torrentPath}","{filesetPath}","{filesetParentPath}"

### Configure using the file: 'application.yml' (YAML format)

#### Example: Default Report Format Console Output

    locateFilesetByTorrent:

      # In this example the directory structure is /Collection/Volume/Partition/Fileset(s)
      # So fileset(s) live at depth 2 below each Volume
      # i.e. 
      # /archive1-ro/Disk01/Partition1/Fileset1
      # /archive1-ro/Disk01/Partition2/Fileset2
      # /archive1-ro/Disk02/Partition3/Fileset3
      # /archive1-ro/Disk02/Partition4/Fileset4
      # ...
      filesetSearchPaths:
      - basePath: /archive1-ro/Disk01
        searchDepth: 2
      - basePath: /archive1-ro/Disk02
        searchDepth: 2

      # Source directory for torrents used to locate matching filesets
      # Currently chooses only files with the extension ".torrent"
      torrentsDirectory: /home/user/TorrentTasks/Torrents.Reseed

#### Example: Custom Report Format And File Output 

    locateFilesetByTorrent:
    
      filesetSearchPaths:
      - basePath: /archive1-ro/Disk01
        searchDepth: 2
      - basePath: /archive1-ro/Disk02
        searchDepth: 2

      torrentsDirectory: /home/user/TorrentTasks/Torrents.Reseed

      # Custom report format
      # Loads torrents into Transmission for reseed
      reportFormat: 'transmission-remote http://192.168.11.21:8084/transmission -a "{torrentPath}" --find {filesetParentPath}'

      # Generate a shell script
      reportFile: /home/user/TorrentTasks/reseed-torrents.sh

#### Example: Custom Report Format and Ignore/Quarantine Missing and Incomplete Torrents

    locateFilesetByTorrent:
    
      filesetSearchPaths:
      - basePath: /archive1-ro/Disk01
        searchDepth: 2
      - basePath: /archive1-ro/Disk02
        searchDepth: 2

      torrentsDirectory: /home/user/TorrentTasks/Torrents.Reseed

      # Custom report format
      # Loads torrents into Transmission for reseed
      reportFormat: 'transmission-remote http://192.168.11.21:8084/transmission -a "{torrentPath}" --find {filesetParentPath}'

      # Generate a shell script
      reportFile: /home/user/TorrentTasks/reseed-torrents.sh

      # If set, any torrents for which filesets cannot be found will be moved here
      missingTorrentsDirectory: /home/user/TorrentTasks/Torrents.Missing
    
      # Skip any filesets which have mismatching "Directory Content" and "Torrent Content" hashes
      # default: true
      #skipIncompleteFilesets: false
    
      # If set, any torrents for which incomplete filesets are found will be moved here
      incompleteTorrentsDirectory: /home/user/TorrentTasks/Torrents.Incomplete

### Usage:

Ensure the application.yml is in the same directory from which you run the jar

    java -jar torrent-tasks-locate-1.0.0-SNAPSHOT.jar 



