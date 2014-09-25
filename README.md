Source Checksum
===============

Features
--------

The goal of this tool is compute checksums of source code.
It could be used to compute checksums on a file tree or on a Subversion location.
Checksums could be computed using the following algoriths: MD5, SHA-256 or CRC32.
In case of Subversion location, it handles externals, keywords substitution and client EOL settings.
A diff mode is available and allows to compare two locations (files changed, added or deleted).

Usage
-----

| Parameter | Description |
|-----------|-------------|
| --algorithm <arg> | The checksum algorithm to use (CRC32, MD5 or SHA256 (default)) |
| --diff | Compute version differences |
| --list | Compute checksums |
| --output <arg> | The result output file |
| --password <arg> | The Subversion user password |
| --path <arg> | The paths to compute checksums or differences |
| --url <arg> | The URLs of versionned resources to compute checksums or differences |
| --user <arg> | The Subversion user name |
