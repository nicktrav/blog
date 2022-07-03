package content

import (
	"log"
	"net/http"
	"os"
	"path/filepath"

	"github.com/gorilla/handlers"
	"github.com/gorilla/mux"
)

type Manager struct {
	m Manifest
	t Templater
}

func NewManager(m Manifest) (*Manager, error) {
	t, err := NewTemplater(filepath.Join(m.root, m.Templates))
	if err != nil {
		return nil, err
	}
	mgr := &Manager{
		m: m,
		t: t,
	}
	return mgr, nil
}

func (m *Manager) Router() (*mux.Router, error) {
	r := mux.NewRouter()

	type pageInfo struct {
		Name, URL, Date string
	}
	var posts, useful []pageInfo

	// Posts.
	for _, page := range m.m.Posts.Pages {
		p := page
		html, err := os.ReadFile(filepath.Join(m.m.root, p.File))
		if err != nil {
			return nil, err
		}
		log.Printf("registering post handler %s", p.URL)
		title, date := p.Metadata["title"], p.Metadata["date"]
		r.Path(p.URL).Handler(fnWithLogging(func(w http.ResponseWriter, req *http.Request) {
			params := struct {
				Title, Date, HTML string
			}{
				Title: title,
				Date:  date,
				HTML:  string(html),
			}
			err = m.t.Render(w, m.m.Posts.Template, params)
			if err != nil {
				log.Printf("error: %s\n", err)
				w.WriteHeader(http.StatusInternalServerError)
			}
		}))
		posts = append(posts, pageInfo{
			Name: title,
			URL:  p.URL,
			Date: date,
		})
	}

	// Useful.
	for _, page := range m.m.Useful.Pages {
		p := page
		html, err := os.ReadFile(filepath.Join(m.m.root, p.File))
		if err != nil {
			return nil, err
		}
		log.Printf("registering useful handler %s", p.URL)
		title := p.Metadata["title"]
		r.Path(p.URL).Handler(fnWithLogging(func(w http.ResponseWriter, req *http.Request) {
			params := struct {
				Title, Date, HTML string
			}{
				Title: title,
				HTML:  string(html),
			}
			err = m.t.Render(w, m.m.Useful.Template, params)
			if err != nil {
				log.Printf("error: %s\n", err)
				w.WriteHeader(http.StatusInternalServerError)
			}
		}))
		useful = append(useful, pageInfo{
			Name: title,
			URL:  p.URL,
		})
	}

	// Root.
	log.Printf("registering index handler")
	r.Path("/").Handler(fnWithLogging(func(w http.ResponseWriter, req *http.Request) {
		params := struct {
			Posts, Useful []pageInfo
		}{
			Posts:  posts,
			Useful: useful,
		}
		err := m.t.Render(w, m.m.Index.Template, params)
		if err != nil {
			log.Printf("error: %s\n", err)
			w.WriteHeader(http.StatusInternalServerError)
		}
	}))

	r.Path("/favicon.ico").Handler(withLogging(http.RedirectHandler("/static/favicon.ico", 301)))
	r.PathPrefix("/static/").Handler(withLogging(http.StripPrefix("/static/", http.FileServer(http.Dir("./content/static")))))

	return r, nil
}

func withLogging(handler http.Handler) http.Handler {
	return handlers.LoggingHandler(os.Stdout, handler)
}

func fnWithLogging(handler http.HandlerFunc) http.Handler {
	return handlers.LoggingHandler(os.Stdout, handler)
}
