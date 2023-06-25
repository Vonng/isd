package main

import (
	"flag"
	"fmt"
	"github.com/Vonng/isd/parser/isdd"
	"github.com/Vonng/isd/parser/isdh"
	"log"
	"os"
	"runtime/pprof"
	"strings"
)

var (
	subCommand   = "parse" // daily | hourly | etc...
	inputPath    string    // input path
	outputPath   string    // output path
	profilePath  string    // profile path (pprof)
	dedupeMode   string    // dedupe level: raw | ts | hour
	extraColumns string    // extra columns
	verbose      bool      // verbose mode
	help         bool      // help mode
)

func Usage() {
	fmt.Println(`
NAME
	isd -- Intergrated Surface Dataset Parser

SYNOPSIS
	isd daily   [-i <input|stdin>] [-o <output|stout>] [-v]
	isd hourly  [-i <input|stdin>] [-o <output|stout>] [-v] [-d raw|ts-first|hour-first]

DESCRIPTION
	The isd program takes noaa isd daily/hourly raw tarball data as input.
	and generate parsed data in csv format as output. Works in pipe mode

	cat data/daily/2023.tar.gz | bin/isd daily -v | psql ${PGURL} -AXtwqc "COPY isd.daily FROM STDIN CSV;" 

	isd daily  -v -i data/daily/2023.tar.gz | psql ${PGURL} -AXtwqc "COPY isd.daily FROM STDIN CSV;"
	isd hourly -v -i data/hourly/2023.tar.gz | psql ${PGURL} -AXtwqc "COPY isd.hourly FROM STDIN CSV;"

OPTIONS
	-i  <input>     input file, stdin by default
	-o  <output>    output file, stdout by default
	-p  <profpath>  pprof file path, enable if specified
	-d              de-duplicate rows for hourly dataset (raw, ts-first, hour-first)
	-v              verbose mode
	-h              print help
`)
	os.Exit(0)
}

// RunDailyParser will launch daily raw data parser
func RunDailyParser() {
	p := isdd.NewProcessor(inputPath, outputPath)
	p.Verbose = verbose
	if err := p.Run(); err != nil {
		panic(err)
	}
	log.Printf("ISD Daily Parser Done")
}

// RunHourlyParser will launch hourly raw data parser
func RunHourlyParser() {
	p := isdh.NewProcessor(inputPath, outputPath)
	p.Verbose = verbose
	p.DedupeMode = dedupeMode
	p.ExtraColumns = strings.Split(extraColumns, ",")
	if err := p.Run(); err != nil {
		panic(err)
	}
	log.Printf("ISD Hourly Parser Done")
}

func main() {
	// global parameters
	flag.BoolVar(&verbose, "v", false, "print progress report")
	flag.BoolVar(&help, "h", false, "print help information")
	flag.StringVar(&profilePath, "p", ``, "pprof file path (disable by default)")
	flag.Parse()
	if help {
		Usage()
	}

	// check subcommand and perf
	args := flag.Args()
	if len(args) < 1 {
		log.Println("subcommand is required: daily or hourly")
		Usage()
	}
	subCommand = args[0]
	if verbose {
		log.Printf("Subcommand: %s\n", subCommand)
	}

	// Launcher pprof if profilePath is specified
	if profilePath != "" {
		f, _ := os.Create(profilePath)
		if err := pprof.StartCPUProfile(f); err != nil {
			panic(err)
		}
		defer pprof.StopCPUProfile()
	}

	// run command
	switch subCommand {
	case "daily", "day":
		dailyCmd := flag.NewFlagSet("daily", flag.ExitOnError)
		dailyCmd.StringVar(&inputPath, "i", `stdin`, "input file path, stdin by default")
		dailyCmd.StringVar(&outputPath, "o", `stdout`, "output file path, stdout by default")
		dailyCmd.BoolVar(&verbose, "v", false, "print progress report")
		if err := dailyCmd.Parse(args[1:]); err != nil {
			log.Printf("invalid args for daily subcommand: %v\n", args[1:])
			Usage()
		}
		log.Printf("Run daily parser from %s to %s\n", inputPath, outputPath)
		RunDailyParser()
	case "hourly", "hour":
		hourlyCmd := flag.NewFlagSet("daily", flag.ExitOnError)
		hourlyCmd.StringVar(&inputPath, "i", `stdin`, "input file path, stdin by default")
		hourlyCmd.StringVar(&outputPath, "o", `stdout`, "output file path, stdout by default")
		hourlyCmd.StringVar(&dedupeMode, "d", `raw`, "dedupe mode, raw by default, could be ts-first or hour-first")
		hourlyCmd.BoolVar(&verbose, "v", false, "print progress report")
		if err := hourlyCmd.Parse(args[1:]); err != nil {
			log.Printf("invalid args for hourly subcommand: %v\n", args[1:])
			Usage()
		}
		log.Printf("Run hourly parser from %s to %s in %s dedupe mode\n", inputPath, outputPath, dedupeMode)
		RunHourlyParser()

	default:
		log.Println("invalid subcommand:", subCommand)
		Usage()
	}
}
