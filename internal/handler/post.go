package handler

import (
	"fmt"
	"github.com/russross/blackfriday/v2"
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"text/template"
)

type Post struct {
	Content string
}

func NewTemplateHandler(baseDir string, templatePath string) (http.HandlerFunc, error) {
	// Walk the path and build up a mapping of post names to paths
	m := make(map[string]string)
	err := filepath.Walk(baseDir, func(path string, info os.FileInfo, err error) error {
		if info.IsDir() || !strings.HasSuffix(info.Name(), ".md") {
			return nil
		}

		name, err := filepath.Rel(baseDir, path)
		if err != nil {
			return err
		}
		m[strings.TrimSuffix(name, ".md")] = path

		return nil
	})
	if err != nil {
		return nil, fmt.Errorf("handler: posts: %s", err)
	}

	raw, err := ioutil.ReadFile(templatePath)
	if err != nil {
		return nil, fmt.Errorf("handler: posts: %s", err)
	}

	t := template.New(templatePath)
	_, err = t.Parse(string(raw))

	handleFunc := func(w http.ResponseWriter, r *http.Request) {
		name := r.URL.Path
		p, ok := m[name]
		if !ok {
			http.NotFound(w, r)
			return
		}

		raw, err := ioutil.ReadFile(p)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
		md := blackfriday.Run(raw)

		w.Header().Set("content/type", "text/html")
		err = t.Execute(w, Post{Content: string(md)})
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
	}

	return handleFunc, nil
}
