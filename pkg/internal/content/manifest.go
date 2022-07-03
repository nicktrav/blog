package content

import (
	"os"
	"path/filepath"

	"gopkg.in/yaml.v2"
)

type Manifest struct {
	root      string
	Address   string `yaml:"address"`
	Templates string `yaml:"templates"`
	Index     Index `yaml:"index"`
	Posts     Posts  `yaml:"posts"`
	Useful    Posts  `yaml:"useful"`
}

type Index struct {
	Template string `yaml:"template"`
}

type Posts struct {
	Template string `yaml:"template"`
	Pages    []Page `yaml:"pages"`
}

type Metadata map[string]string

type Page struct {
	URL      string   `yaml:"url"`
	File     string   `yaml:"file"`
	Metadata Metadata `yaml:"meta"`
}

func ParseManifest(path string) (Manifest, error) {
	m := Manifest{
		root: filepath.Dir(path),
	}
	b, err := os.ReadFile(path)
	if err != nil {
		return m, err
	}
	err = yaml.Unmarshal(b, &m)
	return m, err
}
