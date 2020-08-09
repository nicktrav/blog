package main

import (
	"github.com/gorilla/handlers"
	"net/http"
	"os"
	"time"

	"github.com/gorilla/mux"
	"github.com/nicktrav/site/internal/config"
	"github.com/nicktrav/site/internal/content"
	"github.com/spf13/cobra"
)

var runCmd = &cobra.Command{
	Use:   "run",
	Short: "Run the webserver.",
	Long:  "Run the webserver for the site.",
	RunE: func(cmd *cobra.Command, args []string) error {
		configPath, err := cmd.Flags().GetString("config")
		if err != nil {
			return err
		}

		c, err := config.Parse(configPath)
		if err != nil {
			return err
		}

		t, err := content.NewTemplater(c.Templates.Dir)
		if err != nil {
			return err
		}

		indexController := content.NewIndexController(t, c.Index.Template)

		postOpts := content.PageManagerOpts{
			Dir:           c.Posts.Dir,
			PageTemplate:  c.Posts.PostTemplate,
			IndexTemplate: c.Posts.IndexTemplate,
		}
		postManager, err := content.NewPageManager(t, postOpts)
		if err != nil {
			return err
		}

		usefulOpts := content.PageManagerOpts{
			Dir:           c.Useful.Dir,
			PageTemplate:  c.Useful.PageTemplate,
			IndexTemplate: c.Useful.IndexTemplate,
		}
		usefulManager, err := content.NewPageManager(t, usefulOpts)
		if err != nil {
			return err
		}

		r := mux.NewRouter()
		r.Path("/").Handler(withLogging(indexController.NewHandler()))
		r.Path("/favicon.ico").Handler(withLogging(http.RedirectHandler("/static/favicon.ico", 301)))
		r.PathPrefix("/static/").Handler(withLogging(http.StripPrefix("/static/", http.FileServer(http.Dir("./static")))))

		h, err := postManager.NewIndexHandler()
		if err != nil {
			return err
		}
		r.Path("/posts").Handler(withLogging(h))
		r.PathPrefix("/posts/").Handler(withLogging(http.StripPrefix("/posts/", postManager.NewPageHandler())))

		h, err = usefulManager.NewIndexHandler()
		if err != nil {
			return err
		}
		r.Path("/useful").Handler(withLogging(h))
		r.PathPrefix("/useful/").Handler(withLogging(http.StripPrefix("/useful/", usefulManager.NewPageHandler())))

		s := http.Server{
			Addr:         c.Server.Addr(),
			Handler:      r,
			ReadTimeout:  15 * time.Second,
			WriteTimeout: 15 * time.Second,
		}

		return s.ListenAndServe()
	},
}

func init() {
	runCmd.Flags().String("config", "", "Path to the configuration file")
}

func withLogging(handler http.Handler) http.Handler {
	return handlers.LoggingHandler(os.Stdout, handler)
}
