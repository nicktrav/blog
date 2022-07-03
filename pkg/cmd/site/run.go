package main

import (
	"log"
	"net/http"
	"time"

	"github.com/nicktrav/site/pkg/internal/content"
	"github.com/spf13/cobra"
)

func init() {
	runCmd.Flags().String("manifest", "", "Path to the manifest file")
}

var runCmd = &cobra.Command{
	Use:   "run",
	Short: "Run the webserver.",
	Long:  "Run the webserver for the site.",
	RunE: func(cmd *cobra.Command, args []string) error {
		path, err := cmd.Flags().GetString("manifest")
		if err != nil {
			return err
		}
		m, err := content.ParseManifest(path)
		if err != nil {
			return err
		}
		mgr, err := content.NewManager(m)
		if err != nil {
			return err
		}
		router, err := mgr.Router()
		if err != nil {
			return err
		}
		s := http.Server{
			Addr:         m.Address,
			Handler:      router,
			ReadTimeout:  15 * time.Second,
			WriteTimeout: 15 * time.Second,
		}
		log.Printf("listening on %s ...", m.Address)
		return s.ListenAndServe()
	},
}
