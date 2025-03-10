package main

import "os"

var step = 2

func main() {

	// Capture data.
	err := capture(pathData, pathIndex)
	if err != nil {
		os.Exit(1)
	}
	
}
