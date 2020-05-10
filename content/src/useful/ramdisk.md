ramdisk
=======

Use the following to create a 1MB RAM-backed mount:

```bash
_size=2048
$ diskutil erasevolume HFS+ secure $(hdiutil attach -nobrowse -nomount ram://$_size)
```

Unmount:

```bash
$ diskutil eject /Volumes/RAM\ Disk/
```
