package content

import (
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"text/template"
)

type Templater struct {
	cache map[string]*template.Template
}

func NewTemplater(dir string) (Templater, error) {
	m := make(map[string]*template.Template)
	err := filepath.Walk(dir, func(path string, info os.FileInfo, err error) error {
		if info.IsDir() || !strings.HasSuffix(info.Name(), ".html.tpl") {
			return nil
		}

		// Skip files prefixed with '_' as these are considered helper functions
		// that shouldn't be indexed.
		if strings.HasPrefix(info.Name(), "_") {
			return nil
		}

		raw, err := ioutil.ReadFile(path)
		if err != nil {
			return fmt.Errorf("template: %s", err)
		}

		t := template.New(info.Name())
		funcMap := map[string]interface{}{
			"import": func(file string) (string, error) {
				// The file to be imported is relative to the file currently
				// being parsed.
				d, _ := filepath.Split(path)
				b, err := ioutil.ReadFile(filepath.Join(d, file))
				if err != nil {
					return "", err
				}
				return string(b), nil
			},
		}
		t.Funcs(funcMap)

		_, err = t.Parse(string(raw))
		if err != nil {
			return fmt.Errorf("template: %s", err)
		}
		m[info.Name()] = t

		return nil
	})

	return Templater{cache: m}, err
}

func (t *Templater) Render(w io.Writer, name string, params interface{}) error {
	tt, ok := t.cache[name]
	if !ok {
		return fmt.Errorf("template: %q not found", name)
	}
	err := tt.Execute(w, params)
	if err != nil {
		return fmt.Errorf("template: %s", err)
	}
	return nil
}
