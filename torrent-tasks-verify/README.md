# Verify Torrent Fileset

Verify a Torrent's Fileset content to ensure it is complete 

### Torrent Files used for Verification

* Torrent files are read from the source directory and indexed by the "name" attribute
* Multiple torrent files with the same "name" will all be indexed and used to attempt verification
* The first torrent with a matching "name" that verifies will result in a VERIFIED result and any remaining will be skipped

### Torrent Verification Status

* **VERIFIED** - All torrent files present and all piece hashes match
* **INCOMPLETE** - One or more files in the torrent are missing from the fileset
* **FAILED** - All torrent files present, but some piece hashes don't match
* **UNKNOWN** - Torrent file cannot be found with name corresponding to this fileset

NOTE: 
* 0 length files are ignored when computing hashes and when computing fileset completeness
* Missing 0 length files will **not** result in an INCOMPLETE status

### Known Issues
* Unprintable UTF-8 characters in "name" and file paths will cause mis-matches and result in an INCOMPLETE 
* Some torrent clients replace unprintable UTF-8 characters with alternate printable characters, which will cause mis-matches and result in an INCOMPLETE  

### Configure using the file: 'application.yml' (YAML format)

&nbsp;&nbsp;Verify downloaded Fileset(s)

    verifyTorrentFileset:

      # Set the number of threads for verifying Torrent(s)
      # default: 1
      numberOfThreads: 2
      
      # Filesets to be verified
      # Single level of depth (filesets directly below this directory)
      filesetsDirectory: /home/user/TorrentTasks/Filesets

      # Torrents to be used for verifying Filesets
      # Single level of depth
      torrentsDirectory: /home/user/TorrentTasks/Filesets.Torrents

&nbsp;&nbsp;Move verified Fileset(s) to a separate directory

    verifyTorrentFileset:

      # Set the number of threads for verifying Torrent(s)
      numberOfThreads: 2
      
      filesetsDirectory: /home/user/TorrentTasks/Filesets
      torrentsDirectory: /home/user/TorrentTasks/Filesets.Torrents

      # If included, move verified Fileset(s) to this directory
      # Optional
      verifiedFilesetsDirectory: /home/user/TorrentTasks/Filesets.Verified

### Usage:

Ensure the application.yml is in the same directory from which you run the jar

    java -jar torrent-tasks-report-1.0.0-SNAPSHOT.jar 