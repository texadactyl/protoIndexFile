package main

import (
	"encoding/binary"
	"fmt"
	"os"
)

func capture(pathData string, pathIndex string) error {
	var recordCounter = int32(0)

	// Create or open the index file (where the B-tree will be stored)
	indexFile, err := os.Create(pathIndex)
	if err != nil {
		fmt.Println("capture: Error creating index file:", err)
		return err
	}
	defer indexFile.Close()

	// Create or open the data file (where the records will be stored)
	dataFile, err := os.Create(pathData)
	if err != nil {
		fmt.Println("capture: Error creating data file:", err)
		return err
	}
	defer dataFile.Close()

	var rbfr RecordBeginFrame
	rbfr.RID = rtypeBeginFrame
	rbfr.ClassName = [len(rbfr.ClassName)]byte(stringToFixedBytes("java/lang/String", len(rbfr.ClassName)))
	rbfr.MethodName = [len(rbfr.MethodName)]byte(stringToFixedBytes("getBytes", len(rbfr.MethodName)))
	rbfr.MethodType = [len(rbfr.MethodType)]byte(stringToFixedBytes("()[B", len(rbfr.MethodType)))
	err = writeRecord(indexFile, dataFile, recordCounter, rbfr)
	if err != nil {
		fmt.Println("capture: writeRecord(RecordBeginFrame) failed, err:", err)
		return err
	}

	var ri64chg RecordI64Change
	ri64chg.RID = rtypeI64Change
	ri64chg.ValueOld = [len(ri64chg.ValueOld)]byte(stringToFixedBytes("0", len(ri64chg.ValueOld)))

	for recordCounter = 1; recordCounter < (maxRecords + 1); recordCounter++ {

		// Create a record.
		ri64chg.ValueNew = [len(ri64chg.ValueNew)]byte(stringToFixedBytes(fmt.Sprintf("%d", recordCounter), len(ri64chg.ValueNew)))

		// Write int64 change record to the data file.
		err := writeRecord(indexFile, dataFile, recordCounter, ri64chg)
		if err != nil {
			fmt.Println("capture: writeRecord(RecordI64Change) failed, err:", err)
			return err
		}

		// Update old value of int64.
		ri64chg.ValueOld = ri64chg.ValueNew
	}

	// Write end frame record.
	var refr RecordEndFrame
	refr.RID = rtypeEndFrame
	refr.ClassName = rbfr.ClassName
	refr.MethodName = rbfr.MethodName
	refr.MethodType = rbfr.MethodType
	err = writeRecord(indexFile, dataFile, recordCounter, refr)
	if err != nil {
		fmt.Println("capture: writeRecord(RecordEndFrame) failed, err:", err)
		return err
	}

	return nil
}

// writeRecord:
// * Write a record to the data file.
// * Insert its file offset associated with the record number into the B-tree.
func writeRecord(indexFile *os.File, dataFile *os.File, recordNumber int32, record any) error {

	offset, err := fileSize(dataFile)
	if err != nil {
		fmt.Printf("writeRecord: fileSize(dataFile) failed, recordNumber=%d, err: %v\n", recordNumber, err)
		return err
	}

	err = binary.Write(indexFile, binary.LittleEndian, recordNumber) // Record Number
	if err != nil {
		fmt.Printf("writeRecord: binary.Write(recordNumber) failed, recordNumber=%d, offset=%d, err: %v\n",
			recordNumber, offset, err)
		return err
	}
	err = binary.Write(indexFile, binary.LittleEndian, offset) // Byte Offset
	if err != nil {
		fmt.Printf("writeRecord: binary.Write(offset) failed, recordNumber=%d, offset=%d, err: %v\n",
			recordNumber, offset, err)
		return err
	}

	// Write record to data file
	err = binary.Write(dataFile, binary.LittleEndian, record)
	if err != nil {
		fmt.Printf("writeRecord: binary.Write(record) failed, recordNumber=%d, offset=%d, err: %v\n",
			recordNumber, offset, err)
		return err
	}

	// Return success to caller.
	return nil
}
