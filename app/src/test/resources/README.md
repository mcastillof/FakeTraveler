# Test resources

`Zavicaj-Mandarina.kmz` is a real KMZ file used by `KmzFileTest` to verify
that `KmzFile#getSize()` returns the size reported by the content resolver.
The test loads the file from the classpath and uses its byte length as the
expected size.


