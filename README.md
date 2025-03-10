Prototype  for:
* Index file and its associated data file created by Go.
* Java reader.

Both the index file and the data files are created without any Golang encoding. Just raw bytes.

See `common.go` for the record definitions.

Note that the `filler` field in each record was inserted so the developer could use a hex dumper to readily see the contents.
