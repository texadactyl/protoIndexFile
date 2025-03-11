package main

import (
	"fmt"
	"os"
)

// Data and its associated index file.
const pathData = "saucisse.data"
const pathIndex = "saucisse.index"

// Number of records to write to the data file.
const maxRecords = 100

// Record types.
const rtypeBeginFrame = 'B'
const rtypeEndFrame = 'E'
const rtypeI64Change = 'I'

// Data record definitions.
// Record structures
type RecordBeginFrame struct {
	RID        byte
	filler     [15]byte
	ClassName  [32]byte
	MethodName [32]byte
	MethodType [32]byte
}

type RecordI64Change struct {
	RID      byte
	filler   [15]byte
	ValueOld [32]byte
	ValueNew [32]byte
}

type RecordEndFrame struct {
	RID        byte
	filler     [15]byte
	ClassName  [32]byte
	MethodName [32]byte
	MethodType [32]byte
}

// fileSize gets the current size of the file using its open file handle.
func fileSize(file *os.File) (int64, error) {
	info, err := file.Stat()
	if err != nil {
		return -1, err
	}
	return info.Size(), nil
}

// Convert a string to a fixed-length byte array with space filled on the right.
func stringToFixedBytes(s string, size int) []byte {
	padded := fmt.Sprintf("%-*s", size, s) // Left-align and pad with spaces
	return []byte(padded)[:size]           // Ensure it is exactly 'size' bytes
}
