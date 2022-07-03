package main

import "github.com/spf13/cobra"

var rootCmd = &cobra.Command{
	Use:   "site",
	Short: "Tools for running my site",
}

func init() {
	rootCmd.SilenceUsage = true
	rootCmd.AddCommand(runCmd)
}
